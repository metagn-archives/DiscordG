package hlaaftana.discordg.logic

import groovy.transform.CompileStatic

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@CompileStatic
class ActionPool {
	long max
	long ms
	ExecutorService waitPool
	Map<String, Integer> actions = [:]
	Closure<Boolean> suspend = { String it -> (actions[it] ?: 0) >= max }

	static ActionPool create(long max, long ms){
		def ap = new ActionPool()
		ap.max = max
		ap.ms = ms
		ap
	}

	long setMax(long n){
		this.@max = n
		waitPool = Executors.newFixedThreadPool(n as int)
		max
	}

	def <T> T ask(String bucket = '$', Closure<T> action){
		while (suspend(bucket)) { Thread.sleep 10 }
		if (actions[bucket]) actions[bucket]++
		else actions[bucket] = 1
		def result = action()
		waitPool.submit {
			Thread.sleep(ms)
			actions[bucket] = actions[bucket] - 1
		}
		result
	}
}
