import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.APIBuilder

API api = APIBuilder.build("example@example.com", "example123")
api.addListener("channel create") { Map d ->
	if (d.channel instanceof TextChannel && d.guild != null){
		d.channel.sendMessage("Hello there, new channel!")
	}
}
api.addListener("channel delete") { Map d ->
	if (d.guild != null)
		d.guild.defaultChannel.sendMessage("Looks like $d.channel.type channel \"$d.channel.name\" was deleted.")
}

api.addListener("channel update") { Map d ->
	Channel oldChan
	TextChannel chan
	if (d.channel instanceof TextChannel){
		chan = d.channel
		oldChan = d.guild.getTextChannelById(d.channel.id)
		chan.sendMessage("It seems this channel was modified.")
	}else{
		chan = d.guild.defaultChannel
		oldChan = d.guild.getVoiceChannelById(d.channel.id)
		chan.sendMessage("It seems voice channel $chan.name was modified.")
	}
}