package hlaaftana.discordg.objects

import java.io.File;
import java.net.URL
import java.util.List;

import hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	static final MENTION_REGEX = { String id = /\d+/ -> /<@!?$id>/ }

	User(Client client, Map object){
		super(client, object)
	}

	List<Presence> getPresences(){ return this.sharedServers.collect { it.member(this).presence } - null }
	String getStatus(){ return this.presences[0]?.status ?: "offline" }
	Presence.Game getGame(){ return this.presences[0]?.game ?: "game" }
	boolean isOnline(){ return this.status == "online" }
	boolean isOffline(){ return this.status == "offline" }
	boolean isIdle(){ return this.status == "idle" }
	boolean isAway(){ return this.status == "idle" }
	String getMentionRegex(){ return MENTION_REGEX(id) }
	/**
	 * @return the user's username.
	 */
	String getName(){ return this.username }
	/**
	 * @return the user's username.
	 */
	String getUsername() { return this.object["username"] }
	/**
	 * @return the user's avatar's hash/ID.
	 */
	String getAvatarHash(){ return this.object["avatar"] ?: DefaultAvatars.get(Integer.parseInt(this.discriminator) % 5).hash }
	String getRawAvatarHash(){ return this.object["avatar"] }
	/**
	 * @return the user's avatar as a URL string.
	 */
	String getAvatar() { return (this.object["avatar"] != null) ? "https://cdn.discordapp.com/avatars/${this.id}/${this.avatarHash}.jpg"
		: "https://discordapp.com/assets/${this.avatarHash}.png" }
	/**
	 * @return the user's avatar as a URL object.
	 */
	URL getAvatarURL(){ return new URL(this.avatar) }
	URL getAvatarUrl(){ return new URL(this.avatar) }
	InputStream downloadAvatar(){ return client.requester.headerUp(this.avatarUrl).inputStream }
	String getDiscriminator(){ return this.object["discriminator"] }
	String getDiscrim(){ return this.object["discriminator"] }
	boolean isBot(){ return this.object["bot"] as boolean }
	/**
	 * @return a private channel for the user. If not created already, it'll create a new one.
	 */
	PrivateChannel getPrivateChannel(){
		for (pc in client.privateChannels){
			if (pc.user.id == this.id) return pc
		}
		PrivateChannel pc = new PrivateChannel(client, JSONUtil.parse(client.requester.post("users/@me/channels", [recipient_id: this.id])))
		return pc
	}

	List<Server> getSharedServers(){ return client.servers.findAll { it.members*.id.contains(this.id) } }
	/**
	 * @return a mention string for the user.
	 */
	String getMention(){ return "<@${this.id}>" }
	Member getMember(Server server){ return server.members.find { it.id == this.id } }
	Member member(Server server){ return server.members.find { it.id == this.id } }

	Message sendMessage(String message, boolean tts=false){ this.privateChannel.sendMessage(message, tts) }
	Message sendFile(File file){ this.privateChannel.sendFile(file) }
	Message sendFile(String filePath){ this.privateChannel.sendFile(filePath) }
}
