package hlaaftana.discordg.objects

import java.util.Map

import hlaaftana.discordg.util.JSONUtil

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
		return this.object["permission_overwrites"].list
	}

	Map<String, PermissionOverwrite> getPermissionOverwriteMap(){
		return this.object["permission_overwrites"].map
	}

	Permissions permissionsFor(User user){
		if (this.server == null) return Permissions.ALL_TRUE
		else return this.server.member(user).permissionsFor(this)
	}

	List<Invite> getInvites(){
		return JSONUtil.parse(client.requester.get("channels/${this.id}/invites")).collect { new Invite(client, it) }
	}

	/**
	 * @return a mention for the channel.
	 */
	String getMention() { return "<#${this.id}>" }

	/**
	 * Start typing in the channel.
	 */
	void startTyping() {
		client.requester.post("channels/${this.id}/typing", [:])
	}

	/**
	 * Deletes the channel.
	 */
	void delete() {
		client.requester.delete("channels/${this.id}")
	}

	/**
	 * Edits the channel.
	 * @param data - the data to edit the channel with. Can be:
	 * [name: "generalobby", position: 1, topic: "Lobby for general talk."]
	 * @return the edited channel.
	 */
	Channel edit(Map<String, Object> data) {
		return this.class.declaredConstructors[0].newInstance(client, JSONUtil.parse(client.requester.patch("channels/${this.id}", ["name": (data.containsKey("name")) ? data["name"].toString() : this.getName(), "position": (data.containsKey("position")) ? data["position"] : this.getPosition(), "topic": (data.containsKey("topic")) ? data["topic"].toString() : this.getTopic()])))
	}

	void editPermissions(DiscordObject target, def allow, def deny){
		String id = target.id
		String type = (target instanceof Role) ? "role" : "member"
		int allowBytes = (allow instanceof Integer) ? allow : allow.value
		int denyBytes = (deny instanceof Integer) ? deny : deny.value
		client.requester.put("channels/${this.id}/permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(DiscordObject target, def allow, def deny){
		this.editPermissions(target, allow, deny)
	}

	void deletePermissions(DiscordObject target){
		client.requester.delete("channels/${this.id}/permissions/${target.id}")
	}

	static Channel typed(Channel channel){
		return channel.type == "text" ? new TextChannel(channel.client, channel.object) : channel.type == "voice" ? new VoiceChannel(channel.client, channel.object) : channel
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
		boolean involves(DiscordObject involved){
			if (involved instanceof User){
				if (this.affected instanceof Role) return involved in this.affected.members
				else return involved == this.affected
			}else if (involed instanceof Role){
				return involved == this.affected
			}
		}
		boolean isRole(){ return this.type == "role" }
		boolean isMember(){ return this.type == "member" }
		boolean isUser(){ return this.type == "member" }
	}
}
