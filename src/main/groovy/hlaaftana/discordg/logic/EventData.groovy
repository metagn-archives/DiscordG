package hlaaftana.discordg.logic

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.LazyClosureMap

@CompileStatic
class EventData extends LazyClosureMap<String, Object> {
	String type
	EventData(type, Map data){ super(data); this.type = Client.parseEvent(type) }

	EventData clone(){
		EventData a = new EventData(type, [:])
		rawEach { String k, v -> a[k] = (Closure) v.clone() }
		a
	}

	def <T> T define(String name, T obj = null) {
		put name, obj
	}

	def getProperty(String name){
		String methodName = 'get' + name.capitalize()
		if (methodName in getMetaClass().methods*.name) this.invokeMethod(methodName, null)
		else if (containsKey(name)) get name
		else throw new MissingPropertyException(name, this.class)//propertyMissing(name)
	}

	void setProperty(String name, value){
		String methodName = 'set' + name.capitalize()
		if (methodName in getMetaClass().methods*.name) this.invokeMethod(methodName, value)
		else if (containsKey(name)) put name, value
		else throw new MissingPropertyException(name, this.class)//propertyMissing(name, value)
	}

	def methodMissing(String name, args){
		if (containsKey(this)) get(name).invokeMethod('call', args)
		else throw new MissingMethodException(name, EventData, args as Object[])
	}
}
