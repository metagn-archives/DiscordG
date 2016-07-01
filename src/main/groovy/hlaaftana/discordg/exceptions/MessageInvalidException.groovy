package hlaaftana.discordg.exceptions

class MessageInvalidException extends Exception {
	MessageInvalidException(String message){
		super(message ? "Message longer than 2000 characters" : "Cannot send empty message")
	}
}
