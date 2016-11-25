package hlaaftana.discordg.net

import hlaaftana.discordg.objects.DiscordObject;

@groovy.transform.InheritConstructors
class RateLimit extends DiscordObject {
	String getMessage(){ object["message"] }
	long getRetryTime(){ object["retry_after"] }
	boolean isGlobal(){ object["global"] }
}
