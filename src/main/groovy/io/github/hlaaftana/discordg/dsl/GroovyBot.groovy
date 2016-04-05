package io.github.hlaaftana.discordg.dsl

import io.github.hlaaftana.discordg.objects.Client

class GroovyBot {
	Client client

	def client(Map data = [:], Closure closure){
		closure.delegate = new ClientBuilder(data)
		return closure()
	}
}
