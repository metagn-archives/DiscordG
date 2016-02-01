import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("guild member add") { Event e ->
	e.data.guild.defaultChannel.sendMessage("Welcome to the server, $e.data.member.mention!")
}
api.addListener("guild member remove") { Event e ->
	e.data.guild.defaultChannel.sendMessage("Aww, $e.data.member.mention left the server.")
}

api.addListener("guild member update") { Event e ->
	e.data.guild.defaultChannel.sendMessage("I see your roles changed, $e.data.member.mention.")
}