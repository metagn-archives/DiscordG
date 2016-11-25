package hlaaftana.discordg.exceptions

class NotFoundException extends Exception {
	NotFoundException(String url, message){ super("$url with message: $message") }
}
