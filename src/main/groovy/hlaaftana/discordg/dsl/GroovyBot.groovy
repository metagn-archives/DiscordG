package hlaaftana.discordg.dsl

import hlaaftana.discordg.Client;

class GroovyBot {
	Client client

	def client(Map data = [:], Closure closure){
		closure.delegate = new ClientBuilder(data)
		return closure()
	}
}
