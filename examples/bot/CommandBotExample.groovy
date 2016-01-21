import ml.hlaaftana.discordg.APIBuilder
import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.util.bot.CommandBot
import ml.hlaaftana.discordg.util.bot.CommandBot.*

API api = APIBuilder.build() // we build the api. if you want to configure the api,
// go ahead. make sure to use commandBotObject.api after constructing CommandBot though
CommandBot bot = new CommandBot(api) // notice how we don't log in yet

bot.defaultPrefix = ["!", "\$"] // can be a string or a list. i know it being a list
// doesn't make grammatical sense but who cares

// here we add an anonymous class extending Command. its only alias is "hello", and
// can also be a list. do note that after "hello" i can add a list or string as
// a list of prefixes / a prefix
bot.addCommand new Command("hello"){
	def run(Event e){
		e.data.sendMessage("Hello there, ${e.data.message.author.mention}!")
	}
}

// we add the response first since prefix values are optional
bot.addCommand new ResponseCommand("Hello there, but sadly I can't access the event data using this method of responding to commands. :'(", "hello")

// again, we add the response closure first since prefix values are optional
bot.addCommand new ClosureCommand({ Event e, Command c ->
	// you can have two or one arguments.
	// if you have two arguments, make sure to make the first one the event object,
	// and the second one a command object.
	// if you have one, make sure to make that one an event object.
	e.data.sendMessage("Hello there, ${e.data.message.author.mention}!")
}, "hello")

// here we login with the api and register our listeners.
// note that you can login before initializing and then call bot.initalize
// without any arguments.
bot.initialize("example@example.xmpl", "example")