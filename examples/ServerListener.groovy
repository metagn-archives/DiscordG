import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("server create") { Event e ->
	e.data.server.defaultChannel.sendMessage("Hello there, new channel!")
}
api.addListener("server delete") { Event e ->
	println "It seems I left or was banned in/kicked out of " + e.data.server.name
}

api.addListener("server update") { Event e ->
	e.data.server.defaultChannel.sendMessage("Seems this server updated.")
}