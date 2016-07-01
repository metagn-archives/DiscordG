package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class Emoji extends DiscordObject {
	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }
	List<Role> getRoles(){ server.roles.findAll { it.id in object["roles"] } }
	boolean requiresColons(){ object["require_colons"] }
	boolean requireColons(){ object["require_colons"] }
	boolean isRequiresColons(){ object["require_colons"] }
	boolean isRequireColons(){ object["require_colons"] }
	boolean isManaged(){ object["managed"] }
}
