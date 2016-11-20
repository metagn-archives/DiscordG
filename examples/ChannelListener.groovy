import hlaaftana.discordg.objects.*
import hlaaftana.discordg.*

Client client = DiscordG.withToken("token")

client.listener(Events.CHANNEL){
	if (channel.text && !channel.private)
		channel.sendMessage("Hello there, new channel!")
}

client.listener(Events.CHANNEL_DELETE){
	if (server)
		server.sendMessage("Looks like $channel.type channel \"$channel.name\" was deleted.")
}

client.listener(Events.CHANNEL_UPDATE){
	if (channel.text)
		channel.sendMessage("Seems this channel changed. I like it.")
	if (channel.voice)
		server.sendMessage("Seems $channel.mention changed. I like it.")
}