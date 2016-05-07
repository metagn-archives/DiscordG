package hlaaftana.discordg.objects

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.codehaus.groovy.runtime.InvokerHelper

class Cache implements Map { // god fuck me i guess
	def root
	Map store
	Cache(Map store, def root = null){ this.root = root; this.store = store }

	static Cache empty(def root = null){
		return new Cache([:], root)
	}

	def plus(Cache other){
		return new Cache(this.store + other.store, this.root)
	}

	def putAt(String key, value){
		return store[key] = value
	}

	def getAt(String key){
		return store[key]
	}

	def getProperty(String name){
		try{
			return this.invokeMethod("get${name.capitalize()}", null)
		}catch (MissingMethodException ex){
			try{
				return this.invokeMethod("is${name.capitalize()}", null)
			}catch (MissingMethodException ex2){
				return this.propertyMissing(name)
			}
		}
	}

	void setProperty(String name, value){
		try{
			this.invokeMethod("set${name.capitalize()}", value)
		}catch (MissingMethodException ex){
			this.propertyMissing(name, value)
		}
	}

	def propertyMissing(String name, value){
		return store[name] = value
	}

	def propertyMissing(String name){
		return store[name]
	}

	int size(){
		return store.size()
	}

	boolean isEmpty() {
		return store.isEmpty()
	}

	boolean containsKey(key) {
		return store.containsKey(key)
	}

	boolean containsValue(value) {
		return store.containsValue(value)
	}

	def get(key) {
		return store.get(key)
	}

	def put(key, value) {
		return store.put(key, value)
	}

	def remove(key) {
		return store.remove(key)
	}

	void putAll(Map m) {
		store.putAll(m)
	}

	void clear() {
		store.clear()
	}

	Set keySet() {
		return store.keySet()
	}

	Collection values() {
		return store.values()
	}

	Set entrySet() {
		return store.entrySet()
	}
}
