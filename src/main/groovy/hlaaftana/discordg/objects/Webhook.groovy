package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject

@CompileStatic
class Webhook extends DiscordObject {
	Webhook(Client client, Map object){
		super(client, object)
	}

	String getName(){ object.name ?: ((Map) object.user).username }
	String getAvatarHash(){ object.avatar ?: ((Map) object.user).avatar }
	boolean hasAvatar(){ object.avatar }
	String getAvatar(){ hasAvatar() ?
		"https://cdn.discordapp.com/avatars/$id/${avatarHash}.jpg" : '' }
	InputStream getAvatarInputStream(){ inputStreamFromDiscord(avatar) }
	File downloadAvatar(file){ downloadFileFromDiscord(avatar, file) }
	User getUser(){ object.user ? new User(client, (Map) object.user) : null }
	String getChannelId() { (String) object.channel_id }
	String getGuildId() { (String) object.guild_id }
	Guild getGuild(){ client.guild(guildId) }
	Channel getChannel(){ guild.channel(channelId) }
	String getToken() { (String) object.token }

	Webhook retrieve() {
		client.requestWebhook(id, token)
	}

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
		sendFile([:], implicatedFile, filename)
	}
}
