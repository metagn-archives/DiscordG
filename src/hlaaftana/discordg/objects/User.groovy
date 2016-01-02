package hlaaftana.discordg.objects

import java.net.URL

class User extends Base{
	User(API api, Map object){
		super(api, object)
	}

	String getUsername() { return object["username"] }
	String getAvatarHash(){ return object["avatar"] }
	String getAvatar() {
		if (this.getAvatarHash() != null){
			return "https://discordapp.com/api/users/${this.getID()}/avatars/${this.getAvatarHash()}.jpg"
		}else{
			return ""
		}
	}
	URL getAvatarURL(){ return new URL(this.getAvatar()) }
}
