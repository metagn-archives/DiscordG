package io.github.hlaaftana.discordg.objects

/**
 * A Discord server role.
 * @author Hlaaftana
 */
class Role extends DiscordObject{
	Role(Client client, Map object){
		super(client, object)
	}

	/**
	 * @return the color for the role as an int.
	 */
	int getColorValue(){ return this.object["color"] }
	Color getColor(){ return new Color(this.object["color"]) }
	/**
	 * @return whether the role is hoist or not.
	 */
	boolean isHoist(){ return this.object["hoist"] }
	/**
	 * @return whether the role is managed or not. I have no idea what this means.
	 */
	boolean isManaged(){ return this.object["managed"] }
	/**
	 * @return the permission bits for this role as an int. I will replace this with a Permissions object later
	 */
	Permissions getPermissions(){ return new Permissions(this.object["permissions"]) }
	int getPermissionValue(){ return this.object["permissions"] }
	/**
	 * @return the position index for the role.
	 */
	int getPosition(){ return this.object["position"] }

	Server getServer(){ return client.server(this.object["guild_id"]) }

	List<Member> getMembers(){ return this.server.members.findAll { this in it.roles } }
	Role edit(Map data){ return this.server.editRole(this, data) }
	void delete(){ this.server.deleteRole(this) }
	void addTo(Member user){ this.server.addRole(user, this) }
	void addTo(List<Member> users){ users.each { this.server.addRole(it, this) } }
}
