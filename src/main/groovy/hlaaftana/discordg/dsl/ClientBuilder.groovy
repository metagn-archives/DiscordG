package hlaaftana.discordg.dsl

import hlaaftana.discordg.oauth.BotClient
import hlaaftana.discordg.objects.*

class ClientBuilder {
	Map options
	Client client

	ClientBuilder(Map options = [:]){
		if (options.bot){
			client = new BotClient(options)
		}else{
			client = new Client(options)
		}
		this.options = options
	}

	def listener(event, Closure dung){
		Events e = Events.get(event)
		client.addListener(e) { Map d ->
			dung.delegate = new DelegatableEvent(e, d)
			dung.resolveStrategy = Closure.DELEGATE_FIRST
			dung()
		}
	}

	def login(String email, String password, boolean threaded = true){
		if (client instanceof BotClient){
			throw new IllegalArgumentException("Tried to login with an email and password on a bot account")
		}
		client.login(email, password, threaded)
	}

	def login(String token, boolean threaded = true){
		client.login(token, threaded)
	}

	def login(String customBotName, Closure requestToken, boolean threaded = true){
		if (!(client instanceof BotClient)){
			throw new IllegalArgumentException("Tried to use bot account logging in method on regular account")
		}
		((BotClient) client).login(customBotName, requestToken, threaded)
	}
}
