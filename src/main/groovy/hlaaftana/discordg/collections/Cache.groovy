package hlaaftana.discordg.collections

import groovy.transform.CompileStatic

@CompileStatic
class Cache<K, V> implements Map<K, V> {
	def root
	@Delegate(excludes = ['plus', 'getClass']) Map<K, V> store
	Cache(Map<K, V> store, root = null) { this.root = root; this.store = store }

	Map<K, V> store() { store }
	Map<K, V> store(Map<K, V> n) { store = n }

	static Cache empty(root = null) {
		new Cache([:], root)
	}

	def plus(Cache other) {
		new Cache(store + other.store(), root)
	}
}
