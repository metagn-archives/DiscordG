import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("server create") { Map d ->
	d.server.defaultChannel.sendMessage("Hello there, new channel!")
}
api.addListener("server delete") { Map d ->
	println "It seems I left or was banned in/kicked out of " + d.server.name
}

api.addListener("server update") { Map d ->
	d.server.defaultChannel.sendMessage("Seems this server updated.")
}