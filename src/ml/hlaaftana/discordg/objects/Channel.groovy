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
	Server getServer(){ if (this."private") return null
		for (s in api.client.servers){
			if (s.id == object["guild_id"]) return s
		}
	}

	/**
	 * Deletes the channel.
	 */
	void delete() {
		api.requester.delete("https://discordapp.com/api/channels/${this.id}")
	}

	/**
	 * Edits the channel.
	 * @param data - the data to edit the channel with. Can be:
	 * [name: "generalobby", position: 1, topic: "Lobby for general talk."]
	 * @return the edited channel.
	 */
	Channel edit(Map<String, Object> data) {
		return new Channel(api, api.requester.patch("https://discordapp.com/api/channels/${this.id}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()]))
	}

	void editPermissions(Base target, def allow, def deny){
		String id = target.id
		String type = (target instanceof Role) "role" : "member"
		int allowBytes = (allow instanceof int) allow : allow.value
		int denyBytes = (deny instanceof int) deny : deny.value
		api.requester.put("https://discordapp.com/api/channels/${this.id}/permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(Base target, def allow, def deny){
		this.editPermissions(target, allow, deny)
	}
	
	void deletePermissions(Base target){
		api.requester.delete("https://discordapp.com/api/channels/${this.id}/permissions/${target.id}")
	}
}
