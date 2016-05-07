package io.github.hlaaftana.discordg.objects

import java.net.URL
import java.util.List;

import io.github.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	User(Client client, Map object){
		super(client, object)
	}

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
		PrivateChannel pc = new PrivateChannel(client, JSONUtil.parse(client.requester.post("https://discordapp.com/api/users/@me/channels", [recipient_id: this.id])))
		client.readyData["private_channels"].add(pc.object)
		return pc
	}
	List<UserServer> getServers(){
		return JSONUtil.parse(client.requester.get("https://discordapp.com/api/users/${this.id}/guilds")).collect { new UserServer(client, it) }
	}
	/**
	 * @return a mention string for the user.
	 */
	String getMention(){ return "<@${this.id}>" }
	Member getMember(Server server){ return server.members.find { it.id == this.id } }
	Member member(Server server){ return server.members.find { it.id == this.id } }

	static class UserServer extends DiscordObject {
		UserServer(Client client, Map object){ super(client, object) }

		/**
		 * @return the hash/ID of this server's icon.
		 */
		String getIconHash(){ return this.object["icon"] }
		/**
		 * @return the URL of the icon of this server as a string.
		 */
		String getIcon() {
			if (this.iconHash != null){
				return "https://cdn.discordapp.com/icons/${this.id}/${this.iconHash}.jpg"
			}else{
				return ""
			}
		}
		boolean isOwner(){ return this.object["owner"] }
	}
}
