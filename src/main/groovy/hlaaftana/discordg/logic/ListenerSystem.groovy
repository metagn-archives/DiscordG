package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

@CompileStatic
abstract class ListenerSystem<T> {
	Map<Object, List<Closure>> listeners = [:]

	abstract parseEvent(param)

	abstract void listenerError(event, Throwable ex, Closure closure, T data)

	def <R> Closure<R> temporaryListener(event, Closure<R> closure) {
		return {
			final result = closure.call(it)
			listeners.get(event).remove(closure)
			result
		}
	}

	def <R> Closure<R> addListener(event, Closure<R> closure) {
		event = parseEvent(event)
		if (listeners.containsKey(event)) listeners[event].add(closure)
		else listeners[event] = [(Closure) closure]
		closure
	}
	
	def <R> Closure<R> listen(event, @DelegatesTo(T) Closure<R> closure) {
		final c = { T d, Closure internal ->
			Closure<R> copy = (Closure<R>) closure.clone()
			copy.delegate = d
			copy.parameterTypes.size() == 2 ? copy(copy.delegate, internal) : copy(copy.delegate)
		}
		c.resolveStrategy = Closure.DELEGATE_FIRST
		addListener(event, c)
	}

	boolean removeListener(event, Closure closure) {
		final L = listeners[parseEvent(event)]
		null == L ? false: L.remove(closure)
	}

	boolean removeListenersFor(event) {
		listeners.remove(parseEvent(event))
	}

	void removeAllListeners() {
		listeners = [:]
	}

	def dispatchEvent(type, T data) {
		def x = listeners[parseEvent(type)]
		if (null != x) for (l in x) {
			def a = (Closure) l.clone()
			try{
				if (a.parameterTypes.length > 1) a.call(data, l)
				else a.call(data)
			}catch (ex) {
				listenerError(parseEvent(type), ex, l, data)
			}
		}
	}
}
