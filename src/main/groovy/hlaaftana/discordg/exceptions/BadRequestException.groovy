package hlaaftana.discordg.exceptions

class BadRequestException extends Exception {
	BadRequestException(String url){ super("$url report to hlaaf") }
}
