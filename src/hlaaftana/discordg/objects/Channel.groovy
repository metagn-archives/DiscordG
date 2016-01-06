package hlaaftana.discordg.objects

import java.util.Map
import hlaaftana.discordg.util.JSONUtil

class Channel extends Base{
	Channel(API api, Map object){
		super(api, object)
	}

	boolean isPrivate(){ return object["is_private"] }
	String getPosition(){ return object["position"] }
	String getType(){ return object["type"] }
	String getTopic(){ return object["topic"] }
	Server getServer(){ if (this.isPrivate()) return null
		for (s in api.client.getServers()){
			if (s.getId().equals(object["guild_id"])) return s
		}
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${this.getId()}")
	}

	Channel edit(Map<String, Object> data) {
		return new Channel(api, api.getRequester().patch("https://discordapp.com/api/channels/${this.getId()}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()]))
	}
}
