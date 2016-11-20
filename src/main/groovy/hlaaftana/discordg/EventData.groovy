package hlaaftana.discordg

import hlaaftana.discordg.util.LazyClosureMap
import org.codehaus.groovy.runtime.InvokerHelper

class EventData extends LazyClosureMap {
	Events type
	EventData(type, Map data){ super(data); this.type = Events.get(type) }

	static EventData create(type, Map initial = [:], Closure ass){
		new EventData(type, LazyClosureMap.create(initial, ass))
	}

	EventData clone(){
		EventData ass = new EventData(type, [:])
		this.each { LazyClosureMap.LazyEntry entry ->
			ass[entry.key] = entry.rawValue
		}
		ass
	}

	def propertyMissing(String name){
		this[name]
	}

	def propertyMissing(String name, value){
		this[name] = value
	}

	def getProperty(String name){
		String methodName = "get" + name.capitalize()
		if (methodName in getMetaClass().methods*.name) return this."$methodName"()
		else return propertyMissing(name)
	}

	void setProperty(String name, value){
		String methodName = "set" + name.capitalize()
		if (methodName in getMetaClass().methods*.name) this."$methodName"(value)
		else propertyMissing(name, value)
	}

	def methodMissing(String name, args){
		if (containsKey(this)) return this[name].call(args)
		else throw new MissingMethodException(name, EventData, args)
	}
}
