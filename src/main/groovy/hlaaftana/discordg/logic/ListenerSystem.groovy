package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

@CompileStatic
abstract class ListenerSystem<T> {
	Map<Object, List<Closure>> listeners = [:]

	abstract parseEvent(param)

	abstract listenerError(event, Throwable ex, Closure closure, T data)

	Closure submit(event, boolean temporary = false, @DelegatesTo(T) Closure closure){
		addListener(event, temporary, closure)
	}

	Closure addListener(event, boolean temporary = false, Closure closure) {
		event = parseEvent(event)
		if (temporary) closure = { Map d -> closure(d); listeners.get(event).remove(closure) }
		if (listeners.containsKey(event)) listeners[event] = listeners[event] + closure
		else listeners[event] = [closure]
		closure
	}
	
	Closure listen(event, boolean temporary = false, @DelegatesTo(T) Closure closure){
		Closure ass
		ass = { T d, Closure internal ->
			Closure copy = (Closure) closure.clone()
			copy.delegate = d
			copy.parameterTypes.size() == 2 ? copy(copy.delegate, internal) : copy(copy.delegate)
		}
		ass.resolveStrategy = Closure.DELEGATE_FIRST
		addListener(event, temporary, ass)
	}

	def removeListener(event, Closure closure) {
		listeners[parseEvent(event)]?.remove(closure)
	}

	def removeListenersFor(event){
		listeners.remove(parseEvent(event))
	}

	def removeAllListeners(){
		listeners = [:]
	}

	def dispatchEvent(type, T data){
		def x = listeners[parseEvent(type)]
		if (null != x) for (l in x){
			def a = (Closure) l.clone()
			try{
				if (a.parameterTypes.length > 1) a.call(data, l)
				else a.call(data)
			}catch (ex){
				listenerError(parseEvent(type), ex, l, data)
			}
		}
	}
}
