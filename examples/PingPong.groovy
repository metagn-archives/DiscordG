import hlaaftana.discordg.objects.*
import hlaaftana.discordg.*


Client client = DiscordG.withToken("token")

/*
 * Names like "message" and "sendMessage" directly correspond to values in the ".listener" method,
 * but need to be gathered from the first argument of the closure in the ".addListener" method.
 */
client.listener(Events.MESSAGE){
	if (message.content.startsWith("!ping")){
		sendMessage("Pong!")
	}
}
