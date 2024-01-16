package metagn.discordg.dsl

import groovy.transform.CompileStatic
import metagn.discordg.Client
import metagn.discordg.logic.EventData

@CompileStatic
class ClientBuilder {
	Map options
	@Delegate(excludes = ['toString', 'getClass', 'sendFile']) Client client

	ClientBuilder(Map options = [:]) {
		client = new Client()
		for (e in options) client.setProperty((String) e.key, e.value)
		this.options = options
	}

	def filter(a, b) { client.messageFilters.put(a, b) }
	int threadPoolSize(int x) { client.threadPoolSize = x }
	def shard(int id, int num) { client.shardTuple = new Tuple2<>(id, num) }

	def <T> Closure<T> listener(event, Closure<T> dung) {
		client.addListener(event) { Map d ->
			dung.delegate = new EventData(event, d)
			dung.resolveStrategy = Closure.DELEGATE_FIRST
			dung.call()
		}
	}
}
