package hlaaftana.discordg.objects

import java.awt.Color

/**
 * A Discord server role.
 * @author Hlaaftana
 */
class Role extends DiscordObject{
	static final Color DEFAULT = new Color(0)
	static final Color AQUA = new Color(0x1ABC9C)
	static final Color DARK_AQUA = new Color(0x11806a)
	static final Color GREEN = new Color(0x2ECC71)
	static final Color DARK_GREEN = new Color(0x1F8B4C)
	static final Color BLUE = new Color(0x3498DB)
	static final Color DARK_BLUE = new Color(0x206694)
	static final Color PURPLE = new Color(0x9B59B6)
	static final Color DARK_PURPLE = new Color(0x71368A)
	static final Color MAGENTA = new Color(0xE91E63)
	static final Color DARK_MAGENTA = new Color(0xAD1457)
	static final Color GOLD = new Color(0xF1C40F)
	static final Color DARK_GOLD = new Color(0xC27C0E)
	static final Color ORANGE = new Color(0xE67E22)
	static final Color DARK_ORANGE = new Color(0xA84300)
	static final Color RED = new Color(0xE74C3C)
	static final Color DARK_RED = new Color(0x992D22)
	static final Color LIGHT_GRAY = new Color(0x95A5A6)
	static final Color GRAY = new Color(0x607D8B)
	static final Color LIGHT_BLUE_GRAY = new Color(0x979C9F)
	static final Color BLUE_GRAY = new Color(0x546E7A)
	static final Color LIGHT_GREY = new Color(0x95A5A6)
	static final Color GREY = new Color(0x607D8B)
	static final Color LIGHT_BLUE_GREY = new Color(0x979C9F)
	static final Color BLUE_GREY = new Color(0x546E7A)
	static final MENTION_REGEX = { String id = /\d+/ -> /<@&$id>/ }

	Role(Client client, Map object){
		super(client, object)
	}

	/**
	 * @return the color for the role as an int.
	 */
	int getColorValue(){ return this.object["color"] }
	Color getColor(){ return new Color(this.object["color"]) }
	boolean isLocked(){
		return this.position >= this.server.me.primaryRole.position
	}
	/**
	 * @return whether the role is hoist or not.
	 */
	boolean isHoist(){ return this.object["hoist"] }
	/**
	 * @return whether the role is managed or not. I have no idea what this means.
	 */
	boolean isManaged(){ return this.object["managed"] }
	boolean isMentionable(){ return this.object["mentionable"] }
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

	String getMention(){ return "<@&${this.id}>" }
	String getMentionRegex(){ return MENTION_REGEX(id) }

	List<Member> getMembers(){ return this.server.members.findAll { this in it.roles } }
	Role edit(Map data){ return this.server.editRole(this, data) }
	void delete(){ this.server.deleteRole(this) }
	void addTo(Member user){ this.server.addRole(user, this) }
	void addTo(List<Member> users){ users.each { this.server.addRole(it, this) } }
}
