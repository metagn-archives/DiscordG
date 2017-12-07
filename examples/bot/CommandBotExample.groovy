import hlaaftana.discordg.util.bot.CommandBot
import hlaaftana.discordg.util.bot.CommandEventData

CommandBot bot = new CommandBot(triggers: ["!", "\$"])

bot.command("hello") {
	respond "Hello there, $author.mention!"
}

bot.formatter = { "| $it |" }

bot.command(["say", "repeat", ~/echo+/]) {
	formatted arguments
}

bot.command "mfw", category: "meme", {
	assert command.info.category == "meme"
	respond ">mfw $arguments"
	delete()
}

bot.command("whatsmyname") {
	respond username
}.extraArgs.username = { CommandEventData it -> it.private ? it.member.nick : it.author.name }

bot.initialize args[0] // token