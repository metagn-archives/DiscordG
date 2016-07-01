package hlaaftana.discordg.util

// i dont like this its not super thread safe
class Modifier {
	static Map<Thread, Map> modifications = [:]

	static modify(self, Map data){
		modify(self, data, Thread.currentThread())
	}

	static modify(self, Map data, Thread thread){
		if (!modifications[thread])
			modifications[thread] = [:]
		modifications[thread][self] = data
	}

	static getModifications(self){
		getModifications(self, Thread.currentThread())
	}

	static getModifications(self, Thread thread){
		if (modifications[thread])
			if (modifications[thread][self])
				modifications[thread][self]
	}

	static getAllModifications(Thread self){
		modifications[self]
	}

	static clearModifications(self){
		clearModifications(self, Thread.currentThread())
	}

	static clearModifications(self, Thread thread){
		if (modifications[thread])
			if (modifications[thread][self])
				modifications[thread].remove(self)
		if (modifications[thread] != null
			&& modifications[thread].empty)
			modifications.remove(thread)
	}

	static clearAllModifications(Thread self){
		modifications.remove(self)
	}
}
