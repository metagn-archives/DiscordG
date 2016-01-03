package hlaaftana.discordg.events

import hlaaftana.discordg.util.JSONUtil

class Event {
	Map data
	String type
	Event (Map data, String type){ this.data = data; this.type = type }

	Map data(){
		return data
	}

	String getType(){
		return type
	}
}
