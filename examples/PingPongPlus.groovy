import hlaaftana.discordg.objects.API
import hlaaftana.discordg.objects.Event
import hlaaftana.discordg.objects.Message
import hlaaftana.discordg.APIBuilder

// This example doesn't really teach you anything about the API, just wanted to help
// beginners.
class PingPongPlus {
	static API api
	// This is the character before the message which the bot will use.
	// You can change this to a string at your own will.
	static char prefix = '!'
	static main(args){
		api = APIBuilder.build("email@example.com", "password123")
		api.addListener("message create", { Event e ->
			// Doing this because it'll get annoying to refer to the same variable
			// in the same lengthy way later. Don't blame me, you're gonna end up
			// having special handling with this API anyway. Kinda the point of it
			// being so low-level.
			Message message = e.data["message"]
			
			// Refer to isMessageCommand below to understand how it works.
			if (isMessageCommand(message.getContent(), "ping")){
				// Refer to getCommandArgs below to understand how it works.
				message.getChannel().sendMessage("Pong! In addition to ${prefix}ping, you said " + getCommandArgs(message.getContent(), "ping"))
			}
		})
	}
	
	static boolean isMessageCommand(String message, String command){
		// We're checking if the lowercase message plus a space starts with the prefix
		// for the commands, the lowercase command plus another space.
		// The reason we do the toLowerCase()s are because we want the command to not be
		// case-sensitive. Remove those if you want it to be case sensitive.
		// The reason we are adding a space is because if we didn't and the command string
		// was "ping", "!pingas" would trigger as well. Wow, I haven't heard that word 
		// since 2009.
		return (message + " ").toLowerCase().startsWith(prefix + command.toLowerCase() + " ")
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
			return message.substring((prefix + command + " ").length())
		}catch (ex){
			return ""
		}
	}
}
