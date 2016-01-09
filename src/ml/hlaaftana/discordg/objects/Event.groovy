package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * An object for events containing the data and the type.
 * @author Hlaaftana
 */
class Event {
	Map data
	String type
	Event (Map data, String type){ this.data = data; this.type = type }
}
