package ml.hlaaftana.discordg.objects

class Emoji extends DiscordObject {
	Emoji(API api, Map object){ super(api, object) }

	Server getServer(){ return api.client.getServerById(this.object["guild_id"]) }
	List<Role> getRoles(){ return this.server.roles.findAll { it.id in this.object["roles"] } }
	boolean requiresColons(){ return this.object["require_colons"] }
	boolean requireColons(){ return this.object["require_colons"] }
	boolean isRequiresColons(){ return this.object["require_colons"] }
	boolean isRequireColons(){ return this.object["require_colons"] }
	boolean isManaged(){ return this.object["managed"] }
}
