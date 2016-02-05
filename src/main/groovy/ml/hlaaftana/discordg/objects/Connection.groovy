package ml.hlaaftana.discordg.objects

class Connection extends DiscordObject {
	Connection(API api, Map object){ super(api, object) }

	List getIntegrations(){ return this.object["integrations"] }
	boolean isRevoked(){ return this.object["revoked"] }
	String getType(){ return this.object["type"] }
}
