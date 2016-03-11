package ml.hlaaftana.discordg.objects

class Emoji extends DiscordObject {
	Emoji(Client client, Map object){ super(client, object) }

	Server getServer(){ return client.getServerById(this.object["guild_id"]) }
	List<Role> getRoles(){ return this.server.roles.findAll { it.id in this.object["roles"] } }
	boolean requiresColons(){ return this.object["require_colons"] }
	boolean requireColons(){ return this.object["require_colons"] }
	boolean isRequiresColons(){ return this.object["require_colons"] }
	boolean isRequireColons(){ return this.object["require_colons"] }
	boolean isManaged(){ return this.object["managed"] }
}
