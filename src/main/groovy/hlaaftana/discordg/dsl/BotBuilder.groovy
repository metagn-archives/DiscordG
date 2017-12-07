package hlaaftana.discordg.dsl

import groovy.transform.CompileStatic
import hlaaftana.discordg.util.bot.CommandBot

@CompileStatic
class BotBuilder {
	Map options
	@Delegate(interfaces = false) CommandBot bot

	BotBuilder(Map options = [:]){
		bot = new CommandBot(options)
		this.options = options
	}

	def client(Map data = [:], Closure closure){
		ClientBuilder dad = new ClientBuilder(data)
		closure.delegate = dad
		closure()
		bot.client = dad.client
	}
}
