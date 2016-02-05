package ml.hlaaftana.discordg.objects

import java.net.URL

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends DiscordObject{
	User(API api, Map object){
		super(api, object)
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
	String getAvatarHash(){ return this.object["avatar"] ?: DefaultAvatars.get(Integer.parseInt(this.discriminator) % 5) }
	String getRawAvatarHash(){ return this.object["avatar"] }
	/**
	 * @return the user's avatar as a URL string.
	 */
	String getAvatar() { return "https://discordapp.com/api/users/${this.id}/avatars/${this.avatarHash}.jpg" }
	/**
	 * @return the user's avatar as a URL object.
	 */
	URL getAvatarURL(){ return new URL(this.avatar) }
	URL getAvatarUrl(){ return new URL(this.avatar) }
	String getDiscriminator(){ return this.object["discriminator"] }
	String getDiscrim(){ return this.object["discriminator"] }
	/**
	 * @return a private channel for the user. If not created already, it'll create a new one.
	 */
	PrivateChannel getPrivateChannel(){
		for (pc in api.client.privateChannels){
			if (pc.user.id == this.id) return pc
		}
		PrivateChannel pc = new PrivateChannel(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/users/$api.client.user.id/channels", [recipient_id: this.id])))
		api.readyData["private_channels"].add(pc.object)
		return pc
	}
	/**
	 * @return a mention string for the user.
	 */
	String getMention(){ return "<@${this.id}>" }
	Member getMember(Server server){ return server.members.find { it.id == this.id } }
	Member member(Server server){ return server.members.find { it.id == this.id } }
}
