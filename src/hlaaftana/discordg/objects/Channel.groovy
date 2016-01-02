package hlaaftana.discordg.objects

import java.util.Map
import org.json.JSONObject

class Channel extends Base{
	JSONObject object
	Channel(JSONObject object){
		super(object)
		this.object = object
	}

	String getPosition(){ return object.getInt("position") }
	String getType(){ return object.getString("type") }
	Server getServer(){ return }

	void delete() {

	}

	Channel edit(Map<String, Object> data) {
		return null
	}
}
