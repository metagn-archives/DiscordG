package hlaaftana.discordg.exceptions

class NoPermissionException extends Exception {
	NoPermissionException(url) { super("Insufficient permissions while connecting to $url") }
}
