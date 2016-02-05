package ml.hlaaftana.discordg.objects

import java.util.List

import ml.hlaaftana.discordg.util.*

/**
 * A Discord message.
 * @author Hlaaftana
 */
class Message extends DiscordObject{
	Message(API api, Map object){
		super(api, object)
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
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.timestampRaw) }
	/**
	 * @return a raw timestamp string of when the message was edited.
	 */
	String getRawEditTime(){ return this.object["edited_timestamp"] }
	/**
	 * @return a Date of when the message was edited.
	 */
	Date getEditTime(){ return ConversionUtil.fromJsonDate(this.editTimeRaw) }
	/**
	 * @return whether or not this message has text to speech.
	 */
	boolean isTTS(){ return this.object["tts"] }
	/**
	 * @return whether or not the message mentions everyone. Yes, this method's name makes no sense, but Groovy inference is nice
	 */
	boolean isMentionsEveryone(){ return this.object["mention_everyone"] }
	/**
	 * @return a List of Attachments.
	 */
	List<Attachment> getAttachments(){ return this.object["attachments"].collect { new Attachment(api, it) } }
	/**
	 * @return a List of Maps containing the embeds. Might replace with Embed objects.
	 */
	List getEmbeds() { return this.object["embeds"] }
	/**
	 * @return the author of the message.
	 */
	User getAuthor() { return new User(api, this.object["author"]) }
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
	TextChannel getTextChannel() { return api.client.getTextChannelById(this.object["channel_id"]) }
	/**
	 * @return the channel the message is in.
	 */
	TextChannel getChannel() { return this.textChannel }

	/**
	 * @return a list of users who were mentioned in this message.
	 */
	List<User> getMentions(){ return this.object["mentions"].collect { new User(api, it) } }

	/**
	 * Edits the message.
	 * @param newContent - the new content of the message.
	 * @return the edited Message.
	 */
	Message edit(String newContent) {
		return new Message(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/channels/${this.object["channel_id"]}/messages/${this.id}", ["content": newContent])))
	}

	/**
	 * Deletes the message.
	 */
	void delete() {
		api.requester.delete("https://discordapp.com/api/channels/${this.object["channel_id"]}/messages/${this.id}")
	}

	// removed ack method because of discord dev request

	String toString(){ return this.author.name + ": " + this.content }

	static class Attachment extends DiscordObject {
		Attachment(API api, Map object){ this.api = api; this.object = object }

		String getName(){ return this.object["filename"] }
		String getFilename(){ return this.object["filename"] }
		String getFileName(){ return this.object["filename"] }
		String getProxyUrl(){ return this.object["proxy_url"] }
		String getUrl(){ return this.object["url"] }
		int getSize(){ return this.object["size"] }
		boolean isImage(){ return this.object["width"] != null && this.object["height"] != null }
		int getWidth(){ return this.object["width"] }
		int getHeight(){ return this.object["height"] }
	}
}
