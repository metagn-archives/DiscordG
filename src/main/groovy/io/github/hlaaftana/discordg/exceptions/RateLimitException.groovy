package io.github.hlaaftana.discordg.exceptions

class RateLimitException extends Exception {
	RateLimitException(message){ super(message.toString()) }
}
