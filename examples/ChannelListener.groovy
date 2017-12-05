import hlaaftana.discordg.*

client = DiscordG.withToken args[0]

client.listener('channel') {
	if (channel.text && !channel.private)
		channel.sendMessage("Hello there, new channel!")
}

client.listener('channel deleted') {
	if (guild) guild.sendMessage("Looks like $channel.type channel \"$channel.name\" was deleted.")
}

client.listener('channel changed') {
	if (channel.text)
		channel.sendMessage("Seems this channel changed. I like it.")
	else guild.sendMessage("Seems $channel.mention changed. I like it.")
}