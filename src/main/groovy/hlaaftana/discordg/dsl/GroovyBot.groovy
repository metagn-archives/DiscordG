package hlaaftana.discordg.dsl

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.util.bot.CommandBot

@CompileStatic
class GroovyBot {
	CommandBot bot
	Client client

	Client client(Map data = [:], @DelegatesTo(value = ClientBuilder, strategy = Closure.DELEGATE_FIRST)
								  Closure<Client> closure) {
		def builder = new ClientBuilder(data)
		closure.delegate = builder
		closure()
		if (null != bot) bot.client = builder.client
		client = builder.client
	}

	CommandBot bot(Map data = [:], @DelegatesTo(value = BotBuilder, strategy = Closure.DELEGATE_FIRST)
								   Closure<CommandBot> closure) {
		def builder = new BotBuilder(data)
		if (null != client) builder.client = client
		closure.delegate = builder
		closure()
		bot = builder.bot
	}
}
