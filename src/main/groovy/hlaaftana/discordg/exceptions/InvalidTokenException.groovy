package hlaaftana.discordg.exceptions

class InvalidTokenException extends HTTPException {
	InvalidTokenException(url, res){
		super("Current token invalid. Try deleting the token cache file and restarting", url, res)
	}
}
