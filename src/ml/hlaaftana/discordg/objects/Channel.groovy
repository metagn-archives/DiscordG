package ml.hlaaftana.discordg.objects

import java.util.Map
import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord channel.
 * @author Hlaaftana
 */
class Channel extends Base{
	Channel(API api, Map object){
		super(api, object)
	}

	/**
	 * @return whether the channel is private or not.
	 */
	boolean isPrivate(){ return object["is_private"] }
	/**
	 * @return the position index of the channel. null if private.
	 */
	String getPosition(){ return object["position"] }
	/**
	 * @return the type of channel this channel is. can be "text" or "voice".
	 */
	String getType(){ return object["type"] }
	/**
	 * @return the topic of the channel. Can be null, but always null if voice.
	 */
	String getTopic(){ return object["topic"] }
	/**
	 * @return the server of the channel. null if private.
	 */
	Server getServer(){ if (this.isPrivate()) return null
		for (s in api.client.getServers()){
			if (s.getId().equals(object["guild_id"])) return s
		}
	}

	/**
	 * Deletes the channel.
	 */
	void delete() {
		api.getRequester().delete("https://discordapp.com/api/channels/${this.getId()}")
	}

	/**
	 * Edits the channel.
	 * @param data - the data to edit the channel with. Can be:
	 * [name: "generalobby", position: 1, topic: "Lobby for general talk."]
	 * @return the edited channel.
	 */
	Channel edit(Map<String, Object> data) {
		return new Channel(api, api.getRequester().patch("https://discordapp.com/api/channels/${this.getId()}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()]))
	}
}
