import hlaaftana.discordg.objects.*
import hlaaftana.discordg.*

Client client = DiscordG.withToken("token")

client.listener(Events.MEMBER){
	server.sendMessage("Welcome to the server, $member.mention!")
}

client.listener(Events.MEMBER_LEFT){
	server.sendMessage("Aww, $member.mention left the server.")
}

client.listener(Events.MEMBER_UPDATE){
	server.sendMessage("I see your roles changed, $member.mention.")
}