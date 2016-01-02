package hlaaftana.discordg.objects

import java.util.Map
import org.json.JSONObject

class Channel extends Base{
	Channel(API api, JSONObject object){
		super(api, object)
	}

	boolean isPrivate(){ return object.getBoolean("is_private") }
	String getPosition(){ return object.getInt("position") }
	String getType(){ return object.getString("type") }
	String getTopic(){ return object.getString("topic") }
	Server getServer(){ if (this.isPrivate()) return null
		for (s in api.client.getServers()){
			if (s.getID().equals(object.getString("guild_id"))) return s
		}
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${this.getID()}")
	}

	Channel edit(Map<String, Object> data) {
		return new Channel(api, api.getRequester().patch("https://discordapp.com/api/channels/${this.getID()}", new JSONObject().put("name", (data.containsKey("name")) ? data["name"].toString() : this.getName()).put("position", (data.containsKey("position")) ? data["position"] : this.getPosition()).put("topic", (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic())))
	}
}
