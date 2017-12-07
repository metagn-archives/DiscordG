import hlaaftana.discordg.*

// This example doesn't really teach you anything about the API, just wanted to help beginners.
Client client
// This is the character before the message which the bot will use.
// No type is defined here, so methods can use this variable. (Scripts only)
prefix = "!"

static boolean isMessageCommand(String message, String command){
	// We're checking if the lowercase message plus a space starts with the prefix
	// for the commands, the lowercase command plus another space.
	// The reason we do the toLowerCase()s are because we want the command to not be
	// case-sensitive. Remove those if you want it to be case sensitive.
	// The reason we are adding a space is because if we didn't and the command string
	// was "ping", "!pingpong" would trigger as well.
	(message + " ").toLowerCase().startsWith(prefix.toString() + command.toLowerCase() + " ")
}

static String getCommandArgs(String message, String command){
	// If our message doesn't contain a space after the command, we would be returning
	// a string with -1 length which would throw an exception. If we catch an exception
	// after this, its reason is because of no arguments, so we return an empty string.
	try{
		// In this case, we would substring from the length of the prefix plus the
		// command plus a space until the end of the string.
		// Therefore the arguments of "!ping already" (considering our command
		// is "ping") would be "already".
		message.substring("$prefix$command ".length())
	}catch (ignored){
		""
	}
}

client = DiscordG.withToken args[0]
client.listen('message'){
	// Refer to isMessageCommand above to understand how it works.
	if (isMessageCommand(content, "ping")){
		// Refer to getCommandArgs above to understand how it works.
		sendMessage("Pong! In addition to ${prefix}ping, you said " + getCommandArgs(content, "ping"))
	}
}
