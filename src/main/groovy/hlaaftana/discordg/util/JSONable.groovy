package hlaaftana.discordg.util

trait JSONable {
	// this is NOT supposed to return raw JSON, just a Groovy representation of an object
	// that will regularly be translated to JSON
	abstract json()
}
