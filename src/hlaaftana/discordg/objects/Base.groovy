package hlaaftana.discordg.objects

import org.json.JSONObject

class Base {
	JSONObject object
	Base(JSONObject object){ this.object = object }
	String getID(){ return object.getString("id") }
	String getName(){ return object.getString("name") }
}
