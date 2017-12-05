package hlaaftana.discordg.exceptions

import groovy.transform.CompileStatic

@CompileStatic
class NoPermissionException extends HTTPException {
	NoPermissionException(url, Map<String, Object> res){
		super("Insufficient permissions while connecting to $url, message: $res.message", url, res)
	}
}
