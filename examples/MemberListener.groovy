import metagn.discordg.*

Client client = DiscordG.withToken args[0]

client.listen('member') {
	guild.sendMessage("Welcome to the guild, $member.mention!")
}

client.listen('member left') {
	guild.sendMessage("Aww, $member.mention left the guild.")
}

client.listen('member changed') {
	guild.sendMessage("I see you changed, $member.mention.")
}
