package ml.hlaaftana.discordg.objects

/**
 * A Discord server role.
 * @author Hlaaftana
 */
class Role extends Base{
	Role(API api, Map object){
		super(api, object)
	}

	/**
	 * @return the color for the role as an int.
	 */
	int getColor(){ return object["color"] }
	/**
	 * @return whether the role is hoist or not.
	 */
	boolean isHoist(){ return object["hoist"] }
	/**
	 * @return whether the role is managed or not. I have no idea what this means.
	 */
	boolean isManaged(){ return object["managed"] }
	/**
	 * @return the permission bits for this role as an int. I will replace this with a Permissions object later
	 */
	int getPermissions(){ return object["permissions"] }
	/**
	 * @return the position index for the role.
	 */
	int getPosition(){ return object["position"] }
}
