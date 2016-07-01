package hlaaftana.discordg

class ParentListenerSystem extends ListenerSystem {
	def parent
	ParentListenerSystem(parent){ this.parent = parent }

	def parseEvent(param){
		parent.parseEvent(param)
	}

	def listenerError(event, Throwable ex, Closure closure, data){
		parent.listenerError(event, ex, closure, data)
	}
}
