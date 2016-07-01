package hlaaftana.discordg.util

import java.util.concurrent.Future
import java.util.concurrent.FutureTask

class LazyClosureMap implements Map {
	List entries = Collections.synchronizedList([])

	LazyClosureMap(){}
	LazyClosureMap(Map map){ putAll(map) }

	static LazyClosureMap create(args = null, Closure ass){
		Closure copy = ass.clone()
		copy.delegate = new LazyClosureMapBuilder()
		copy.resolveStrategy = Closure.DELEGATE_FIRST
		copy(args)
		copy.delegate.result
	}

	LazyEntry addEntry(LazyEntry entry){
		def ass = entries.withIndex().find { it, int i -> it.key == entry.key }
		if (ass){
			entries[ass[1]] = entry
		}else{
			entries.add entry
		}
		entry
	}

	def put(key, boolean lazy = true, Closure value){
		if (lazy){
			return addEntry(new LazyEntry(this, key, value)).rawValue
		}else{
			return addEntry(new LazyEntry(this, key, { value })).rawValue
		}
	}

	def put(key, boolean lazy = true, value){
		put(key){ value }
	}

	Closure putClosure(key, Closure value){
		put(key, false, value)
	}

	Closure putAt(key, boolean lazy = true, Closure value){
		put(key, lazy, value)
	}

	Closure putAt(key, boolean lazy = true, ass){
		put(key, lazy, ass)
	}

	Closure putAt(String key, boolean lazy = true, Closure value){
		put(key, lazy, value)
	}

	Closure putAt(String key, boolean lazy = true, ass){
		put(key, lazy, ass)
	}

	void putAll(Map m) {
		m.each { k, v ->
			put(k, false, v)
		}
	}

	void putAll(LazyClosureMap map){
		map.rawEach { k, Closure v ->
			put(k, true, v)
		}
	}

	LazyClosureMap rawEach(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			this.rawEntryTuples().each { closure(it) }
		}else{
			this.entrySet().each { closure(it) }
		}
		this
	}

	List rawCollect(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			return this.rawEntryTuples().collect { closure(it) }
		}else{
			return this.entrySet().collect { closure(it) }
		}
	}

	boolean rawAny(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			return this.rawEntryTuples().any { closure(it) }
		}else{
			return this.entrySet().any { closure(it) }
		}
	}

	boolean rawEvery(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			return this.rawEntryTuples().every { closure(it) }
		}else{
			return this.entrySet().every { closure(it) }
		}
	}

	LazyEntry rawFind(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			return this.entrySet().find { closure([it.key, it.rawValue]) }
		}else{
			return this.entrySet().find { closure(it) }
		}
	}

	List rawFindAll(Closure closure){
		if (closure.maximumNumberOfParameters == 2){
			return this.entrySet().findAll { closure([it.key, it.rawValue]) }
		}else{
			return this.entrySet().findAll { closure(it) }
		}
	}

	boolean containsKey(key){
		key in entries*.key
	}

	boolean containsRawValue(Closure value){
		value in entries*.rawValue
	}

	boolean containsValue(value) {
		value in entries*.value
	}

	int size() {
		entries.size()
	}

	boolean isEmpty() {
		entries.empty
	}

	Set entrySet(){
		entries as Set
	}

	List entryTuples(){
		entrySet().collect { [it.key, it.value] as Tuple2 }
	}

	List rawEntryTuples(){
		entrySet().collect { [it.key, it.rawValue] as Tuple2 }
	}

	Set keySet(){
		entrySet()*.key
	}

	Collection values(){
		entries*.value
	}

	Collection rawValues(){
		entries*.rawValue
	}

	LazyEntry getEntry(key){
		entries.find { it.key == key }
	}

	def get(key){
		getEntry(key)?.value
	}

	def getAt(key){
		get(key)
	}

	def getAt(String key){
		get(key)
	}

	def getProperty(String key){
		get(key)
	}

	void setProperty(String key, value){
		put(key, value)
	}

	Closure getRaw(key){
		getEntry(key)?.rawValue
	}

	LazyEntry removeEntry(key){
		LazyEntry entry = getEntry(key)
		entries.remove(entry)
		entry
	}

	def remove(key){
		removeEntry(key)?.value
	}

	Closure removeRaw(key){
		removeEntry(key)?.rawValue
	}

	void clear(){
		entries.clear()
	}

	LazyClosureMap build(){
		LazyClosureMap ass = this
		ass.buildSelf()
	}

	LazyClosureMap buildSelf(){
		this.refreshAll()
		this.evaluateAll()
		this
	}

	Future buildSelfAsync(){
		new FutureTask(this.&buildSelf)
	}

	Future buildAsync(){
		new FutureTask(this.&build)
	}

	def evaluate(key){
		getEntry(key).value
	}

	def evaluateAll(){
		entries*.value
	}

	def refresh(key){
		getEntry(key).refresh()
	}

	def refreshAll(){
		entries*.refresh()
	}

	static class LazyEntry implements Map.Entry {
		LazyClosureMap map
		private key
		private Closure value
		private evaluatedValue
		boolean evaluated
		LazyEntry(LazyClosureMap map, key, Closure value){ this.map = map; this.key = key; this.value = value }

		Closure getRawValue(){
			return this.@value
		}

		def getValue(){
			if (!evaluated){
				evaluatedValue = this.@value(map)
				evaluated = true
			}
			evaluatedValue
		}

		Closure setValue(boolean lazy = true, Closure newValue){
			map.put(key, lazy, newValue)
		}

		Closure setValue(newValue){
			map.put(key, newValue)
		}

		def getKey(){
			this.@key
		}

		def refresh(){
			evaluated = false
			evaluatedValue
		}

		boolean equals(Map.Entry other){
			other.key == this.key && (
				(other instanceof LazyEntry ?
					other.rawValue == this.rawValue :
					false)
				|| other.value == this.value)
		}

		boolean equals(other){ false }
	}
}

class LazyClosureMapBuilder {
	LazyClosureMap result = [:]
	LazyClosureMapBuilder(){}
	LazyClosureMapBuilder(LazyClosureMap initial){ result << initial }

	def methodMissing(String name, args){
		if (args.class.array || args instanceof Collection){
			if (args[0] instanceof Closure){
				result[name] = args[0]
			}else if (args[0] instanceof Boolean && args[1] instanceof Closure){
				result.put(name, args[0], args[1])
			}else if (args[0] instanceof Closure){
				result.put(name, true, args[0])
			}else{
				result.put(name, args[0])
			}
		}else{
			methodMissing(name, [args])
		}
	}
}
