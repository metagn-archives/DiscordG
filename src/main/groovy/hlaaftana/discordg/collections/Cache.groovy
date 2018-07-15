package hlaaftana.discordg.collections

import groovy.transform.CompileStatic

@CompileStatic
class Cache<K, V> implements Map<K, V> {
	def root
	@Delegate(excludes = ['plus', 'getClass']) Map<K, V> store
	Cache(Map<K, V> store = Collections.<K, V>emptyMap(), root = null) {
		this.root = root
		this.store = Collections.synchronizedMap(store)
	}

	Map<K, V> store() { store }
	Map<K, V> store(Map<K, V> n) { store = n }

	static <K, V> Cache<K, V> empty(root = null) {
		new Cache<K, V>(Collections.synchronizedMap(Collections.<K, V>emptyMap()), root)
	}

	Cache plus(Cache other) {
		new Cache(store + other.store(), root)
	}
}
