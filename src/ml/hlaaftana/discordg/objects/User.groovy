package ml.hlaaftana.discordg.objects

import java.net.URL

/**
 * A Discord user.
 * @author Hlaaftana
 */
class User extends Base{
	User(API api, Map object){
		super(api, object)
	}

	/**
	 * @return the user's username.
	 */
	String getName(){ return this.getUsername() }
	/**
	 * @return the user's username.
	 */
	String getUsername() { return object["username"] }
	/**
	 * @return the user's avatar's hash/ID.
	 */
	String getAvatarHash(){ return object["avatar"] }
	/**
	 * @return the user's avatar as a URL string.
	 */
	String getAvatar() {
		if (this.getAvatarHash() != null){
			return "https://discordapp.com/api/users/${this.getId()}/avatars/${this.getAvatarHash()}.jpg"
		}else{
			return ""
		}
	}
	/**
	 * @return the user's avatar as a URL object.
	 */
	URL getAvatarURL(){ return new URL(this.getAvatar()) }
	/**
	 * @return a private channel for the user. If not created already, it'll create a new one.
	 */
	PrivateChannel getPrivateChannel(){
		for (pc in api.client.getPrivateChannels()){
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
}
