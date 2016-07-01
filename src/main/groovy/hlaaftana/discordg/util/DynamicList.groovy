package hlaaftana.discordg.util

class DynamicList extends AbstractList {
	Closure converter = Closure.IDENTITY
	Map internal = [:]
	Map<String, Closure> listeners = [:]

	DynamicList(Map internal = [:], Closure converter = Closure.IDENTITY){
		this.internal = internal
		this.converter = converter
	}

	DynamicList(Collection internal, Closure converter = Closure.IDENTITY){
		this(internal.withIndex().collectEntries { value, int i -> [(i): value] }, converter)
	}

	static DynamicList "new"(Map internal = [:], Closure converter = Closure.IDENTITY){
		new DynamicList(internal, converter)
	}

	static DynamicList "new"(Collection internal, Closure converter = Closure.IDENTITY){
		new DynamicList(internal, converter)
	}

	def on(String event, Closure listener){
		if (listeners.containsKey(event)) listeners[event] += listener
		else listeners[event] = [listener]
	}

	def call(String event, args){
		listeners[event] ? listeners[event]*.call(args) : null
	}

	def call(String event, ...args){
		listeners[event] ? listeners[event]*.call(args) : null
	}

	int size(){ internal.size() }

	int getSize(){ size() }

	def find(String propertyName, String value){ this.find { it."$propertyName" == value } }

	def get(int index){ call("get", index); converter(getRaw(index)) }

	def getRaw(int index){ call("getRaw", index); index < 0 ? internal[size() + index] : internal[index] }

	def mod(int index){ getRaw(index) } // weird operator overloading i guess

	def set(int index, element){ call("set", element, index); internal[index] = element; converter(element) }

	void add(int index, element){
		call("add", element, index)
		Map ass = internal.findAll { k, v -> k >= index }.collectEntries { k, v -> [(++k): v] }
		Map da = internal.findAll { k, v -> k < index }
		internal = da + [(index): element] + ass
	}

	boolean addAll(Map map){ map.each { int k, v -> add(k + size(), v) }; true }

	boolean addAll(Collection collection){ collection.withIndex().each { int i, v -> add(i + size(), v) }; true }

	boolean contains(thing){ thing in internal.values() }

	boolean containsAll(Collection collection){ collection.every { this.contains(it) } }

	boolean matches(Collection collection){ toArrayList() == collection }

	boolean matches(Map map){ internal == map }

	boolean remove(thing){ call("remove", thing); internal.remove(internal.keySet().find { internal[it] == thing }) }

	def remove(int index){ call("remove", index); internal.remove(index) }

	def remove(IntRange indexes){ indexes.each { remove it } }

	boolean removeAll(Collection ugh){ ugh.each { remove it }; true }

	boolean isEmpty(){ internal.empty }

	DynamicList plus(other){
		DynamicList ass = this
		if (other instanceof Collection || other.class.array) ass.addAll other
		else ass.add other
		return ass
	}

	DynamicList leftShift(other){
		DynamicList ass = this
		ass.add other
		return ass
	}

	DynamicList minus(other){
		DynamicList ass = this
		if (other instanceof Collection || other.class.array) other.each { ass -= it }
		else ass.findAll { it == other }.withIndex().each { k, int v -> ass.remove(v) }
		return ass
	}

	DynamicList subList(int fromIndex, int toIndex){
		new DynamicList(internal.findAll { k, v -> k in (fromIndex..toIndex) }, converter)
	}

	DynamicList subList(int index){ subList(index, size()) }

	DynamicList until(int index){ subList(0, index) }

	ArrayList toArrayList(){ internal.sort { int k, v -> k }.values().collect { converter(it) } as ArrayList }

	Object[] toArray(){ toArrayList().toArray() }

	Object[] toArray(Object[] array){ toArrayList().toArray(array) }
}
