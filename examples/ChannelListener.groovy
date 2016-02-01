import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("channel create") { Event e ->
	if (e.data.channel instanceof TextChannel && e.data.guild != null){
		e.data.channel.sendMessage("Hello there, new channel!")
	}
}
api.addListener("channel delete") { Event e ->
	if (e.data.guild != null)
		e.data.guild.defaultChannel.sendMessage("Looks like $e.data.channel.type channel \"$e.data.channel.name\" was deleted.")
}

api.addListener("channel update") { Event e ->
	Channel oldChan
	TextChannel chan
	if (e.data.channel instanceof TextChannel){
		chan = e.data.channel
		oldChan = e.data.guild.getTextChannelById(e.data.channel.id)
		chan.sendMessage("It seems this channel was modified.")
	}else{
		chan = e.data.guild.defaultChannel
		oldChan = e.data.guild.getVoiceChannelById(e.data.channel.id)
		chan.sendMessage("It seems voice channel $chan.name was modified.")
	}
}