package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class Connection extends DiscordObject {
	List<Integration> getIntegrations(){ object["integrations"].collect { new Integration(client, it) } }
	boolean isRevoked(){ object["revoked"] }
	String getType(){ object["type"] }
}
