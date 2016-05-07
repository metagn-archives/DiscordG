package hlaaftana.discordg.dsl

import hlaaftana.discordg.objects.Events

class DelegatableEvent {
	Events type
	Map data
	DelegatableEvent(type, Map data){ this.type = Events.get(type); this.data = data }

	def propertyMissing(String name){
		if (data.containsKey(name))
			return data[name]
		else throw new MissingPropertyException(name, this.class)
	}

	void propertyMissing(String name, thing){
		if (data.containsKey(name))
			data[name] = thing
		else throw new MissingPropertyException(name, this.class)
	}

	def methodMissing(String name, args){
		if (data.containsKey(name))
			data[name].call(args)
		else throw new MissingMethodException(name, this.class, args)
	}
}
