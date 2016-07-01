package hlaaftana.discordg.dsl

import hlaaftana.discordg.Client
import hlaaftana.discordg.EventData;
import hlaaftana.discordg.Events;
import hlaaftana.discordg.objects.*

class ClientBuilder {
	Map options
	Client client

	ClientBuilder(Map options = [:]){
		client = new Client(options)
		this.options = options
	}

	def listener(event, Closure dung){
		Events e = Events.get(event)
		client.addListener(e) { Map d ->
			dung.delegate = new EventData(e, d)
			dung.resolveStrategy = Closure.DELEGATE_FIRST
			dung()
		}
	}

	def login(String email, String password, boolean threaded = true){
		if (client.bot){
			throw new IllegalArgumentException("Tried to login with an email and password on a bot account")
		}
		client.login(email, password, threaded)
	}

	def login(String token, boolean threaded = true){
		client.login(token, threaded)
	}

	def login(String customBotName, Closure requestToken, boolean threaded = true){
		if (!client.bot){
			throw new IllegalArgumentException("Tried to use bot account logging in method on regular account")
		}
		client.login(customBotName, requestToken, threaded)
	}
}
