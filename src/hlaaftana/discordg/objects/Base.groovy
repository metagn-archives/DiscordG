package hlaaftana.discordg.objects

import org.json.JSONObject

class Base {
	API api
	JSONObject object
	Base(API api, JSONObject object){ this.object = object; this.api = api }
	String getID(){ return object.getString("id") }
	String getName(){ return object.getString("name") }
}
