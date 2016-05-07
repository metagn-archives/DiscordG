package io.github.hlaaftana.discordg.dsl

import io.github.hlaaftana.discordg.objects.Events

class DelegatableEvent {
	Events type
	Map data
	DelegatableEvent(type, Map data){ this.type = Events.get(type); this.data = data }

	def getProperty(String name){
		return data[name]
	}

	def getAt(String name){
		return data[name]
	}

	void setProperty(String name, thing){
		data[name] = thing
	}

	def putAt(String name, thing){
		return data[name] = thing
	}

	def invokeMethod(String name, args){
		return data[name].call(args)
	}

	def invokeMethod(String name, ...args){
		return invokeMethod(name, args as List)
	}
}
