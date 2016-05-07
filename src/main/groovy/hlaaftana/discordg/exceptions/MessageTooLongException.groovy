package hlaaftana.discordg.exceptions

class MessageTooLongException extends Exception {
	MessageTooLongException(){ super("Message character length surpassed 2000 characters") }
}
