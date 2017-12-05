package hlaaftana.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class MessageInvalidException extends Exception {
	MessageInvalidException(String message){
		super(message ? 'Message longer than 2000 characters' : 'Cannot send empty message')
	}
}
