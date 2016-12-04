package hlaaftana.discordg.objects

import java.io.File;
import java.io.InputStream;

import com.mashape.unirest.http.Unirest
import groovy.transform.InheritConstructors
import hlaaftana.discordg.Client
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.net.HTTPClient
import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.JSONUtil

class Webhook extends DiscordObject {
	Webhook(Client client, Map object){
		super(client, object)
	}

	String getName(){ object.name ?: object.user.username }
	String getAvatarHash(){ object.avatar ?: object.user.avatar }
	boolean hasAvatar(){ object["avatar"] }
	String getAvatar(){ hasAvatar() ?
		"https://cdn.discordapp.com/avatars/${id}/${avatarHash}.jpg" : "" }
	InputStream getAvatarInputStream(){ inputStreamFromDiscord(avatar) }
	File downloadAvatar(file){ downloadFileFromDiscord(avatar, file) }
	User getUser(){ object.user ? new User(client, object.user) : null }
	String getChannelId(){ object.channel_id }
	String getServerId(){ object.guild_id }
	Server getServer(){ client.server(serverId) }
	Channel getChannel(){ server.channel(channelId) }
	String getToken(){ object.token }

	Webhook edit(Map data){
		client.editWebhook(data, this)
	}

	void delete(){
		client.deleteWebhook(this)
	}

	Message sendMessage(Map data){
		client.sendMessage(data + [webhook: true], this)
	}

	Message sendMessage(content, boolean tts = false){
		sendMessage(content: content, tts: tts)
	}

	Message send(Map data){ sendMessage(data) }
	Message send(content, boolean tts = false){ sendMessage(content, tts) }

	Message sendFile(Map data, implicatedFile, filename = null) {
		client.sendFile(data + [webhook: true], this, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([webhook: true], implicatedFile, filename)
	}
}
