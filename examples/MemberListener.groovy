import hlaaftana.discordg.*

Client client = DiscordG.withToken args[0]

client.listener('member') {
	guild.sendMessage("Welcome to the guild, $member.mention!")
}

client.listener('member left') {
	guild.sendMessage("Aww, $member.mention left the guild.")
}

client.listener('member changed') {
	guild.sendMessage("I see you changed, $member.mention.")
}