package hlaaftana.discordg.objects

import java.util.List

import java.net.URL
import hlaaftana.discordg.util.JSONUtil

class Member extends User{
	Member(API api, Map object){
		super(api, object)
	}

	User getUser(){ return new User(api, object["user"]) }
	Server getServer(){ return api.client.getServerById(object["guild_id"]) }
	String getJoinDate(){ return object["joined_at"] }

	List<Role> getRoles(){
		List array = object["roles"]
		List<Role> roles = new ArrayList<Role>()
		for (o in array){
			for (r in this.getServer().getRoles()){
				if (o["id"].equals(r.getID())) roles.add(r)
			}
		}
		return roles
	}

	void editRoles(List<Role> roles) {
		this.getServer().editRoles(this, roles)
	}

	void addRoles(List<Role> roles) {
		this.getServer().addRoles(this, roles)
	}

	void kick() {
		this.getServer().kickMember(this)
	}
}
