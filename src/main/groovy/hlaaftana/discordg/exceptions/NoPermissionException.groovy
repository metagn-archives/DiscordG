package hlaaftana.discordg.exceptions

class NoPermissionException extends HTTPException {
	NoPermissionException(url, res){
		super("Insufficient permissions while connecting to $url, message: $res.message", url, res)
	}
}
