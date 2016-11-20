package hlaaftana.discordg.collections

import java.util.Collection;
import java.util.Map;
import java.util.Set;

class DynamicMap implements Map {
	Closure keyConverter = Closure.IDENTITY
	Closure valueConverter = Closure.IDENTITY	List<DynamicEntry> entries = Collections.synchronizedList([])

	DynamicMap(){}

	int size(){
		entries.size()
	}

	boolean isEmpty(){
		entries.empty
	}

	boolean containsKey(ke){
		entries.any { keyConverter(ke) == it.key }
	}

	boolean containsValue(valu){
		entries.any { valueConverter(valu) == it.value }
	}

	def getEntry(ke){
		entries.find { it.key == keyConverter(ke) }
	}

	def getRaw(key){
		getEntry(key).rawValue
	}

	def get(key){
		getEntry(key).value
	}

	def put(key, value){
		if (containsKey(key)) remove(key)
		entries += new DynamicEntry(this, key, value)
	}

	def remove(key){
		entries.remove(getEntry(key))
	}

	void putAll(Map m){
		m.each { k, v ->
			put(k, v)
		}
	}

	void clear(){
		entries.clear()
	}

	Set keySet(){
		entries*.key as Set
	}

	Collection values(){
		entries*.value
	}

	Set entrySet(){
		entries as Set
	}

	static class DynamicEntry implements Entry {
		DynamicMap map
		def rawKey
		def rawValue
		DynamicEntry(DynamicMap map, rawKey, rawValue){ this.map = map; this.rawKey = rawKey; this.rawValue = rawValue }

		def getKey(){
			map.keyConverter(rawKey)
		}

		def getValue(){
			map.valueConverter(rawValue)
		}

		boolean equals(Entry other){
			other.key == this.key && other.value == this.value
		}

		def setValue(value) {
			rawValue = value
		}
	}
}
