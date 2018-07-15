package hlaaftana.discordg.collections

import groovy.transform.CompileStatic

@CompileStatic
class VoidMap<K, V> implements Map<K, V> {
	int size() { 0 }
	boolean isEmpty() { true }
	boolean containsKey(key) { false }
	boolean containsValue(value) { false }
	V get(key) { null }
	V put(K key, V value) { null }
	V remove(key) { null }
	void putAll(Map<? extends K, ? extends V> m) {}
	void clear() {}
	Set<K> keySet() { (Set<K>) Collections.emptySet() }
	Collection<V> values() { (Collection<V>) Collections.emptyList() }
	Set<Entry<K, V>> entrySet() { (Set<Entry<K, V>>) Collections.emptySet() }
}
