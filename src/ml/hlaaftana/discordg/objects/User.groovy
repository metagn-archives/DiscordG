package ml.hlaaftana.discordg.objects

import java.net.URL

class User extends Base{
	User(API api, Map object){
		super(api, object)
	}

	String getName(){ return this.getUsername() }
	String getUsername() { return object["username"] }
	String getAvatarHash(){ return object["avatar"] }
	String getAvatar() {
		if (this.getAvatarHash() != null){
			return "https://discordapp.com/api/users/${this.getId()}/avatars/${this.getAvatarHash()}.jpg"
		}else{
			return ""
		}
	}
	URL getAvatarURL(){ return new URL(this.getAvatar()) }
	PrivateChannel getPrivateChannel(){
		for (pc in api.client.getPrivateChannels()){
			if (pc.user.id == this.id) return pc
		}
		PrivateChannel pc = new PrivateChannel(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/users/$api.client.user.id/channels", [recipient_id: this.id])))
		api.readyData["private_channels"].add(pc.object)
		return pc
	}
	String getMention(){ return "<@${this.id}>" }
}
