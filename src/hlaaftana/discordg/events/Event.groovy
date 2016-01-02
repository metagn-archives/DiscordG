package hlaaftana.discordg.events

import org.json.JSONObject

class Event {
	JSONObject jsonForEvent
	String type
	Event (JSONObject json, String type){ jsonForEvent = json; this.type = type }

	JSONObject json(){
		return jsonForEvent
	}

	String getType(){
		return type
	}
}
