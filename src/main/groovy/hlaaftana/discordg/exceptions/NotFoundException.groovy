package hlaaftana.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class NotFoundException extends HTTPException {
	NotFoundException(url, Map<String, Object> res){ super("$url with message: $res.message", url, res) }
}
