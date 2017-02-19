package hlaaftana.discordg.util

import java.util.regex.Pattern

class ClosureString implements CharSequence {
	Closure closure
	boolean regex

	ClosureString(Closure c){
		closure = c
	}

	ClosureString(Pattern pattern){
		closure = pattern.&toString
		regex = true
	}

	ClosureString(notClosure){
		closure = notClosure.&toString
	}

	ClosureString(ClosureString otherTrigger){
		closure = otherTrigger.closure
	}

	def plus(smh){
		"${closure()}$smh"
	}

	def plus(ClosureString trigger){
		this.class.newInstance({ "$this$trigger" })
	}

	boolean equals(other){
		is(other) || closure.is(other.closure) || toString() == other.toString()
	}

	String toString(){
		"${closure()}"
	}

	char charAt(int index){ toString() charAt index }
	int length(){ toString() length() }
	CharSequence subSequence(int start, int end){ toString() subSequence start, end }
}