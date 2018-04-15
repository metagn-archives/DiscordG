package hlaaftana.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class BadRequestException extends HTTPException {
	BadRequestException(url, Map<String, Object> res) {
		super(res.collect { k, v -> "$k: $v" }.join('\n'), url, res)
	}
}
