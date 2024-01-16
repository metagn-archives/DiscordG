package metagn.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class InvalidTokenException extends HTTPException {
	InvalidTokenException(url, Map<String, Object> res) {
		super('Current token invalid. Try deleting the token cache file and restarting', url, res)
	}
}
