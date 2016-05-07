package hlaaftana.discordg.exceptions

class HTTP5xxException extends Exception {
	HTTP5xxException(code, url){ super("$code when connecting to $url") }
}
