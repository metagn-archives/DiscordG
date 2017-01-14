package hlaaftana.discordg.logic

import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

class ActionPool {
	int max
	long ms
	ExecutorService waitPool
	Map actions = [:]
	Closure suspend = { (actions[it] ?: 0) >= max }

	static "new"(int max, long ms){
		new ActionPool(max: max, ms: ms)
	}

	long setMax(long n){
		this.@max = n
		waitPool = Executors.newFixedThreadPool(n as int)
		max
	}

	def ask(String bucket = '$', Closure action){
		while (suspend(bucket));
		if (actions[bucket]) actions[bucket]++
		else actions[bucket] = 1
		def result = action()
		waitPool.submit {
			Thread.sleep(ms)
			actions[bucket]--
		}
		result
	}
}
