package ml.hlaaftana.discordg.objects

import groovy.json.JsonException
import java.util.List

import ml.hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.Unirest

/**
 * A text channel. Extends Channel.
 * @author Hlaaftana
 */
class TextChannel extends Channel {
	TextChannel(API api, Map object){
		super(api, object)
	}

	/**
	 * @return the topic of the channel. Might be null.
	 */
	String getTopic() { return object["topic"] }
	/**
	 * @return a mention for the channel.
	 */
	String getMention() { return "<#${this.id}>" }

	/**
	 * Start typing in the channel.
	 */
	void startTyping() {
		api.requester.post("https://discordapp.com/api/channels/${this.getId()}/typing", [:])
	}

	/**
	 * Send a message to the channel.
	 * @param content - a string containing the message content.
	 * @param tts - a boolean to decide whether or not this message has text to speech. False by default.
	 * @return a Message object which is the sent message.
	 */
	Message sendMessage(String content, boolean tts=false) {
		if (content.length() > 2000) throw new Exception("You tried to send a message longer than 2000 characters.")
		try{
			return new Message(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/channels/${this.id}/messages", ["content": content, "tts": tts, "channel_id": this.id])))
		}catch (JsonException ex){
			throw new Exception("You need to enter the chill zone.")
		}
	}

	/**
	 * Sends a file to a channel. Currently not directly working.
	 * @param file - the File object to send.
	 * @return - the sent message as a Message object.
	 */
	Message sendFile(File file){
		return new Message(api, JSONUtil.parse(Unirest.post("https://discordapp.com/api/channels/${this.id}/messages").header("authorization", api.token).header("user-agent", "https://github.com/hlaaftana/DiscordG, 1.0").field("file", file).asString().getBody()))
	}

	/**
	 * Get message history from the channel.
	 * @param max - the max number of messages. 100 by default.
	 * @return a List of Message containing the messages in the channel.
	 */
	List<Message> getLogs(int max=100) {
		List<Message> logs = new ArrayList<Message>()
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=50"))
		for (m in array){
			logs.add(new Message(api, m))
		}
		return logs
	}
}
