package hlaaftana.discordg.exceptions

class HTTP5xxException extends HTTPException {
	HTTP5xxException(url, res){
		super("$res.code when connecting to $url, message: $res.message", url, res)
	}
}
