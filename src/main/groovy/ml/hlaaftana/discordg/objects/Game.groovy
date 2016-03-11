package ml.hlaaftana.discordg.objects

class Game extends DiscordObject {
	Game(Client client, Map object){ super(client, object) }

	Map<String, List<String>> getExecutables(){ return this.object["executables"] }
	String getCommandLineOptions(){ return this.object["cmdline"] ?: "" }
}
