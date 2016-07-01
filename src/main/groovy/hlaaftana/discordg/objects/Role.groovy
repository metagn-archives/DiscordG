package hlaaftana.discordg.objects

import java.awt.Color

import hlaaftana.discordg.Client;

/**
 * A Discord server role.
 * @author Hlaaftana
 */
@groovy.transform.InheritConstructors
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

	int getColorValue(){ object["color"] }
	Color getColor(){ new Color(object["color"]) }
	boolean isLocked(){ isLockedFor(server.me) }
	boolean isLockedFor(user){
		position >= server.member(user).primaryRole.position
	}
	boolean isHoist(){ object["hoist"] }
	boolean isManaged(){ object["managed"] }
	boolean isMentionable(){ object["mentionable"] }
	Permissions getPermissions(){ new Permissions(object["permissions"]) }
	int getPermissionValue(){ object["permissions"] }
	int getPosition(){ object["position"] }

	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }

	String getMention(){ "<@&${id}>" }
	String getMentionRegex(){ MENTION_REGEX(id) }

	List<Member> getMembers(){ server.members.findAll { this in it.roles } }
	Role edit(Map data){ server.editRole(this, data) }
	void delete(){ server.deleteRole(this) }
	void addTo(Member user){ server.addRole(user, this) }
	void addTo(List<Member> users){ users.each { server.addRole(it, this) } }
}
