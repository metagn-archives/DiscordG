import metagn.discordg.*

client = DiscordG.withToken args[0]

client.listen('channel') {
	if (channel.text && !channel.private)
		channel.sendMessage("Hello there, new channel!")
}

client.listen('channel deleted') {
	if (guild) guild.sendMessage("Looks like $channel.type channel \"$channel.name\" was deleted.")
}

client.listen('channel changed') {
	if (channel.text)
		channel.sendMessage("Seems this channel changed. I like it.")
	else guild.sendMessage("Seems $channel.mention changed. I like it.")
}
