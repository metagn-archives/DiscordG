package io.github.hlaaftana.discordg.objects

import java.util.Map

import io.github.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord channel.
 * @author Hlaaftana
 */
class Channel extends DiscordObject{
	Channel(Client client, Map object){
		super(client, object)
	}

	/**
	 * @return whether the channel is private or not.
	 */
	boolean isPrivate(){ return this.object["is_private"] as boolean }
	/**
	 * @return the position index of the channel. null if private.
	 */
	int getPosition(){ return this.object["position"] }
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
		return client.servers.find { it.id == this.object["guild_id"] }
	}

	List<PermissionOverwrite> getPermissionOverwrites(){
		return this.object["permission_overwrites"].collect { new PermissionOverwrite(client, it) }
	}

	List<Invite> getInvites(){
		return JSONUtil.parse(client.requester.get("https://discordapp.com/api/channels/${this.id}/invites")).collect { new Invite(client, it) }
	}

	/**
	 * @return a mention for the channel.
	 */
	String getMention() { return "<#${this.id}>" }

	/**
	 * Start typing in the channel.
	 */
	void startTyping() {
		client.requester.post("https://discordapp.com/api/channels/${this.id}/typing", [:])
	}

	/**
	 * Deletes the channel.
	 */
	void delete() {
		client.requester.delete("https://discordapp.com/api/channels/${this.id}")
	}

	/**
	 * Edits the channel.
	 * @param data - the data to edit the channel with. Can be:
	 * [name: "generalobby", position: 1, topic: "Lobby for general talk."]
	 * @return the edited channel.
	 */
	Channel edit(Map<String, Object> data) {
		return this.class.declaredConstructors[0].newInstance(client, JSONUtil.parse(client.requester.patch("https://discordapp.com/api/channels/${this.id}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()])))
	}

	void editPermissions(DiscordObject target, def allow, def deny){
		String id = target.id
		String type = (target instanceof Role) ? "role" : "member"
		int allowBytes = (allow instanceof int) ? allow : allow.value
		int denyBytes = (deny instanceof int) ? deny : deny.value
		client.requester.put("https://discordapp.com/api/channels/${this.id}/permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(DiscordObject target, def allow, def deny){
		this.editPermissions(target, allow, deny)
	}

	void deletePermissions(DiscordObject target){
		client.requester.delete("https://discordapp.com/api/channels/${this.id}/permissions/${target.id}")
	}

	static class PermissionOverwrite extends DiscordObject {
		PermissionOverwrite(Client client, Map object){ super(client, object) }

		Permissions getAllowed(){ return new Permissions(this.object["allow"]) }
		Permissions getDenied(){ return new Permissions(this.object["deny"]) }
		String getType(){ return this.object["type"] }
		DiscordObject getAffected(){
			if (this.type == "role"){
				List<Role> roles = []
				client.servers.each { roles.addAll(it.roles) }
				return roles.find { it.id == this.id }
			}else if (this.type == "member"){
				List<Member> members = []
				client.servers.each { members.addAll(it.members) }
				return members.find { it.id == this.id }
			}
			return (DiscordObject) this
		}
		String getName(){ return this.affected.name }
	}
}
