import metagn.discordg.*

Client client = DiscordG.withToken args[0]

/*
 * Names like "message" and "sendMessage" directly correspond to values in the ".listener" method,
 * but need to be gathered from the first argument of the closure in the ".addListener" method.
 */
client.listen('message') {
	if (content.startsWith("!ping")) respond "Pong!"
}
