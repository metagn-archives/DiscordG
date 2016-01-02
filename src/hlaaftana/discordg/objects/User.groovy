package hlaaftana.discordg.objects

import java.net.URL
import org.json.JSONObject

class User extends Base{
	JSONObject object
	User(JSONObject object){
		super(object)
		this.object = object
	}

	String getUsername() { return object.getString("username") }
	String getAvatarHash(){ return object.getString("avatar") }
	String getAvatar() {
		if (this.getAvatarHash() != null){
			return "https://discordapp.com/api/users/${this.getID()}/avatars/${this.getAvatarHash()}.jpg"
		}else{
			return ""
		}
	}
	URL getAvatarURL(){ return new URL(this.getAvatar()) }

	Member getMember(Server server) {
		return server.getMemberForUser(this)
	}
}
