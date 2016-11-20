package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class RateLimit extends DiscordObject {
	String getMessage(){ object["message"] }
	long getRetryTime(){ object["retry_after"] }
	boolean isGlobal(){ object["global"] }
}
