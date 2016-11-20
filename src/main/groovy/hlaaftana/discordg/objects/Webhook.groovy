package hlaaftana.discordg.objects

import com.mashape.unirest.http.Unirest
import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.JSONUtil

class Webhook extends DiscordObject {
	Webhook(Client client, Map object){ super(client, object, "webhooks/$object.id") }

	String getName(){ object.name ?: object.user.username }
	String getAvatarHash(){ object.avatar ?: object.user.avatar }
	User getUser(){ object.user ? new User(client, object.user) : null }
	String getChannelId(){ object.channel_id }
	String getServerId(){ object.guild_id }
	Server getServer(){ client.server(serverId) }
	Channel getChannel(){ server.channel(channelId) }
	String getToken(){ object.token }

	Webhook edit(Map data){
		new Webhook(client, http.jsonPatch("", ConversionUtil.fixImages(data)))
	}

	void delete(){
		http.delete("")
	}

	Message sendMessage(Map data){
		if (data.content != null){
			data.content = client.filterMessage(data.content)
			if (!data.content || data.content.size() > 2000)
				throw new MessageInvalidException(data.content)
		}
		new Message(client, http.jsonPost(token, data))
	}

	Message sendMessage(content, boolean tts = false){
		sendMessage(content: content, tts: tts)
	}

	Message send(Map data){ sendMessage(data) }
	Message send(content, boolean tts = false){ sendMessage(content, tts) }

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
		def aa = Unirest.post("$http.baseUrl/messages")
			.header("Authorization", client.token)
			.header("User-Agent", client.fullUserAgent)
			.field("content", data["content"] == null ? "" : data["content"].toString())
			.field("tts", data["tts"] as boolean)
		if (fileArgs.size() == 1){
			aa = aa.field("file", fileArgs[0])
		}else if (fileArgs.size() == 2){
			aa = aa.field("file", fileArgs[0], fileArgs[1])
		}
		JSONUtil.parse(aa.asString().body)
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
}
