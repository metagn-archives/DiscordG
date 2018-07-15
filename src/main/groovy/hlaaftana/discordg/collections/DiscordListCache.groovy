package hlaaftana.discordg.collections

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Snowflake

@CompileStatic
class DiscordListCache<T extends DiscordObject> extends Cache<Snowflake, T> {
	Client client
	DiscordListCache(List<T> list, Client client) {
		super(superlister(list), client)
		this.client = client
	}

	private static Map<Snowflake, T> superlister(List<T> list) {
		def r = new LinkedHashMap(list.size())
		for (a in list) r.put(a.id, a)
		r
	}

	Client client() { client }

	Map<Snowflake, T> map() {
		store
	}

	List<T> list() {
		store.values().collect()
	}

	T get(key) {
		super.get(Snowflake.from(key))
	}

	T add(T uh) {
		store.put(uh.id, uh)
	}

	T remove(key) {
		store.remove(Snowflake.from(key))
	}
}