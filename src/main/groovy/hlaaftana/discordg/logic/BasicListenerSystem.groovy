package hlaaftana.discordg.logic

class BasicListenerSystem extends ListenerSystem {
	def parseEvent(param){ param }

	def listenerError(event, Throwable ex, Closure closure, data){
		throw new ListenerException(event, ex, closure, data)
	}
}

class ListenerException extends Exception {
	def event, data, closure
	ListenerException(e, Throwable ex, c, d){
		super(ex.toString(), ex)
		event = e
		data = d
		closure = c
	}
}