package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

@CompileStatic
class ParentListenerSystem extends ListenerSystem {
	def parent
	ParentListenerSystem(parent){ this.parent = parent }

	def parseEvent(param){
		parent.invokeMethod('parseEvent', param)
	}

	def listenerError(event, Throwable ex, Closure closure, data){
		parent.invokeMethod('listenerError', [event, ex, closure, data])
	}
}
