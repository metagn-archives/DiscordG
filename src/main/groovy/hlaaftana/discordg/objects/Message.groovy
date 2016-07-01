package hlaaftana.discordg.objects

import java.io.File;
import java.io.InputStream;
import java.util.List

import hlaaftana.discordg.Client;
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.util.*

/**
 * A Discord message.
 * @author Hlaaftana
 */
class Message extends DiscordObject{
	// this is discord's url pattern (with a little fiddling)
	static String urlPattern = /https?:\/\/[^\s<]+[^<"{|^~`\[\s]/

	Message(Client client, Map object){
		super(client, object, "channels/${object["channel_id"]}/messages/${object["id"]}")
	}

	String getName(){ getContent() }
	def getNonce(){ object["nonce"] }
	String getContent(){ object["content"] }
	String getRawEditTime(){ object["edited_timestamp"] }
	Date getEditTime(){ ConversionUtil.fromJsonDate(rawEditTime) }
	String getRawTimestamp(){ object["timestamp"] }
	Date getTimestamp(){ ConversionUtil.fromJsonDate rawTimestamp }
	boolean isTts(){ object["tts"] }
	boolean isMentionsEveryone(){ object["mention_everyone"] }
	boolean isPrivate(){ !server }
	Attachment getAttachment(){ attachments[0] }
	List<Attachment> getAttachments(){ object["attachments"].collect { new Attachment(client, it) } }
	List<Embed> getEmbeds() { object["embeds"].collect { new Embed(client, it) } }

	List<String> getUrls(){
		(content =~ urlPattern).collect()
	}

	List<URL> getUrlObjects(){ urls*.toURL() }
	User getAuthor(boolean member = false) {
		resolveMember(object["author"], member)
	}
	User author(boolean member = false){
		getAuthor(member)
	}
	User getSender() { getAuthor() }
	User resolveMember(User user, boolean member = true){
		Member ass = server?.member(user)
		if (ass && member) ass
		else user
	}
	User resolveMember(Map object, boolean member = true){
		Member ass = server?.member(object)
		if (ass && member) ass
		else new User(client, object)
	}
	Server getServer() { textChannel.server }
	TextChannel getParent(){ channel }
	TextChannel getTextChannel() { client.textChannel(object["channel_id"]) }
	TextChannel getChannel() { textChannel }

	List<User> getMentions(boolean member = false){ object["mentions"].collect { resolveMember(it, member) } }
	List mentions(boolean member = false){ getMentions(member) }
	List<Role> getMentionedRoles(){ object["mention_roles"].collect { server.roleMap[it] } }
	List<Role> getRoleMentions(){ mentionedRoles }

	boolean isMentioned(thing = client.user){
		id(thing) in mentions.collect(this.&id) ||
			server.member(user)?.roles?.any { it in mentionedRoles } ||
			id(thing) in object.mention_roles
	}

	boolean isPinned(){ object["pinned"] }
	def pin(){ channel.pin this }
	def unpin(){ channel.unpin this }

	Message edit(String newContent) {
		edit(content: newContent)
	}

	Message edit(Map data){
		channel.editMessage(this, data)
	}

	void delete() {
		channel.deleteMessage(this)
	}

	void deleteAfter(long ms){ Thread.sleep(ms); delete() }
	Message editAfter(String newContent, long ms){ Thread.sleep(ms); edit(newContent) }
	void deleteIf(Closure closure){ if (closure(this)){ delete() } }
	Message editIf(String newContent, Closure closure){ if (closure(this)){ edit(newContent) } }

	String toString(){ author.name + ": " + content }

	static class Attachment extends DiscordObject {
		Attachment(Client client, Map object){ super(client, object) }

		String getName(){ object["filename"] }
		String getFilename(){ object["filename"] }
		String getFileName(){ object["filename"] }
		int getSize(){ object["size"] }
		boolean isImage(){ object["width"] && object["height"] }
		int getWidth(){ object["width"] }
		int getHeight(){ object["height"] }
		String getProxyUrl(){ object["proxy_url"] }
		String getUrl(){ object["url"] }
		InputStream getInputStream(){
			url.toURL().openConnection().with {
				setRequestProperty("User-Agent", client.fullUserAgent)
				setRequestProperty("Accept", "*/*")
				delegate
			}.inputStream
		}
		File download(File file){
			file.withOutputStream { out ->
				out << inputStream
				new File(file.path)
			}
		}
	}

	static class Embed extends DiscordObject {
		Embed(Client client, Map object){ super(client, object) }

		String getName(){ object["title"] }
		String getTitle(){ object["title"] }
		String getType(){ object["type"] }
		String getDescription(){ object["description"] }
		String getUrl(){ object["url"] }
		Thumbnail getThumbnail(){ new Thumbnail(client, object["thumbnail"]) }
		boolean isImage(){ thumbnail.image }
		Provider getProvider(){ new Provider(client, object["provider"]) }
		InputStream getInputStream(){
			url.toURL().openConnection().with {
				setRequestProperty("User-Agent", client.fullUserAgent)
				setRequestProperty("Accept", "*/*")
				delegate
			}.inputStream
		}
		File download(File file){
			file.withOutputStream { out ->
				out << inputStream
				new File(file.path)
			}
		}

		static class Thumbnail extends APIMapObject {
			Thumbnail(Client client, Map object){ super(client, object) }

			String getProxyUrl(){ object["proxy_url"] }
			String getUrl(){ object["url"] }
			boolean isImage(){ object["width"] && object["height"] }
			int getWidth(){ object["width"] }
			int getHeight(){ object["height"] }
			InputStream getInputStream(){
				url.toURL().openConnection().with {
					setRequestProperty("User-Agent", client.fullUserAgent)
					setRequestProperty("Accept", "*/*")
					delegate
				}.inputStream
			}
			File download(File file){
				file.withOutputStream { out ->
					out << inputStream
					new File(file.path)
				}
			}
		}

		static class Provider extends APIMapObject {
			Provider(Client client, Map object){ super(client, object) }

			String getUrl(){ object["url"] }
			InputStream getInputStream(){
				url.toURL().openConnection().with {
					setRequestProperty("User-Agent", client.fullUserAgent)
					setRequestProperty("Accept", "*/*")
					delegate
				}.inputStream
			}
			File download(File file){
				file.withOutputStream { out ->
					out << inputStream
					new File(file.path)
				}
			}
		}
 	}
}
