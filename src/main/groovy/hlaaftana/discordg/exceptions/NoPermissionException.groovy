package hlaaftana.discordg.exceptions

class NoPermissionException extends Exception {
	NoPermissionException(message){ super(message) }
	NoPermissionException(url, message) { super("Insufficient permissions while connecting to $url, message: $message") }
}
