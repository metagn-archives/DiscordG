package hlaaftana.discordg.exceptions

class HTTPException extends Exception {
	String url
	Map response
	HTTPException(message, u, res){ super(message); response = res; url = u }
}
