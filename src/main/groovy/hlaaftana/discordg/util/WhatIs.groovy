package hlaaftana.discordg.util

import groovy.transform.CompileStatic

@CompileStatic
class WhatIs {
	boolean guessed
	def value
	def returnedValue

	WhatIs(gay){ value = gay }

	static whatis(value, @DelegatesTo(WhatIs) Closure tf){
		WhatIs g = new WhatIs(value)
		tf.delegate = g
		tf.resolveStrategy = Closure.DELEGATE_FIRST
		tf()
		g.returnedValue
	}

	def when(match, @DelegatesTo(WhatIs) Closure d){
		if (value in match){
			guessed = true
			d.delegate = this
			d.resolveStrategy = Closure.DELEGATE_FIRST
			returnedValue = d()
		}
	}

	def when(match, val){
		if (value in match){
			guessed = true
			returnedValue = val
		}
	}

	def when(Map a){
		a.each(this.&when)
	}

	def otherwise(Closure c){
		if (!guessed) {
			c.delegate = this
			c.resolveStrategy = Closure.DELEGATE_FIRST
			returnedValue = c()
		}
	}

	def otherwise(val){
		if (!guessed) returnedValue = val
	}
}
