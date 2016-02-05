import ml.hlaaftana.discordg.APIBuilder
import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.util.bot.CommandBot
import ml.hlaaftana.discordg.util.bot.CommandBot.Command
import ml.hlaaftana.discordg.util.bot.CommandBot.ResponseCommand
import ml.hlaaftana.discordg.util.bot.CommandBot.ClosureCommand

API api = APIBuilder.build() // we build the api. if you want to configure the api,
// go ahead. make sure to use commandBotObject.api after constructing CommandBot though
CommandBot bot = new CommandBot(api) // notice how we don't log in yet

bot.defaultPrefix = ["!", "\$"] // can be a string or a list. i know it being a list
// doesn't make grammatical sense but who cares

// here we add an anonymous class extending Command. its only alias is "hello", and
// can also be a list. do note that after "hello" i can add a list or string as
// a list of prefixes / a prefix
bot.addCommand new Command("hello"){
	def run(Map d){
		d.sendMessage("Hello there, ${d.message.author.mention}!")
	}
}

// we add the response first since prefix values are optional
bot.addCommand new ResponseCommand("Hello there, but sadly I can't access the event data using this method of responding to commands. :'(", "hello")

// again, we add the response closure first since prefix values are optional
bot.addCommand new ClosureCommand({ Map d ->
	d.sendMessage("Hello there, ${d.message.author.mention}!")
}, "hello")

// here we login with the api and register our listeners.
// note that you can login before initializing and then call bot.initalize
// without any arguments.
bot.initialize("example@example.xmpl", "example")