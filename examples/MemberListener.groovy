import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("guild member add") { Map d ->
	d.guild.defaultChannel.sendMessage("Welcome to the server, $d.member.mention!")
}
api.addListener("guild member remove") { Map d ->
	d.guild.defaultChannel.sendMessage("Aww, $d.member.mention left the server.")
}

api.addListener("guild member update") { Map d ->
	d.guild.defaultChannel.sendMessage("I see your roles changed, $d.member.mention.")
}