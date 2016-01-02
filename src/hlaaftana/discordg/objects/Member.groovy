package hlaaftana.discordg.objects

import java.util.List

import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class Member extends User{
	Member(API api, JSONObject object){
		super(api, object)
	}

	User getUser() {
		return new User(api, object.getJSONObject("user"))
	}

	Server getServer() {
		for (s in api.client.getServers()){
			if (s.getID().equals(object.getString("guild_id"))) return s
		}
	}

	List<Role> getRoles() {
		JSONArray array = object.getJSONArray("roles")
		List<Role> roles = new ArrayList<Role>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				for (r in this.getServer().getRoles()){
					if (r.getID().equals(array.get(i))) roles.add(r)
				}
			}catch (e){
				break
			}
		}
		return roles
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
