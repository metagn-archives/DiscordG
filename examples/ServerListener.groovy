import hlaaftana.discordg.objects.*
import hlaaftana.discordg.DiscordG

BotClient client = DiscordG.withToken("token")
client.addListener(Events.SERVER) { Map d ->
	d.server.sendMessage("Hello there, new channel!")
}
client.addListener(Events.SERVER_DELETE) { Map d ->
	println "It seems I left or was banned in/kicked out of $d.server.name"
}

client.addListener(Events.SERVER_UPDATE) { Map d ->
	d.server.defaultChannel.sendMessage("Seems this server updated.")
}