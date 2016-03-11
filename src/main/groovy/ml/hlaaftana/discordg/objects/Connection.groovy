package ml.hlaaftana.discordg.objects

class Connection extends DiscordObject {
	Connection(Client client, Map object){ super(client, object) }

	List<Integration> getIntegrations(){ return this.object["integrations"].collect { new Integration(client, it) } }
	boolean isRevoked(){ return this.object["revoked"] }
	String getType(){ return this.object["type"] }
}
