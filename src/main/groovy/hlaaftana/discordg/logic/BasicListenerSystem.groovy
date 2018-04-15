package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

@CompileStatic
class BasicListenerSystem<T> extends ListenerSystem<T> {
	def parseEvent(param) { param }

	void listenerError(event, Throwable ex, Closure closure, T data) {
		throw new ListenerException(event, ex, closure, data)
	}
}

@CompileStatic
class ListenerException extends Exception {
	def event, data, closure
	ListenerException(e, Throwable ex, c, d) {
		super(ex.toString(), ex)
		event = e
		data = d
		closure = c
	}
}