import hlaaftana.discordg.objects.*
import hlaaftana.discordg.DiscordG

BotClient client = DiscordG.withToken("token")
client.addListener(Events.CHANNEL) { Map d ->
	if (d.channel instanceof TextChannel && d.guild != null){
		d.channel.sendMessage("Hello there, new channel!")
	}
}
client.addListener(Events.CHANNEL_DELETE) { Map d ->
	if (d.guild != null)
		d.guild.sendMessage("Looks like $d.channel.type channel \"$d.channel.name\" was deleted.")
}

client.addListener(Events.CHANNEL_UPDATE) { Map d ->
	Channel oldChan
	TextChannel chan
	if (d.channel instanceof TextChannel){
		chan = d.channel
		oldChan = d.guild.textChannel(d.channel.id)
		chan.sendMessage("It seems this channel was modified.")
	}else{
		chan = d.guild.defaultChannel
		oldChan = d.guild.voiceChannel(d.channel.id)
		chan.sendMessage("It seems voice channel $chan.name was modified.")
	}
}