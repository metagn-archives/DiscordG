import metagn.discordg.*

Client client = DiscordG.withToken args[0]

client.listen 'guild', {
	guild.sendMessage("Hello there, new guild!")
}

client.listen 'guild deleted', {
	println "It seems I left or was banned in/kicked out of $guild.name."
}

client.listen 'guild changed', {
	guild.sendMessage("Seems this guild updated.")
}
