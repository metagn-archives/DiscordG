import hlaaftana.discordg.objects.*
import hlaaftana.discordg.*

Client client = DiscordG.withToken("token")

client.listener(Events.SERVER){
	server.sendMessage("Hello there, new server!")
}

client.listener(Events.SERVER_DELETE){
	println "It seems I left or was banned in/kicked out of $server.name."
}

client.listener(Events.SERVER_UPDATE){
	server.sendMessage("Seems this server updated.")
}