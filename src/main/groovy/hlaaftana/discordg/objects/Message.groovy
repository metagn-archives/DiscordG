package hlaaftana.discordg.objects

import java.io.File;
import java.io.InputStream;
import java.util.List;

import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.util.*

/**
 * A Discord message.
 * @author Hlaaftana
 */
class Message extends DiscordObject {
	// this is discord's url pattern (with a little fiddling)
	static String urlPattern = /https?:\/\/[^\s<]+[^<"{|^~`\[\s]/

	Message(Client client, Map object){
		super(client, object)
	}

	String getName(){ content }
	def getNonce(){ object["nonce"] }
	String getContent(){ object["content"] }
	String getRawEditedAt(){ object["edited_timestamp"] }
	Date getEditedAt(){ ConversionUtil.fromJsonDate(rawEditedAt) }
	String getRawTimestamp(){ object["timestamp"] }
	Date getTimestamp(){ ConversionUtil.fromJsonDate(rawTimestamp) }
	boolean isTts(){ object["tts"] }
	int getType(){ object["type"] }
	boolean isMentionsEveryone(){ object["mention_everyone"] }
	String getChannelId(){ object["channel_id"] }
	boolean isPrivate(){ client.cache["private_channels"].containsKey(object["channel_id"]) }
	boolean isDm(){ channel.dm }
	boolean isGroup(){ channel.group }

	Attachment getAttachment(){ attachments[0] }
	List<Attachment> getAttachments(){
		object["attachments"].collect { new Attachment(client, it + [message_id: id]) }
	}
	List<Embed> getEmbeds(){
		object["embeds"].collect { new Embed(client, it + [message_id: id]) }
	}

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
	User getSender() { author }
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
	Member getMember(){ author(true) }
	boolean isByWebhook(){ webhookId }
	String getWebhookId(){ object.webhook_id }
	Webhook requestWebhook(){ client.requestWebhook(webhookId) }

	String getServerId(){ client.channelServerIdMap[object.channel_id] }
	Server getServer(){ channel?.server }
	Channel getParent(){ channel }
	Channel getChannel(){ client.channel(object.channel_id) }

	List<User> getMentions(boolean member = false){ object["mentions"].collect { resolveMember(it, member) } }
	List mentions(boolean member = false){ getMentions(member) }
	List<Role> getMentionedRoles(){ object["mention_roles"].collect { server.roleMap[it] } }
	List<Role> getRoleMentions(){ mentionedRoles }

	List<String> getMentionedChannelIds(){ channelIdMentions }
	List<String> getChannelMentionIds(){ channelIdMentions }
	List<String> getChannelIdMentions(){
		(content =~ /<#(\d+)>/).collect { full, id -> id }
	}

	List<Channel> getMentionedChannels(){ channelMentions }
	List<Channel> getChannelMentions(){
		channelIdMentions.collect(server.&channel) - null
	}
	
	Permissions getAuthorPermissions(){
		channel.permissionsFor(author)
	}

	boolean isMentioned(thing = client.user){
		id(thing) in mentions.collect(this.&id) ||
			server.member(user)?.roles?.any { it in mentionedRoles } ||
			id(thing) in object.mention_roles
	}

	boolean isPinned(){ object["pinned"] }
	def pin(){ client.pinMessage(channelId, id) }
	def unpin(){ channel.unpinMessage(channelId, id) }

	List<Reaction> getReactions(){ object.reactions.collect { new Reaction(client, it) } }

	void react(emoji){
		client.reactToMessage(channelId, this, emoji)
	}

	void unreact(emoji, user = "@me"){
		client.unreactToMessage(channelId, this, emoji, user)
	}

	List<User> requestReactors(emoji, int limit = 100){
		client.requestReactors(channelId, id, emoji, limit)
	}

	Message edit(String newContent) {
		edit(content: newContent)
	}

	Message edit(Map data){
		client.editMessage(data, channelId, this)
	}

	void delete() {
		client.deleteMessage(channelId, this)
	}

	void deleteAfter(long ms){ Thread.sleep(ms); delete() }
	Message editAfter(String newContent, long ms){ Thread.sleep(ms); edit(newContent) }
	void deleteIf(Closure closure){ if (closure(this)){ delete() } }
	Message editIf(String newContent, Closure closure){ if (closure(this)){ edit(newContent) } }

	String toString(){ "$author.name: $content" }
}

@InheritConstructors
class Attachment extends DiscordObject {
	String getMessageId(){ object["message_id"] }
	String getName(){ object["filename"] }
	String getFilename(){ object["filename"] }
	String getFileName(){ object["filename"] }
	int getSize(){ object["size"] }
	boolean isImage(){ object["width"] && object["height"] }
	int getWidth(){ object["width"] }
	int getHeight(){ object["height"] }
	String getProxyUrl(){ object["proxy_url"] }
	String getUrl(){ object["url"] }
	InputStream newInputStream(){ inputStreamFromDiscord(url) }
	File download(file){ downloadFileFromDiscord(url, file) }
}

@InheritConstructors
class Embed extends DiscordObject {
	String getMessageId(){ object["message_id"] }
	String getId(){ messageId }
	String getName(){ object["title"] }
	String getTitle(){ object["title"] }
	String getType(){ object["type"] }
	String getDescription(){ object["description"] }
	String getUrl(){ object["url"] }
	InputStream newInputStream(){ inputStreamFromDiscord(url) }
	File download(file){ downloadFileFromDiscord(url, file) }
	int getColor(){ object["color"] }
	String getTimestamp(){ object["timestamp"] }

	Image getThumbnail(){ object.thumbnail ? new Image(client, object.thumbnail) : null }
	Provider getProvider(){ object.provider ? new Provider(client, object.provider) : null }
	Video getVideo(){ object.video ? new Video(client, object.video) : null }
	Image getImage(){ object.image ? new Image(client, object.image) : null }
	Footer getFooter(){ object.footer ? new Footer(client, object.footer) : null }
	Author getAuthor(){ object.author ? new Author(client, object.author) : null }
	List<Field> getFields(){ object.fields ? object.fields.collect { new Field(client, it) } : null }

	@InheritConstructors
	static class Image extends DiscordObject {
		String getName(){ url }
		String getProxyUrl(){ object["proxy_url"] }
		String getUrl(){ object["url"] }
		int getWidth(){ object["width"] }
		int getHeight(){ object["height"] }
		InputStream newInputStream(){ inputStreamFromDiscord(url) }
		File download(file){ downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Provider extends DiscordObject {
		String getName(){ url }
		String getUrl(){ object["url"] }
		InputStream newInputStream(){ inputStreamFromDiscord(url) }
		File download(file){ downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Video extends DiscordObject {
		String getName(){ url }
		String getUrl(){ object["url"] }
		int getWidth(){ object["width"] }
		int getHeight(){ object["height"] }
		InputStream newInputStream(){ inputStreamFromDiscord(url) }
		File download(file){ downloadFileFromDiscord(url, file) }
	}

	@InheritConstructors
	static class Author extends DiscordObject {
		String getName(){ object["name"] }
		String getIconUrl(){ object["icon_url"] }
		String getProxyIconUrl(){ object["proxy_icon_url"] }
		InputStream getIconInputStream(){ inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file){ downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Footer extends DiscordObject {
		String getName(){ text }
		String getText(){ object["text"] }
		String getIconUrl(){ object["icon_url"] }
		String getProxyIconUrl(){ object["proxy_icon_url"] }
		InputStream getIconInputStream(){ inputStreamFromDiscord(iconUrl) }
		File downloadIcon(file){ downloadFileFromDiscord(iconUrl, file) }
	}

	@InheritConstructors
	static class Field extends DiscordObject {
		String getName(){ object.name }
		String getValue(){ object.value }
		boolean isInline(){ object.inline }
	}
 }

@InheritConstructors
class Reaction extends DiscordObject {
	String getId(){ object.emoji.id }
	String getName(){ object.emoji.name }
	int getCount(){ object.count }
	String getUrl(){ "https://cdn.discordapp.com/emojis/${id}.png" }
	InputStream newInputStream(){ inputStreamFromDiscord(url) }
	File download(file){ downloadFileFromDiscord(url, file) }
	boolean isCustom(){ name ==~ /\w+/ }
	boolean isByMe(){ object.me }
}
