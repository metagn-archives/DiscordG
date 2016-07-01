package hlaaftana.discordg.objects

import groovy.json.JsonException
import java.util.List
import java.util.Map

import hlaaftana.discordg.Client;
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.util.JSONUtil
import hlaaftana.discordg.util.ConversionUtil

import com.mashape.unirest.http.Unirest

/**
 * A text channel. Extends Channel.
 * @author Hlaaftana
 */
@groovy.transform.InheritConstructors
class TextChannel extends Channel {
	String getTopic(){ object["topic"] }
	String getMention(){ "<#${id}>" }

	String getQueueName(){ server ? server.id : "dm" }

	Message sendMessage(content, boolean tts=false) {
		sendMessage(content: content, tts: tts)
	}

	Message sendMessage(Map data){
		if (data.content != null){
			data.content = data.content.toString()
			if (!data.content || data.content.size() > 2000) throw new MessageInvalidException(data.content)
		}
		client.askPool("sendMessages", queueName){
			new Message(client, requester.jsonPost("messages", [channel_id: id] << data))
		}
	}

	Message editMessage(message, content){
		editMessage(message, content: content)
	}

	Message editMessage(Map data, message){
		editMessage(message, data)
	}

	Message editMessage(message, Map data){
		if (data.content != null){
			data.content = data.content.toString()
			if (!data.content || data.content.size() > 2000) throw new MessageInvalidException(data.content)
		}
		client.askPool("sendMessages", queueName){ // that's right, they're in the same bucket
			new Message(client, requester.jsonPatch("messages/${id(message)}", data))
		}
	}

	void deleteMessage(message){
		client.askPool("deleteMessages",
			queueName){ requester.delete("messages/${id(message)}") }
	}

	def sendFileRaw(Map data = [:], file){
		List fileArgs = []
		if (file instanceof File){
			if (data["filename"]){
				fileArgs += file.bytes
				fileArgs += data["filename"]
			}else fileArgs += file
		}else{
			fileArgs += ConversionUtil.getBytes(file)
			if (!data["filename"]) throw new IllegalArgumentException("Tried to send non-file class ${file.class} and gave no filename")
			fileArgs += data["filename"]
		}
		def aa = Unirest.post("$requester.baseUrl/messages")
			.header("Authorization", client.token)
			.header("User-Agent", client.fullUserAgent)
			.field("content", data["content"] == null ? "" : data["content"].toString())
			.field("tts", data["tts"] as boolean)
		if (fileArgs.size() == 1){
			aa = aa.field("file", fileArgs[0])
		}else if (fileArgs.size() == 2){
			aa = aa.field("file", fileArgs[0], fileArgs[1])
		}
		client.askPool("sendMessages", queueName){
			JSONUtil.parse(aa.asString().body)
		}
	}

	Message sendFile(Map data, implicatedFile, filename = null) {
		def file
		if (implicatedFile.class in [File, String]) file = implicatedFile as File
		else file = ConversionUtil.getBytes(implicatedFile)
		new Message(client, sendFileRaw((filename ? [filename: filename] : [:]) << data, file))
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}

	Message requestMessage(message){
		new Message(client,
			requester.jsonGet("messages/${id(message)}"))
	}

	Message getMessage(message){
		cachedLogMap[id(message)]
	}

	Message message(message){ getMessage message }

	def pinMessage(message){
		requester.put("pins/${id(message)}")
	}
	def pin(message){ pinMessage message }

	def unpinMessage(message){
		requester.delete("pins/${id(message)}")
	}
	def unpin(message){ unpinMessage message }

	Collection<Message> requestPinnedMessages(){
		requester.jsonGet("pins").collect { new Message(client, it) }
	}

	Collection<Message> getLogs(int max = 100) {
		List aa = cachedLogs
		if (aa.size() > max){
			aa.sort { -it.createTimeMillis }[0..max - 1]
		}else{
			List bb = aa ?
				requestLogs(max - aa.size(), aa.min { it.createTimeMillis }) :
				requestLogs(max - aa.size())
			bb.each {
				if (client.messages[this]) client.messages[this].add(it)
				else {
					client.messages[this] = new DiscordListCache([it], client, Message)
					client.messages[this].root = this
				}
			}
			(aa + bb).sort { -it.createTimeMillis }
		}
	}
	Collection<Message> logs(int max = 100){ getLogs(max) }

	List<Message> requestLogs(int max = 100, boundary = null, boolean after = false){
		rawRequestLogs(max, boundary, after).collect { new Message(client, it) }
	}

	List rawRequestLogs(int max, boundary = null, boolean after = false){
		Map params = [limit: max > 100 ? 100 : max]
		if (boundary){
			if (after) params.after = id(boundary)
			else params.before = id(boundary)
		}
		List messages = rawRequestLogs(params)
		if (max > 100){
			for (int m = 1; m < Math.floor(max / 100); m++){
				messages += rawRequestLogs(before: messages.last().id, limit: 100)
			}
			messages += rawRequestLogs(before: messages.last().id, limit: max % 100 ?: 100)
		}
		messages
	}

	List rawRequestLogs(Map data = [:]){
		String parameters = data ? "?" + data.collect { k, v ->
			URLEncoder.encode(k.toString()) + "=" + URLEncoder.encode(v.toString())
		}.join("&") : ""
		requester.jsonGet("messages$parameters")
	}

	List<Message> getCachedLogs(){
		client.messages[id]?.list ?: []
	}

	Map<String, Message> getCachedLogMap(){
		client.messages[id]?.map ?: [:]
	}

	def clear(int number = 100){
		clear(getLogs(number)*.id)
	}

	def clear(int number = 100, Closure closure){
		clear(getLogs(number).findAll { closure(it) })
	}

	def clear(List ids){
		client.askPool("bulkDeleteMessages"){
			requester.post("messages/bulk_delete", [messages: ids.collect { id(it) }])
		}
	}

	def clear(user, int number = 100){
		clear(number){ it.author.id == id(user) }
	}

	boolean clearMessagesOf(User user, int messageCount=100){
		try{
			getLogs(messageCount)*.deleteIf { it.author == user }
		}catch (ex){
			return false
		}
		true
	}

	Message find(int number = 100, int increment = 50, int maxTries = 10, Closure closure){
		List<Message> messages = getLogs(number)
		Message ass = messages.find { closure(it) }
		if (ass) ass
		else {
			while (!ass){
				if (((messages.size() - 100) / 50) > maxTries) return null
				number += increment
				messages = getLogs(number)
				ass = messages.find { closure(it) }
			}
			ass
		}
	}

	String getLastMessageId(){
		object["last_message_id"]
	}
}
