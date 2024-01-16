package metagn.discordg.dsl

import groovy.transform.CompileStatic
import metagn.discordg.util.bot.CommandBot

@CompileStatic
class BotBuilder {
	Map options
	@Delegate(interfaces = false) CommandBot bot

	BotBuilder(Map options = [:]) {
		bot = new CommandBot()
		for (e in options) bot.setProperty((String) e.key, e.value)
		this.options = options
	}

	def client(Map data = [:], Closure closure) {
		ClientBuilder dad = new ClientBuilder(data)
		closure.delegate = dad
		closure()
		bot.client = dad.client
	}
}
