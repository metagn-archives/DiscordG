package hlaaftana.discordg.exceptions

class BadRequestException extends Exception {
	BadRequestException(String url, message){ super("$url with message: $message") }
}
