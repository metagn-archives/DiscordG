package hlaaftana.discordg.collections

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.util.concurrent.Future
import java.util.concurrent.FutureTask

@CompileStatic
class LazyClosureMap<K, V> implements Map<K, V> {
	List<LazyEntry<K, V>> entries = Collections.synchronizedList([])

	LazyClosureMap() {}
	LazyClosureMap(Map<K, V> map) { putAll(map) }

	static LazyClosureMap create(Map<K, V> initial = [:], Closure c) {
		c.delegate = new LazyClosureMapBuilder(initial)
		c.resolveStrategy = Closure.OWNER_FIRST
		c()
		((LazyClosureMapBuilder) c.delegate).result
	}

	LazyEntry<K, V> addEntry(LazyEntry<K, V> entry) {
		int a = -1
		def k = entry.key
		def khc = k.hashCode()
		for (int i = 0; i < entries.size(); ++i) {
			def e = entries[i].key
			if (e.hashCode() == khc && e == k) {
				a = i
				break
			}
		}
		if (a >= 0) entries[a] = entry
		else entries.add entry
		entry
	}

	Closure<V> put(K key, boolean lazy = true, Closure<V> value) {
		addEntry(new LazyEntry(this, key, lazy ? value : { (V) value })).rawValue
	}

	Closure<V> put(K key, boolean lazy = true, V value) {
		put(key, Closure.IDENTITY.curry(value))
	}

	Closure<V> putClosure(K key, Closure<V> value) {
		put(key, false, value)
	}

	Closure<V> putAt(K key, boolean lazy = true, Closure<V> value) {
		put(key, lazy, value)
	}

	Closure<V> putAt(K key, boolean lazy = true, V ass) {
		put(key, lazy, ass)
	}

	Closure<V> putAt(String key, boolean lazy = true, Closure<V> value) {
		put((K) key, lazy, value)
	}

	Closure<V> putAt(String key, boolean lazy = true, V ass) {
		put((K) key, lazy, ass)
	}

	void putAll(Map m) {
		for (e in m) put((K) e.key, false, (V) e.value)
	}

	void putAll(LazyClosureMap<K, V> map) {
		for (e in map.getEntries()) put(e.key, true, e.rawValue)
	}

	def alias(K key, K... otherKeys) {
		for (int i = 0; i < otherKeys.length; ++i)
			this[otherKeys[i]] = getRaw(key)
	}

	LazyClosureMap<K, V> rawEach(Closure closure) {
		if (closure.maximumNumberOfParameters == 2) rawEntryTuples().each(closure)
		else entrySet().each(closure)
		this
	}

	def <T> List<T> rawCollect(Closure<T> closure) {
		if (closure.maximumNumberOfParameters == 2) rawEntryTuples().collect(closure)
		else entrySet().collect(closure)
	}

	boolean rawAny(Closure closure) {
		if (closure.maximumNumberOfParameters == 2) rawEntryTuples().any(closure)
		else entrySet().any(closure)
	}

	boolean rawEvery(Closure closure) {
		if (closure.maximumNumberOfParameters == 2) rawEntryTuples().every(closure)
		else entrySet().every(closure)
	}

	LazyEntry<K, V> rawFind(Closure closure) {
		if (closure.maximumNumberOfParameters == 2) {
			entrySet().find { closure([it.key, it.rawValue]) }
		} else entrySet().find(closure)
	}

	Set<LazyEntry<K, V>> rawFindAll(Closure closure) {
		if (closure.maximumNumberOfParameters == 2) {
			entrySet().findAll { closure([it.key, it.rawValue]) }
		} else entrySet().findAll(closure)
	}

	boolean containsKey(key) {
		int hash = key.hashCode()
		for (e in entries) if (e.key.hashCode() == hash && e.key == key) return true
		false
	}

	boolean containsRawValue(Closure<V> value) {
		for (e in entries) if (e.rawValue == value) return true
		false
	}

	boolean containsValue(value) {
		for (e in entries) if (e.value == value) return true
		false
	}

	int size() {
		entries.size()
	}

	boolean isEmpty() {
		entries.empty
	}

	Set<LazyEntry<K, V>> entrySet() {
		new HashSet<>(entries)
	}

	List<Tuple2<K, V>> entryTuples() {
		entrySet().collect { [it.key, it.value] as Tuple2 }
	}

	List<Tuple2<K, Closure<V>>> rawEntryTuples() {
		entrySet().collect { [it.key, it.rawValue] as Tuple2 }
	}

	Set<K> keySet() {
		entrySet()*.key as Set<K>
	}

	List<V> values() {
		entries*.value
	}

	List<Closure<V>> rawValues() {
		entries*.rawValue
	}

	LazyEntry<K, V> getEntry(K key) {
		int hash = key.hashCode()
		for (e in entries) if (e.key.hashCode() == hash && e.key == key) return e
		null
	}

	V get(key) {
		getEntry((K) key)?.value
	}

	V getAt(K key) {
		get(key)
	}

	V getAt(String key) {
		get((K) key)
	}

	V getProperty(String key) {
		get((K) key)
	}

	@CompileDynamic
	void setProperty(String key, value) {
		put((K) key, (V) value)
	}

	void setProperty(String key, Closure<V> value) {
		put((K) key, (V) value)
	}

	Closure<V> getRaw(K key) {
		getEntry(key)?.rawValue
	}

	LazyEntry<K, V> removeEntry(K key) {
		LazyEntry entry = getEntry(key)
		entries.remove(entry)
		entry
	}

	V remove(key) {
		removeEntry((K) key)?.value
	}

	Closure<V> removeRaw(K key) {
		removeEntry(key)?.rawValue
	}

	void clear() {
		entries.clear()
	}

	LazyClosureMap build() {
		LazyClosureMap ass = this
		ass.buildSelf()
	}

	LazyClosureMap buildSelf() {
		refreshAll()
		evaluateAll()
		this
	}

	Future buildSelfAsync() {
		new FutureTask(this.&buildSelf)
	}

	Future buildAsync() {
		new FutureTask(this.&build)
	}

	V evaluate(K key) {
		getEntry(key).value
	}

	List<V> evaluateAll() {
		entries*.value
	}

	V refresh(K key) {
		getEntry(key).refresh()
	}

	List<V> refreshAll() {
		entries*.refresh()
	}

	static class LazyEntry<K, V> implements Map.Entry<K, V> {
		LazyClosureMap<K, V> map
		private K key
		private Closure<V> value
		private V evaluatedValue
		boolean evaluated
		LazyEntry(LazyClosureMap<K, V> map, K key, Closure<V> value) { this.map = map; this.key = key; this.value = value }

		Closure<V> getRawValue() {
			this.@value
		}

		V getValue() {
			if (!evaluated) {
				evaluatedValue = this.@value(map)
				evaluated = true
			}
			evaluatedValue
		}

		Closure<V> setValue(boolean lazy = true, Closure<V> newValue) {
			map.put(key, lazy, newValue)
		}

		Closure<V> setValue(V newValue) {
			map.put(key, newValue)
		}

		K getKey() {
			this.@key
		}

		V refresh() {
			evaluated = false
			evaluatedValue
		}

		boolean equals(Map.Entry<K, V> other) {
			other.key == this.key && (
				(other instanceof LazyEntry ?
					((LazyEntry) other).rawValue == this.rawValue :
					false)
				|| other.value == this.value)
		}

		boolean equals(other) {
			other instanceof Map.Entry<K, V> &&
					equals((Map.Entry<K, V>) other)
		}
	}
}

@CompileStatic
class LazyClosureMapBuilder {
	LazyClosureMap result = [:]
	LazyClosureMapBuilder() {}
	LazyClosureMapBuilder(Map initial) { result << initial }

	def alias(o, ...g) {
		g.each { result[g] = result.getRaw(o) }
	}

	def methodMissing(String name, args) {
		if (args.class.array) args = ((Object[]) args).toList()
		if (args instanceof Collection) {
			if (args[0] instanceof Closure) {
				result[name] = args[0]
			} else if (args[0] instanceof Boolean && args[1] instanceof Closure) {
				result.put(name, (boolean) args[0], (Closure) args[1])
			} else if (args[0] instanceof Closure) {
				result.put(name, true, args[0])
			} else {
				result.put(name, args[0])
			}
		} else {
			methodMissing(name, [args])
		}
	}

	def propertyMissing(String name) {
		result[name]
	}

	def propertyMissing(String name, value) {
		methodMissing(name, value)
	}
}
