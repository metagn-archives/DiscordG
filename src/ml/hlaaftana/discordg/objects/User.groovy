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
	String getMention(){ return "<@${this.id}>" }
}
