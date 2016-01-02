package hlaaftana.discordg.events

import hlaaftana.discordg.util.JSONUtil

class Event {
	Map jsonForEvent
	String type
	Event (Map json, String type){ jsonForEvent = json; this.type = type }

	Map json(){
		return jsonForEvent
	}

	String getType(){
		return type
	}
}
