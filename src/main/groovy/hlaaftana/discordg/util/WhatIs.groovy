package hlaaftana.discordg.util

class WhatIs {
	boolean guessed
	def value
	def returnedValue

	WhatIs(gay){ value = gay }

	static whatis(value, Closure tf){
		WhatIs g = new WhatIs(value)
		Closure wa = tf.clone()
		wa.delegate = g
		wa.resolveStrategy = Closure.DELEGATE_FIRST
		wa()
		g.returnedValue
	}

	def when(match, Closure d){
		if (match.isCase(value)){
			guessed = true
			Closure wt = d.clone()
			wt.delegate = this
			wt.resolveStrategy = Closure.DELEGATE_FIRST
			returnedValue = wt()
		}
	}

	def when(match, val){
		if (match.isCase(value)){
			guessed = true
			returnedValue = val
		}
	}

	def when(Map a){
		a.each(this.&when)
	}

	def otherwise(Closure c){
		if (!guessed){
			Closure a = c.clone()
			a.delegate = this
			a.resolveStrategy = Closure.DELEGATE_FIRST
			returnedValue = a()
		}
	}

	def otherwise(val){
		if (!guessed) returnedValue = val
	}
}
