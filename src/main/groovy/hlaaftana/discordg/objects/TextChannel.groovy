package io.github.hlaaftana.discordg.objects

import groovy.json.JsonException
import java.util.List

import io.github.hlaaftana.discordg.exceptions.RateLimitException
import io.github.hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.Unirest

/**
 * A text channel. Extends Channel.
 * @author Hlaaftana
 */
class TextChannel extends Channel {
	TextChannel(Client client, Map object){
		super(client, object)
	}

	/**
	 * @return the topic of the channel. Might be null.
	 */
	String getTopic() { return this.object["topic"] }

	/**
	 * @return a mention for the channel.
	 */
	String getMention() { return "<#${this.id}>" }

	/**
	 * Start typing in the channel.
	 */
	void startTyping() {
		client.requester.post("https://discordapp.com/api/channels/${this.id}/typing", [:])
	}

	/**
	 * Send a message to the channel.
	 * @param content - a string containing the message content.
	 * @param tts - a boolean to decide whether or not this message has text to speech. False by default.
	 * @return a Message object which is the sent message.
	 */
	Message sendMessage(String content, boolean tts=false) {
		if (content.length() > 2000) throw new Exception("You tried to send a message longer than 2000 characters.")
		String ass = client.requester.post("https://discordapp.com/api/channels/${this.id}/messages", ["content": content, "tts": tts, "channel_id": this.id])
		try{
			return new Message(client, JSONUtil.parse(ass))
		}catch (JsonException ex){
			throw new RateLimitException(ass)
		}
	}

	/**
	 * Sends a file to a channel.
	 * @param file - the File object to send.
	 * @param data - optional data to send with the request
	 * @return - the sent message as a Message object.
	 */
	Message sendFile(File file, Map data=[:]){
		return new Message(client, JSONUtil.parse(Unirest.post("https://discordapp.com/api/channels/${this.id}/messages").header("authorization", client.token).header("user-agent", client.fullUserAgent).field("file", file).field("content", data["content"] == null ? "" : data["content"]).field("tts", data["tts"] as boolean).asString().body))
	}

	/**
	 * Sends a file to a channel.
	 * @param filePath - the file path as a string.
	 * @return - the sent message as a Message object.
	 */
	Message sendFile(String filePath, Map data=[:]){ return this.sendFile(new File(filePath), data) }

	/**
	 * Get message history from the channel. Warning: this'll be quite slower each multiple of 100.
	 * @param max - the max number of messages. 100 by default.
	 * @return a List of Message containing the messages in the channel.
	 */
	List<Message> getLogs(int max=100, long sleepLength=2000) {
		if (max <= 100){
			return JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=${max}")).collect { try{ new Message(client, it) }catch (ex){ throw new RateLimitException(it.toString()) } }
		}else{
			List<Message> initialRequest = JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=100")).collect { new Message(client, it) }
			Thread.sleep(sleepLength)
			for (int m = 1; m < (int) Math.ceil(max / 100) - 1; m++){
				initialRequest += JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=100")).collect { new Message(client, it) }
				Thread.sleep(sleepLength)
			}
			if (max % 100 > 0) initialRequest += JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=${max % 100}")).collect { new Message(client, it) }
			else initialRequest += JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=${100}")).collect { new Message(client, it) }
			return initialRequest
		}
	}

	List<Message> getCachedLogs(){
		return this.object["cached_messages"].collect { new Message(client, it) }
	}

	boolean clearMessagesOf(User user, int messageCount=100){
		try{
			this.getLogs(messageCount)*.deleteIf { it.author == user }
		}catch (ex){
			return false
		}
		return true
	}

	String getLastMessageId(){
		return this.object["last_message_id"]
	}
}
