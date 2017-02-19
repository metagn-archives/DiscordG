package hlaaftana.discordg.net

import hlaaftana.discordg.objects.DiscordObject;

@groovy.transform.InheritConstructors
class RateLimit extends DiscordObject {
	List requests = []
	
	int newRequest(){ (requests.size() + 1).with { requests.add(it); it } }
	String getMessage(){ object["message"] }
	long getRetryTime(){ object["retry_after"] }
	boolean isGlobal(){ object["global"] }
}
