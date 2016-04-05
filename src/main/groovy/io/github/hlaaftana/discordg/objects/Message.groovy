package io.github.hlaaftana.discordg.objects

import java.util.List

import io.github.hlaaftana.discordg.util.*

/**
 * A Discord message.
 * @author Hlaaftana
 */
class Message extends DiscordObject{
	Message(Client client, Map object){
		super(client, object)
	}

	/**
	 * @return the content of this message.
	 */
	String getName(){ return this.getContent() }
	/**
	 * @return the content of this message.
	 */
	String getContent(){ return this.object["content"] }
	/**
	 * @return a raw timestamp string of when the message was created.
	 */
	String getRawCreateTime(){ return this.object["timestamp"] }
	/**
	 * @return a Date of when the message was created.
	 */
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.rawCreateTime) }
	/**
	 * @return a raw timestamp string of when the message was edited.
	 */
	String getRawEditTime(){ return this.object["edited_timestamp"] }
	/**
	 * @return a Date of when the message was edited.
	 */
	Date getEditTime(){ return ConversionUtil.fromJsonDate(this.rawEditTime) }
	/**
	 * @return whether or not this message has text to speech.
	 */
	boolean isTts(){ return this.object["tts"] }
	/**
	 * @return whether or not the message mentions everyone. Yes, this method's name makes no sense, but Groovy inference is nice
	 */
	boolean isMentionsEveryone(){ return this.object["mention_everyone"] }
	/**
	 * @return a List of Attachments.
	 */
	List<Attachment> getAttachments(){ return this.object["attachments"].collect { new Attachment(client, it) } }
	/**
	 * @return a List of Maps containing the embeds. Might replace with Embed objects.
	 */
	List getEmbeds() { return this.object["embeds"] }
	/**
	 * @return the author of the message.
	 */
	User getAuthor() { return new User(client, this.object["author"]) }
	/**
	 * @return the author of the message.
	 */
	User getSender() { return this.getAuthor() }
	/**
	 * @return the server the message is in.
	 */
	Server getServer() { return this.textChannel.server }
	/**
	 * @return the channel the message is in.
	 */
	TextChannel getTextChannel() { return client.getTextChannelById(this.object["channel_id"]) }
	/**
	 * @return the channel the message is in.
	 */
	TextChannel getChannel() { return this.textChannel }

	/**
	 * @return a list of users who were mentioned in this message.
	 */
	List<User> getMentions(){ return this.object["mentions"].collect { new User(client, it) } }

	/**
	 * Edits the message.
	 * @param newContent - the new content of the message.
	 * @return the edited Message.
	 */
	Message edit(String newContent) {
		return new Message(client, JSONUtil.parse(client.requester.patch("https://discordapp.com/api/channels/${this.object["channel_id"]}/messages/${this.id}", ["content": newContent])))
	}

	/**
	 * Deletes the message.
	 */
	void delete() {
		client.requester.delete("https://discordapp.com/api/channels/${this.object["channel_id"]}/messages/${this.id}")
	}

	void deleteAfter(long ms){ Thread.sleep(ms); this.delete() }
	Message editAfter(String newContent, long ms){ Thread.sleep(ms); return this.edit(newContent) }
	void deleteIf(Closure closure){ if (closure(this)){ this.delete() } }
	Message editIf(String newContent, Closure closure){ if (closure(this)){ this.edit(newContent) } }

	String toString(){ return this.author.name + ": " + this.content }

	static class Attachment extends DiscordObject {
		Attachment(Client client, Map object){ super(client, object) }

		String getName(){ return this.object["filename"] }
		String getFilename(){ return this.object["filename"] }
		String getFileName(){ return this.object["filename"] }
		String getProxyUrl(){ return this.object["proxy_url"] }
		String getUrl(){ return this.object["url"] }
		int getSize(){ return this.object["size"] }
		boolean isImage(){ return this.object["width"] != null && this.object["height"] != null }
		int getWidth(){ return this.object["width"] }
		int getHeight(){ return this.object["height"] }
		URL getUrlObject(){ return new URL(this.url) }
		URL getProxyUrlObject(){ return new URL(this.url) }
		File download(File file){ return file.withOutputStream { out ->
				out << client.requester.headerUp(this.urlObject, true)
					.with { requestMethod = "GET"; delegate }.inputStream
				delegate
			}
		}
	}
}
