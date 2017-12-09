package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

@CompileStatic
class ParentListenerSystem<T> extends ListenerSystem<T> {
	def parent
	ParentListenerSystem(parent){ this.parent = parent }

	def parseEvent(param){
		parent.invokeMethod('parseEvent', param)
	}

	def listenerError(event, Throwable ex, Closure closure, T data){
		parent.invokeMethod('listenerError', [event, ex, closure, data])
	}
}
