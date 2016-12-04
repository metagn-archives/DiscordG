package hlaaftana.discordg.exceptions

class BadRequestException extends HTTPException {
	BadRequestException(url, res){
		super("$url with message: $res.message", url, res)
	}
}
