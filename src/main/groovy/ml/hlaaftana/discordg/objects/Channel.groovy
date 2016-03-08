package ml.hlaaftana.discordg.objects

import java.util.Map

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord channel.
 * @author Hlaaftana
 */
class Channel extends DiscordObject{
	Channel(API api, Map object){
		super(api, object)
	}

	/**
	 * @return whether the channel is private or not.
	 */
	boolean isPrivate(){ return this.object["is_private"] }
	/**
	 * @return the position index of the channel. null if private.
	 */
	String getPosition(){ return this.object["position"] }
	/**
	 * @return the type of channel this channel is. can be "text" or "voice".
	 */
	String getType(){ return this.object["type"] }
	/**
	 * @return the topic of the channel. Can be null, but always null if voice.
	 */
	String getTopic(){ return this.object["topic"] }
	/**
	 * @return the server of the channel. null if private.
	 */
	Server getServer(){ if (this."private") return null
		return api.client.servers.find { it.id == this.object["guild_id"] }
	}

	List<PermissionOverwrite> getPermissionOverwrites(){
		return this.object["permission_overwrites"].collect { new PermissionOverwrite(api, it) }
	}

	List<Invite> getInvites(){
		return JSONUtil.parse(api.requester.get("https://discordapi.com/api/channels/${this.id}/invites")).collect { new Invite(api, it) }
	}

	/**
	 * @return a mention for the channel.
	 */
	String getMention() { return "<#${this.id}>" }

	/**
	 * Start typing in the channel.
	 */
	void startTyping() {
		api.requester.post("https://discordapp.com/api/channels/${this.id}/typing", [:])
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
		return this.class.declaredConstructors[0].newInstance(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/channels/${this.id}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()])))
	}

	void editPermissions(DiscordObject target, def allow, def deny){
		String id = target.id
		String type = (target instanceof Role) "role" : "member"
		int allowBytes = (allow instanceof int) allow : allow.value
		int denyBytes = (deny instanceof int) deny : deny.value
		api.requester.put("https://discordapp.com/api/channels/${this.id}/permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(DiscordObject target, def allow, def deny){
		this.editPermissions(target, allow, deny)
	}

	void deletePermissions(DiscordObject target){
		api.requester.delete("https://discordapp.com/api/channels/${this.id}/permissions/${target.id}")
	}

	static class PermissionOverwrite extends DiscordObject {
		PermissionOverwrite(API api, Map object){ super(api, object) }

		Permissions getAllowed(){ return new Permissions(this.object["allow"]) }
		Permissions getDenied(){ return new Permissions(this.object["deny"]) }
		String getType(){ return this.object["type"] }
		DiscordObject getAffected(){
			if (this.type == "role"){
				List<Role> roles = []
				api.client.servers.each { roles.addAll(it.roles) }
				return roles.find { it.id == this.id }
			}else if (this.type == "member"){
				List<Member> members = []
				api.client.servers.each { members.addAll(it.members) }
				return members.find { it.id == this.id }
			}
			return (DiscordObject) this
		}
		String getName(){ return this.affected.name }
	}
}
