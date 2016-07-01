package hlaaftana.discordg.util

class WhatIs {
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
		if (value.isCase(match) || value in match){
			Closure wt = d.clone()
			wt.delegate = this
			wt.resolveStrategy = Closure.DELEGATE_FIRST
			returnedValue = wt()
		}
	}

	def when(match, val){
		if (value.isCase(match) || value in match){
			returnedValue = val
		}
	}

	def when(Map a){
		a.each { k, v ->
			when(k, v)
		}
	}
}
