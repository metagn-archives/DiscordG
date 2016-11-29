package hlaaftana.discordg.collections

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.codehaus.groovy.runtime.InvokerHelper

class Cache implements Map { // god fuck me i guess
	def root
	Map store
	Map<String, Closure> listeners = [:]
	Cache(Map store, root = null){ this.root = root; this.store = store }

	static Cache empty(root = null){
		new Cache([:], root)
	}

	def on(String event, Closure listener){
		listeners[event] != null ? listeners[event].add(listener) : (listeners[event] = [listener])
	}

	def call(String event, args){
		listeners[event] ? listeners[event]*.call(args) : null
	}

	def call(String event, ...args){
		listeners[event] ? listeners[event]*.call(args) : null
	}

	def plus(Cache other){
		new Cache(this.store + other.store, this.root)
	}

	def putAt(String key, value){
		if (this.containsKey(key)) this.call("modify", key, value)
		else this.call("add", key, value)
		store[key] = value
	}

	def getAt(String key){
		this.call("get", key)
		store[key]
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
		if (this.containsKey(name)) this.call("modify", name, value)
		else this.call("add", name, value)
		store[name] = value
	}

	def propertyMissing(String name){
		this.call("get", name)
		store[name]
	}

	int size(){
		store.size()
	}

	boolean isEmpty() {
		store.isEmpty()
	}

	boolean containsKey(key) {
		store.containsKey(key)
	}

	boolean containsValue(value) {
		store.containsValue(value)
	}

	def get(key) {
		this.call("get", key)
		store.get(key)
	}

	def put(key, value) {
		if (this.containsKey(key)) this.call("modify", key, value)
		else this.call("add", key, value)
		store.put(key, value)
	}

	def remove(key) {
		this.call("remove", key)
		store.remove(key)
	}

	void putAll(Map m) {
		store.putAll(m.each { key, value ->
			if (this.containsKey(key)) this.call("modify", key, value)
			else this.call("add", key, value)
		})
	}

	void clear() {
		this.keySet().each { this.call("remove", it) }
		store.clear()
	}

	Set keySet() {
		store.keySet()
	}

	Collection values() {
		store.values()
	}

	Set entrySet() {
		store.entrySet()
	}
}
