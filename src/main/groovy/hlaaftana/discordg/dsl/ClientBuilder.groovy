package hlaaftana.discordg.dsl

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.logic.EventData

@CompileStatic
class ClientBuilder {
	Map options
	@Delegate(excludes = ['client', 'toString', 'getClass', 'sendFile']) Client client

	ClientBuilder(Map options = [:]){
		client = new Client(options)
		this.options = options
	}

	def filter(a, b) { client.messageFilters.put(a, b) }
	int threadPoolSize(int x) { client.threadPoolSize = x }
	def shard(int id, int num) { client.shardTuple = [id, num] }

	def <T> Closure<T> listener(event, Closure<T> dung){
		client.addListener(event) { Map d ->
			dung.delegate = new EventData(event, d)
			dung.resolveStrategy = Closure.DELEGATE_FIRST
			dung.call()
		}
	}
}
