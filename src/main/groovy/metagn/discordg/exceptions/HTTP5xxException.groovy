package metagn.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class HTTP5xxException extends HTTPException {
	HTTP5xxException(url, Map<String, Object> res) {
		super("$res.code when connecting to $url, message: $res.message", url, res)
	}
}
