package hlaaftana.discordg.collections

import groovy.transform.CompileStatic
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.data.Snowflake

@CompileStatic
class Cache<T extends DiscordObject> implements Map<Snowflake, T>, Iterable<T> {
	@Delegate Map<Snowflake, T> store

	Cache(Map<Snowflake, T> map = new HashMap<>()) {
		store = Collections.synchronizedMap(map)
	}

	Cache(List<T> list) {
		this(superlister(list))
	}

	private static <T extends DiscordObject> Map<Snowflake, T> superlister(List<T> list) {
		def r = new LinkedHashMap(list.size())
		for (a in list) r.put(a.id, a)
		r
	}

	Map<Snowflake, T> map() {
		store
	}

	List<T> list() {
		store.values().collect()
	}

	T get(key) {
		get(Snowflake.from(key))
	}

	T get(Snowflake key) {
		store.get(key)
	}

	T add(T uh) {
		store.put(uh.id, uh)
	}

	List<T> addAll(Collection<T> uh) {
		def result = new ArrayList<T>()
		for (u in uh) result.add(store.put(u.id, u))
		result
	}

	T remove(key) {
		remove(Snowflake.from(key))
	}

	T remove(Snowflake key) {
		store.remove(key)
	}

	List<T> scoop(Collection<Snowflake> ids) {
		def result = new ArrayList<T>(ids.size())
		for (id in ids) result.add(get(id))
		result
	}

	Iterator<T> iterator() { store.values().iterator() }

	Iterator<Entry<Snowflake, T>> withId() { store.entrySet().iterator() }
}