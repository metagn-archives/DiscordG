package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class ActionPool {
	long max
	long ms
	ExecutorService waitPool
	Map<String, AtomicInteger> actions = new HashMap<String, AtomicInteger>().asSynchronized()
	Closure<Boolean> suspend = { String it ->
		final t = actions[it]
		(null == t ? t : 0) >= max
	}

	ActionPool() {}

	ActionPool(long max, long ms) {
		setMax(max)
		this.ms = ms
	}

	static ActionPool create(long max, long ms) {
		new ActionPool(max, ms)
	}

	long setMax(long n) {
		this.@max = n
		waitPool = Executors.newFixedThreadPool(n as int)
		max
	}

	def <T> T ask(String bucket = '$', Closure<T> action) {
		while (suspend(bucket)) { Thread.sleep 10 }
		final t = actions[bucket]
		if (null == t) actions[bucket] = new AtomicInteger(1)
		else actions[bucket].getAndIncrement()
		def result = action()
		waitPool.submit {
			Thread.sleep(ms)
			actions[bucket].getAndDecrement()
		}
		result
	}

	void ask(String bucket = '$', Runnable action) {
		while (suspend(bucket)) { Thread.sleep 10 }
		final t = actions[bucket]
		if (null == t) actions[bucket] = new AtomicInteger(1)
		else actions[bucket].getAndIncrement()
		action.run()
		waitPool.submit {
			Thread.sleep(ms)
			actions[bucket].getAndDecrement()
		}
	}
}
