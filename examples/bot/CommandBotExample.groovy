import ml.hlaaftana.discordg.APIBuilder
import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.util.bot.CommandBot
import ml.hlaaftana.discordg.util.bot.CommandBot.*

API api = APIBuilder.build()
CommandBot bot = new CommandBot(api)

bot.defaultPrefix = ["!", "$"]

bot.addCommand new Command("hello"){
	def run(Event e){
		e.data.sendMessage("Hello there, ${e.data.message.author.mention}!")
	}
}

bot.addCommand new ResponseCommand("Hello there, but sadly I can't access the event data using this method of responding to commands. :'(", "hello")

bot.addCommand new ClosureCommand({ Event e, Command c ->
	e.data.sendMessage("Hello there, ${e.data.message.author.mention}!")
}, "hello")

bot.initialize("example@example.xmpl", "example")