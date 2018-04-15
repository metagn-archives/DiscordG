package hlaaftana.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class HTTPException extends Exception {
	String url
	Map<String, Object> response
	HTTPException(message, u, Map<String, Object> res) { super(message.toString()); response = res; url = u.toString() }
}
