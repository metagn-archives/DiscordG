import hlaaftana.discordg.*

Client client = DiscordG.withToken args[0]

client.listener 'guild', {
	guild.sendMessage("Hello there, new guild!")
}

client.listener 'guild deleted', {
	println "It seems I left or was banned in/kicked out of $guild.name."
}

client.listener 'guild changed', {
	guild.sendMessage("Seems this guild updated.")
}