package ml.hlaaftana.discordg.objects

import java.net.URL;
import java.util.List
import java.util.Map

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * A Discord server/guild.
 * @author Hlaaftana
 */
class Server extends Base {
	Server(API api, Map object){
		super(api, object)
	}

	/**
	 * @return the region the server is in.
	 */
	String getRegion(){ return object["region"] }
	/**
	 * @return the timestamp of when this server was created.
	 */
	String getCreatedTimestamp(){ return object["joined_at"] }
	/**
	 * @return the hash/ID of this server's icon.
	 */
	String getIconHash(){ return object["icon"] }
	/**
	 * @return the URL of the icon of this server as a string.
	 */
	String getIcon() {
		if (this.iconHash != null){
			return "https://discordapp.com/api/users/${this.id}/icons/${this.iconHash}.jpg"
		}else{
			return ""
		}
	}

	/**
	 * @return the owner of this server as a Member object.
	 */
	Member getOwner() {
		for (m in this.members){
			if (m.id == object["owner_id"]){
				return m
			}
		}
		return null
	}

	/**
	 * @return the default text channel for this server.
	 */
	TextChannel getDefaultChannel(){ return this.textChannels.find { it.id == this.id } }
	VoiceChannel getAfkChannel(){ return this.voiceChannels.find { it.id == this.object["afk_channel_id"] } }
	int getAfkTimeout(){ return this.object["afk_timeout"] }
	Channel getWidgetChannel(){ return this.channels.find { it.id == this.object["embed_channel_id"] } }
	boolean isWidgetEnabled(){ return this.object["embed_enabled"] }

	/**
	 * @return the {@literal @everyone} role for this server.
	 */
	Role getDefaultRole(){ return this.roles.find { it.id == this.id } }

	/**
	 * Edit this server. You can currently only edit the name with the API. I'll add changing the icon later.
	 * @param newName - the new name for the server.
	 * @return the edited server as a Server object.
	 */
	Server edit(String newName) {
		return new Server(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.id}", ["name", newName])))
	}

	/**
	 * Leaves the server. Deletes it if the connected client owns it.
	 */
	void leave() {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}")
	}

	/**
	 * Creates a new text channel in the server.
	 * @param name - the name of the channel.
	 * @return a TextChannel object of the created text channel.
	 */
	TextChannel createTextChannel(String name) {
		return new TextChannel(api, api.requester.post("https://discordapp.com/api/guilds/${this.id}/channels", ["name": name, "type": "text"]))
	}

	/**
	 * Creates a new voice channel in the server.
	 * @param name - the name of the channel.
	 * @return a VoiceChannel object of the created voice channel.
	 */
	VoiceChannel createVoiceChannel(String name) {
		return new VoiceChannel(api, api.requester.post("https://discordapp.com/api/guilds/${this.id}/channels", ["name": name, "type": "voice"]))
	}

	/**
	 * @return a List of TextChannels in the server.
	 */
	List<TextChannel> getTextChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/channels"))
		List<TextChannel> channels = []
		for (o in array){
			if (o["type"] == "text") channels.add(new TextChannel(api, o))
		}
		return channels
	}

	/**
	 * @return a List of VoiceChannels in the server.
	 */
	List<VoiceChannel> getVoiceChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/channels"))
		List<VoiceChannel> channels = []
		for (o in array){
			if (o["type"] == "voice") channels.add(new VoiceChannel(api, o))
		}
		return channels
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	TextChannel getTextChannelById(String id){
		for (tc in this.textChannels){
			if (tc.id == id) return tc
		}
		return null
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	VoiceChannel getVoiceChannelById(String id){
		for (vc in this.voiceChannels){
			if (vc.id == id) return vc
		}
		return null
	}

	/**
	 * @return all channels in the server.
	 */
	List<Channel> getChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/channels"))
		List<Channel> channels = []
		for (o in array){
			channels.add(new Channel(api, o))
		}
		return channels
	}

	/**
	 * @return all roles in the server.
	 */
	List<Role> getRoles() {
		List array = object["roles"].collect { it }
		List<Role> roles = []
		for (o in array){
			roles.add(new Role(api, o))
		}
		return roles
	}

	/**
	 * @return all members in the server.
	 */
	List<Member> getMembers() {
		List array = object["members"].collect { it }
		List<Member> members = []
		for (o in array){
			members.add(new Member(api, o))
		}
		return members
	}

	/**
	 * Edits the roles for a member.
	 * @param member - the member as a Member object.
	 * @param roles - the roles the member will be overriden with.
	 */
	void editRoles(Member member, List<Role> roles) {
		List rolesArray = []
		for (r in roles){
			rolesArray.add(r.id)
		}
		api.requester.patch("https://discordapp.com/api/guilds/${this.id}/members/${member.id}", ["roles": rolesArray])
	}

	/**
	 * Adds roles to a member.
	 * @param member - the member as a Member object.
	 * @param roles - the roles to add.
	 */
	void addRoles(Member member, List<Role> roles) {
		this.editRoles(member, member.roles.with { addAll(roles) })
	}

	/**
	 * Kicks a member.
	 * @param member - the Member object to kick.
	 */
	void kickMember(Member member) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}/members/$member.id")
	}

	/**
	 * @return a List of Users who are banned.
	 */
	List<User> getBans() {
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/bans"))
		List<User> bans = []
		for (o in array){
			bans.add(new User(api, o["user"]))
		}
		return bans
	}

	/**
	 * Ban a user from the server.
	 * @param user - the User to ban.
	 * @param days - the amount of days to delete the user's messages until. I explained that badly but you get the deal if you ever banned someone on Discord.
	 */
	void ban(User user, int days=0) {
		api.requester.put("https://discordapp.com/api/guilds/${this.id}/bans/${user.id}?delete-message-days=${days}", [:])
	}

	/**
	 * Unban a user.
	 * @param user - the User object.
	 */
	void unban(User user) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}/bans/${user.id}")
	}

	/**
	 * Create a new role.
	 * @param data - a map containing data for the role. This can be: <br>
	 * [color: 0xFFFF00, hoist: true, name: "Hey there young fellow", permissions: 0]
	 * @return the created role.
	 */
	Role createRole(Map<String, Object> data) {
		Map defaultData = [color: 0, hoist: false, name: "new role", permissions: 0]
		Role createdRole = new Role(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/guilds/${this.id}/roles", [:])))
		return editRole(createdRole, defaultData << data)
	}

	/**
	 * Edits a role.
	 * @param role - the Role object.
	 * @param data - a map containing data for the role. This can be: <br>
	 * [color: 0xFFFF00, hoist: true, name: "Hey there young fellow", permissions: 0] <br>
	 * permissions can be a Permissions object and color can be a Colors object.
	 * @return the edited role.
	 */
	Role editRole(Role role, Map<String, Object> data) {
		if (data["color"] instanceof Colors) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions)
		return new Role(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.id}/roles/${role.id}", data)))
	}

	/**
	 * Deletes a role.
	 * @param role - the Role object.
	 */
	void deleteRole(Role role) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}/roles/${role.id}")
	}
}
