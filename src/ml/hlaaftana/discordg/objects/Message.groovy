package ml.hlaaftana.discordg.objects

import java.util.List

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord message.
 * @author Hlaaftana
 */
class Message extends Base{
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
	String getContent(){ return object["content"] }
	/**
	 * @return whether or not this message has text to speech.
	 */
	boolean isTTS(){ return object["tts"] }
	/**
	 * @return a List of Maps containing the attachments. Might replace with Attachment objects.
	 */
	List getAttachments(){ return object["attachments"] }
	/**
	 * @return a List of Maps containing the embdes. Might replace with Embed objects.
	 */
	List getEmbeds() { return object["embeds"] }
	/**
	 * @return the author of the message.
	 */
	User getAuthor() { return new User(api, object["author"]) }
	/**
	 * @return the author of the message.
	 */
	User getSender() { return this.getAuthor() }
	/**
	 * @return the server the message is in.
	 */
	Server getServer() { return this.getTextChannel().getServer() }
	/**
	 * @return the channel the message is in.
	 */
	TextChannel getTextChannel() { return api.client.getTextChannelById(object["channel_id"]) }
	/**
	 * @return the channel the message is in.
	 */
	TextChannel getChannel() { return this.getTextChannel() }

	List<User> getMentions(){ return this.object["mentions"].collect { new User(api, it) } }

	/**
	 * Edits the message.
	 * @param newContent - the new content of the message.
	 * @return the edited Message.
	 */
	Message edit(String newContent) {
		return new Message(api, JSONUtil.parse(api.getRequester().patch("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getId()}", ["content": newContent])))
	}

	/**
	 * Deletes the message.
	 */
	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${object["channel_id"]}/messages/${this.getId()}")
	}

	// removed ack method because of discord dev request
}
