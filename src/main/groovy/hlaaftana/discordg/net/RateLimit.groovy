package hlaaftana.discordg.net

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.data.Snowflake

@InheritConstructors
@CompileStatic
class RateLimit extends DiscordObject {
	String message
	long retryTime
	boolean global
	List<Integer> requests = []
	
	int newRequest() {
		int x = requests.size() + 1
		requests.add(x)
		x
	}

	Snowflake getId() { null }
	String getName() { null }

	void jsonField(String name, Object value) {
		switch (name) {
		case 'global': global = (boolean) value; break
		case 'retry_after': retryTime = value as long; break
		case 'message': message = (String) value; break
		}
	}
}
