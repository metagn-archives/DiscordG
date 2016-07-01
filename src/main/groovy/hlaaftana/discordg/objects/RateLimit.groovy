package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;

@groovy.transform.InheritConstructors
class RateLimit extends APIMapObject {
	String getBucket(){ object["bucket"] }
	String getMessage(){ object["message"] }
	long getRetryTime(){ object["retry_after"] }
}
