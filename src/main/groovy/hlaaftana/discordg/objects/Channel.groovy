package hlaaftana.discordg.objects

import java.util.Map

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.JSONUtil

/**
 * A Discord channel.
 * @author Hlaaftana
 */
class Channel extends DiscordObject {
	Channel(Client client, Map object){
		super(client, object, "channels/${object["id"]}")
	}

	boolean isPrivate(){ object["is_private"] as boolean }
	int getPosition(){ object["position"] }
	String getType(){ object["type"] }
	boolean isText(){ type == "text" }
	boolean isVoice(){ type == "voice" }
	String getTopic(){ object["topic"] }
	Server getServer(){
		this.private ? null : client.serverMap[object["guild_id"]]
	}
	Server getParent(){ server }

	List<PermissionOverwrite> getPermissionOverwrites(){
		object["permission_overwrites"].list
	}

	Map<String, PermissionOverwrite> getPermissionOverwriteMap(){
		object["permission_overwrites"].map
	}

	PermissionOverwrite permissionOverwrite(ass){ find(permissionOverwrites, ass) }

	Permissions permissionsFor(user){
		if (server == null) return Permissions.PRIVATE_CHANNEL
		else server.member(user).permissionsFor(this)
	}

	Permissions fullPermissionsFor(user){
		if (server == null) return Permissions.PRIVATE_CHANNEL
		else server.member(user).fullPermissionsFor(this)
	}

	List<Invite> getInvites(){
		requester.jsonGet("invites").collect { new Invite(client, it) }
	}

	String getMention() { "<#${id}>" }

	void startTyping() {
		requester.post("typing", [:])
	}

	void delete() {
		requester.delete("")
	}

	Channel edit(Map<String, Object> data) {
		return Channel.typed(new Channel(client, requester.jsonPatch("", [
			name: data.containsKey("name") ? data["name"].toString() : name,
			position: data["position"] != null ? data["position"] : position,
			topic: data.containsKey("topic") ? data["topic"].toString() : topic])))
	}

	void editPermissions(target, allow, deny){
		String id = id(target)
		String type = get(target, Role) ? "role" : "member"
		int allowBytes = allow.toInteger()
		int denyBytes = deny.toInteger()
		requester.put("permissions/${id}", [allow: allowBytes, deny: denyBytes, id: id, type: type])
	}

	void addPermissions(target, allow, deny){
		editPermissions(target, allow, deny)
	}

	void deletePermissions(target){
		requester.delete("permissions/${id(target)}")
	}

	static Map construct(Client client, Map c, String serverId = null){
		if (serverId) c["guild_id"] = serverId
		if (c["guild_id"]){
			def po = c["permission_overwrites"]
			if (po instanceof List) c["permission_overwrites"] = new DiscordListCache(po.collect { it << [channel_id: c["id"]] }, client, Channel.PermissionOverwrite)
		}
		c
	}

	static Channel typed(Channel channel){
		channel.type == "text" ? new TextChannel(channel.client, channel.object) : channel.type == "voice" ? new VoiceChannel(channel.client, channel.object) : channel
	}

	static class PermissionOverwrite extends DiscordObject {
		PermissionOverwrite(Client client, Map object){ super(client, object) }

		Permissions getAllowed(){ new Permissions(object["allow"]) }
		Permissions getDenied(){ new Permissions(object["deny"]) }
		String getType(){ object["type"] }
		DiscordObject getAffected(){
			if (type == "role"){
				List<Role> roles = []
				client.servers.each { roles.addAll(it.roles) }
				roles.find { it.id == id }
			}else if (type == "member"){
				List<Member> members = []
				client.servers.each { members.addAll(it.members) }
				members.find { it.id == id }
			}
			(DiscordObject) this
		}
		Channel getChannel(){ client.channelMap[object["channel_id"]] }
		Channel getParent(){ channel }
		void edit(allow, deny){ channel.editPermissions(affected, allow, deny) }
		void delete(){ channel.deletePermissions(affected) }
		String getName(){ affected.name }
		boolean involves(DiscordObject involved){
			if (involved instanceof User){
				if (affected instanceof Role) return involved in affected.members
				else involved == affected
			}else if (involed instanceof Role){
				involved == affected
			}
		}
		boolean isRole(){ type == "role" }
		boolean isMember(){ type == "member" }
		boolean isUser(){ type == "member" }
	}
}
