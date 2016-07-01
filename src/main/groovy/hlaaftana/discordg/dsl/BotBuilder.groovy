package hlaaftana.discordg.dsl

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.bot.CommandBot

class BotBuilder {
	Map options
	CommandBot bot

	BotBuilder(Map options = [:]){
		bot = CommandBot.create(options)
		this.options = options
	}

	def command(){

	}

	def client(Map data = [:], Closure closure){
		ClientBuilder dad = new ClientBuilder(data)
		closure.delegate = dad
		closure()
		bot.client = dad.client
	}
}
