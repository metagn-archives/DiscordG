import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.util.bot.*

CommandBot bot = new CommandBot(triggers: ["!", "\$"]) // notice how we don't log in yet

// here we add an anonymous class extending Command. its only alias is "hello", and
// can also be a list. do note that after "hello" i can add a list or string as
// a list of prefixes / a prefix
// the variables are the variables in the event data and
// 								methods inside Command which take
//								a Message or the event data as arguments
bot.command("hello"){
	sendMessage("Hello there, $author.mention!")
}

// here we login with the api and register our listeners.
// note that you can login before initializing and then call bot.initalize
// without any arguments.
bot.initialize("token")