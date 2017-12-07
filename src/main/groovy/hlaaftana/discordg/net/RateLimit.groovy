package hlaaftana.discordg.net

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.DiscordObject

@InheritConstructors
@CompileStatic
class RateLimit extends DiscordObject {
	List<Integer> requests = []
	
	int newRequest(){
		int x = requests.size() + 1
		requests.add(x)
		x
	}
	String getMessage(){ (String) object.message }
	long getRetryTime(){ (long) object.retry_after }
	boolean isGlobal(){ (boolean) object.global }
}
