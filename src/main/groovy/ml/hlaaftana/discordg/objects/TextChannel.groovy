package ml.hlaaftana.discordg.objects

import groovy.json.JsonException
import java.util.List

import ml.hlaaftana.discordg.exceptions.RateLimitException
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
	String getTopic() { return this.object["topic"] }

	/**
	 * Send a message to the channel.
	 * @param content - a string containing the message content.
	 * @param tts - a boolean to decide whether or not this message has text to speech. False by default.
	 * @return a Message object which is the sent message.
	 */
	Message sendMessage(String content, boolean tts=false) {
		if (content.length() > 2000) throw new Exception("You tried to send a message longer than 2000 characters.")
		String ass = api.requester.post("https://discordapp.com/api/channels/${this.id}/messages", ["content": content, "tts": tts, "channel_id": this.id])
		try{
			return new Message(api, JSONUtil.parse(ass))
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
		return new Message(api, JSONUtil.parse(Unirest.post("https://discordapp.com/api/channels/${this.id}/messages").header("authorization", api.token).header("user-agent", api.fullUserAgent).field("file", file).field("content", data["content"] == null ? "" : data["content"]).field("tts", data["tts"] as boolean).asString().body))
	}

	/**
	 * Sends a file to a channel.
	 * @param filePath - the file path as a string.
	 * @return - the sent message as a Message object.
	 */
	Message sendFile(String filePath, Map data=[:]){ return this.sendFile(new File(filePath)) }

	/**
	 * Get message history from the channel. Warning: this'll be quite slower each multiple of 100.
	 * @param max - the max number of messages. 100 by default.
	 * @return a List of Message containing the messages in the channel.
	 */
	List<Message> getLogs(int max=100) {
		if (max <= 100){
			return JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=${max}")).collect { try{ new Message(api, it) }catch (ex){ throw new RateLimitException(it.toString()) } }
		}else{
			List<Message> initialRequest = JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?limit=100")).collect { new Message(api, it) }
			for (int m = 1; m < (int) Math.ceil(max / 100) - 1; m++){
				initialRequest += JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=100")).collect { new Message(api, it) }
			}
			if (max % 100 > 0) initialRequest += JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=${max % 100}")).collect { new Message(api, it) }
			else initialRequest += JSONUtil.parse(api.requester.get("https://discordapp.com/api/channels/${this.id}/messages?before=${initialRequest[initialRequest.size() - 1].id}&limit=${100}")).collect { new Message(api, it) }
			return initialRequest
		}
	}

	List<Message> getCachedLogs(){
		return this.object["cached_messages"].collect { new Message(api, it) }
	}

	String getLastMessageId(){
		return this.object["last_message_id"]
	}
}
