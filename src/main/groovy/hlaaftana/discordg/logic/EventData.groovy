package hlaaftana.discordg.logic

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.LazyClosureMap

@CompileStatic
class EventData extends LazyClosureMap {
	String type
	EventData(type, Map data){ super(data); this.type = Client.parseEvent(type) }

	static EventData build(type, Map initial = [:], Closure ass){
		new EventData(type, create(initial, ass))
	}

	EventData clone(){
		EventData a = new EventData(type, [:])
		rawEach { k, v -> a[k] = v.clone() }
		a
	}

	/*def propertyMissing(String name){
		this[name]
	}

	def propertyMissing(String name, value){
		this[name] = value
	}*/

	def getProperty(String name){
		String methodName = 'get' + name.capitalize()
		if (methodName in getMetaClass().methods*.name) this.invokeMethod(methodName, null)
		else if (containsKey(name)) this[name]
		else throw new MissingPropertyException(name, this.class)//propertyMissing(name)
	}

	void setProperty(String name, value){
		String methodName = 'set' + name.capitalize()
		if (methodName in getMetaClass().methods*.name) this.invokeMethod(methodName, value)
		else if (containsKey(name)) this[name] = value
		else throw new MissingPropertyException(name, this.class)//propertyMissing(name, value)
	}

	def methodMissing(String name, args){
		if (containsKey(this)) this[name].invokeMethod('call', args)
		else throw new MissingMethodException(name, EventData, args as Object[])
	}
}
