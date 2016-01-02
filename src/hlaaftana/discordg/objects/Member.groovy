package hlaaftana.discordg.objects

import java.util.List

import java.net.URL
import org.json.JSONObject

class Member extends User{
	Member(API api, JSONObject object){
		super(api, object)
	}

	Member getMember(Server server) {
		return null
	}

	Server getServer() {
		return null
	}

	List<Role> getRoles() {
		return null
	}

	void editRoles(List<Role> roles) {

	}

	void addRoles(List<Role> roles) {

	}

	void removeRoles(List<Role> roles) {

	}

	void kick() {

	}
}
