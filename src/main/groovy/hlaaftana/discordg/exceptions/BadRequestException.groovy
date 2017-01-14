package hlaaftana.discordg.exceptions

class BadRequestException extends HTTPException {
	BadRequestException(url, res){
		super(res.collect { k, v -> "$k: $v" }.join('\n'), url, res)
	}
}
