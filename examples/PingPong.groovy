import hlaaftana.discordg.objects.*
import hlaaftana.discordg.DiscordG

// This part should be easy to understand.
BotClient client = DiscordG.withToken("token")
/*
 * Here we add a closure to listen to the event "MESSAGE_CREATE".
 *
 * Notice how I wrote "message create" instead of "MESSAGE_CREATE", however.
 * What the API does to convert that string to the actual event name is:
 * 1. Making it uppercase,
 * 2. Replacing spaces with underscores,
 * 3. And replacing "change" or "CHANGE" with "update" or "UPDATE" respectively.
 * So we could write "MESSAGE_UPDATE" as "message change" or "message_update"
 * and many other possibilities.
 *
 * The closure takes one argument which is an event.
 * The event object contains a Map variable, data, as the event's variables.
 * Different events usually have different data in that variable.
 * I will add a list for them somewhere later.
 * You can check
 * https://github.com/hlaaftana/DiscordG/blob/master/src/hlaaftana/discordg/request/WSClient.groovy#L89-L236
 * for the data yourself however.
 * And, to add to that, every event has the raw JSON data in them to
 * which is "d["fullData"]" or "d.fullData".
 * You can read what the raw JSON data for each event is by checking the
 * "d" objects in each event here:
 * http://hornwitser.no/discord/analysis
 */
client.addListener(Events.MESSAGE) { Map d ->
	// MESSAGE_CREATE's data is just one Message object called "message".
	// We can get the content of that message with Message#getContent() which Groovy fills in.
	if (d.message.content.startsWith("!ping")){
		// Note that you can replace "d.message" with "d["message"]".
		d.sendMessage("Pong!")
		// Simple as that! You could even send the message with TTS by doing:
		// d.sendMessage("Pong!", true)
	}
}
// Sidenote, instead of "APIBuilder.build("email", "password")" above, you could have typed
// "APIBuilder.build()" and done "api.login("email", "password")" here. This could be useful
// to not miss some debug logs or register listeners late, but I doubt that will happen.
