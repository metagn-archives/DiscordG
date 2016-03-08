package ml.hlaaftana.discordg.objects

class Connection extends DiscordObject {
	Connection(API api, Map object){ super(api, object) }

	List<Integration> getIntegrations(){ return this.object["integrations"].collect { new Integration(api, it) } }
	boolean isRevoked(){ return this.object["revoked"] }
	String getType(){ return this.object["type"] }
}
