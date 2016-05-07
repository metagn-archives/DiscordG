import hlaaftana.discordg.objects.*
import hlaaftana.discordg.DiscordG

Client client = DiscordG.withToken("token")
client.addListener(Events.MEMBER) { Map d ->
	d.guild.sendMessage("Welcome to the server, $d.member.mention!")
}
client.addListener(Events.MEMBER_LEFT) { Map d ->
	d.guild.sendMessage("Aww, $d.member.mention left the server.")
}

client.addListener(Events.MEMBER_UPDATE) { Map d ->
	d.guild.sendMessage("I see your roles changed, $d.member.mention.")
}