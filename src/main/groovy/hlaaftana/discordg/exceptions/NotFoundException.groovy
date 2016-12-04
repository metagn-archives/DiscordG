package hlaaftana.discordg.exceptions

class NotFoundException extends HTTPException {
	NotFoundException(url, res){ super("$url with message: $res.message", url, res) }
}
