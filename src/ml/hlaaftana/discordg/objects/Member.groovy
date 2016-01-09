package ml.hlaaftana.discordg.objects

import java.util.List
import java.net.URL

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A member of a server. Extends the User object.
 * @author Hlaaftana
 */
class Member extends User{
	Member(API api, Map object){
		super(api, object)
	}

	String getId(){ return this.getUser().getId() }
	String getName(){ return this.getUser().getName() }
	String getUsername() { return this.getUser().getUsername() }
	String getAvatarHash(){ return this.getUser().getAvatarHash() }
	String getAvatar() { return this.getUser().getAvatar() }
	URL getAvatarURL(){ return this.getUser().getAvatarURL() }
	/**
	 * @return the User which this Member is.
	 */
	User getUser(){ return new User(api, object["user"]) }
	/**
	 * @return the server the member comes from.
	 */
	Server getServer(){ return api.client.getServerById(object["guild_id"]) }
	/**
	 * @return a timestamp of when the member joined the server.
	 */
	String getJoinDate(){ return object["joined_at"] }

	/**
	 * @return the roles this member has.
	 */
	List<Role> getRoles(){
		List array = object["roles"]
		List<Role> roles = new ArrayList<Role>()
		for (o in array){
			for (r in this.getServer().getRoles()){
				if (o["id"].equals(r.getId())) roles.add(r)
			}
		}
		return roles
	}

	/**
	 * Overrides the roles for this member.
	 * @param roles - a list of roles to add.
	 */
	void editRoles(List<Role> roles) {
		this.getServer().editRoles(this, roles)
	}

	/**
	 * Adds roles to this member.
	 * @param roles - the roles to add.
	 */
	void addRoles(List<Role> roles) {
		this.getServer().addRoles(this, roles)
	}

	/**
	 * Kicks the member from its server.
	 */
	void kick() {
		this.getServer().kickMember(this)
	}
}
