package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class RateLimit extends DiscordObject {
	String getId(){ bucket }
	String getBucket(){ object["bucket"] }
	String getMessage(){ object["message"] }
	long getRetryTime(){ object["retry_after"] }
}
