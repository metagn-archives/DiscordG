package ml.hlaaftana.discordg.objects

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
	int getPermissions(){ return this.object["permissions"] }
	/**
	 * @return the position index for the role.
	 */
	int getPosition(){ return this.object["position"] }
}
