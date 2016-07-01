package hlaaftana.discordg.objects

import java.io.File
import java.io.InputStream;
import java.net.URL
import java.util.List

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	static final MENTION_REGEX = { String id = /\d+/ -> /<@!?$id>/ }

	User(Client client, Map object, String concatUrl = ""){
		super(client, object, concatUrl)
	}

	List<Presence> getPresences(){ sharedServers.collect { it.member(this).presence } - null }
	String getStatus(){ presences[0]?.status ?: "offline" }
	Presence.Game getGame(){ presences[0]?.game }
	boolean isOnline(){ status == "online" }
	boolean isOffline(){ status == "offline" }
	boolean isIdle(){ status == "idle" }
	boolean isAway(){ status == "idle" }
	String getMentionRegex(){ MENTION_REGEX(id) }
	String getName(){ username }
	String getUsername() { object["username"] }
	String getAvatarHash(){ object["avatar"] ?: DefaultAvatars.get(Integer.parseInt(discriminator) % 5).hash }
	String getRawAvatarHash(){ object["avatar"] }
	String getAvatar() { (object["avatar"] != null) ? "https://cdn.discordapp.com/avatars/${id}/${avatarHash}.jpg"
		: "https://discordapp.com/assets/${avatarHash}.png" }
	InputStream getAvatarInputStream(){
		avatar.toURL().newInputStream(requestProperties:
			["User-Agent": client.fullUserAgent, Accept: "*/*"])
	}
	File downloadAvatar(File file){
		file.withOutputStream { out ->
			out << avatarInputStream
			new File(file.path)
		}
	}
	String getDiscriminator(){ object["discriminator"] }
	String getDiscrim(){ object["discriminator"] }
	String getNameAndDiscrim(){ "$name#$discrim" }
	boolean isBot(){ object["bot"] as boolean }
	PrivateChannel getPrivateChannel(){
		PrivateChannel ass = client.privateChannels.find { it.user.id == id }
		ass ?: new PrivateChannel(client,
			client.requester.jsonPost("users/@me/channels", [recipient_id: id]))
	}

	Permissions permissionsFor(Channel channel, Permissions initialPerms = Permissions.ALL_FALSE){
		if (channel.private) return Permissions.PRIVATE_CHANNEL
		Permissions doodle = initialPerms
		List allOverwrites = channel.permissionOverwrites.findAll { it.involves(this) }.sort { it.role ? it.affected.position : channel.server.roles.size() + 1 }
		for (Channel.PermissionOverwrite overwrite in allOverwrites){
			if (doodle["administrator"]){
				Permissions.CHANNEL_ALL_TRUE
			}
			doodle += overwrite.allowed
			doodle -= overwrite.denied
		}
		doodle
	}

	List<Server> getSharedServers(){ client.servers.findAll { it.members*.id.contains(id) } }
	String getMention(){ "<@${id}>" }
	Member getMember(server){ get(this, server, Member) }
	Member member(server){ get(this, server, Member) }

	Message sendMessage(String message, boolean tts=false){ this.privateChannel.sendMessage(message, tts) }
	Message sendFile(File file){ this.privateChannel.sendFile(file) }
	Message sendFile(String filePath){ this.privateChannel.sendFile(filePath) }
}
