package hlaaftana.discordg.data

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors

@CompileStatic
@InheritConstructors
class Webhook extends DiscordObject {
	Snowflake id, channelId, guildId
	String name, avatarHash, token
	User user

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, name: 2, avatar: 3, token: 4, user: 5, channel_id: 5, guild_id: 6)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			name = (String) value
		} else if (f == 3) {
			avatarHash = (String) value
		} else if (f == 4) {
			token = (String) value
		} else if (f == 5) {
			user = new User(client)
			user.fill((Map) value)
		} else if (f == 6) {
			channelId = Snowflake.swornString(value)
		} else if (f == 7) {
			guildId = Snowflake.swornString(value)
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	boolean hasAvatar() { avatarHash }
	String getAvatar() { hasAvatar() ?
		"https://cdn.discordapp.com/avatars/$id/${avatarHash}.jpg" : '' }
	InputStream getAvatarInputStream() { inputStreamFromDiscord(avatar) }
	File downloadAvatar(file) { downloadFileFromDiscord(avatar, file) }
	Guild getGuild() { null == guildId ? channel.guild : client.guildCache[guildId] }
	Channel getChannel() { null == guildId ? client.channel(channelId) : guild.channelCache[channelId] }

	Webhook retrieve() {
		client.requestWebhook(id, token)
	}

	Webhook edit(Map data) {
		client.editWebhook(data, this)
	}

	void delete() {
		client.deleteWebhook(this)
	}

	Message sendMessage(Map data) {
		client.sendMessage(data + [webhook: true], this)
	}

	Message sendMessage(content, boolean tts = false) {
		sendMessage(content: content, tts: tts)
	}

	Message send(Map data) { sendMessage(data) }
	Message send(content, boolean tts = false) { sendMessage(content, tts) }

	Message sendFile(Map data, implicatedFile, filename = null) {
		client.sendFile(data + [webhook: true], this, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}
}
