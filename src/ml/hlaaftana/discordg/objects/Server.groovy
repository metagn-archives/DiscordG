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
		if (this.getIconHash != null){
			return "https://discordapp.com/api/users/${this.getId()}/icons/${this.getIconHash()}.jpg"
		}else{
			return ""
		}
	}
	/**
	 * @return the URL of the icon of this server as a URL object.
	 */
	URL getIconURL() { return new URL(this.getIcon()) }

	/**
	 * @return the owner of this server as a Member object.
	 */
	Member getOwner() {
		for (m in this.getMembers()){
			if (m.getId().equals(object["owner_id"])){
				return m
			}
		}
		return null
	}

	/**
	 * @return the default text channel for this server.
	 */
	TextChannel getDefaultChannel(){
		return this.getTextChannels().find { it.id.equals(this.getId()) }
	}

	/**
	 * @return the {@literal @everyone} role for this server.
	 */
	Role getDefaultRole(){ return this.getRoles().find { it.id.equals(this.getId()) } }

	/**
	 * Edit this server. You can currently only edit the name with the API. I'll add changing the icon later.
	 * @param newName - the new name for the server.
	 * @return the edited server as a Server object.
	 */
	Server edit(String newName) {
		return new Server(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.getId()}", new HashMap().put("name", newName))))
	}

	/**
	 * Leaves the server. Deletes it if the connected client owns it.
	 */
	void leave() {
		api.requester.delete("https://discordapp.com/api/guilds/${this.getId()}")
	}

	/**
	 * Creates a new text channel in the server.
	 * @param name - the name of the channel.
	 * @return a TextChannel object of the created text channel.
	 */
	TextChannel createTextChannel(String name) {
		return new TextChannel(api, api.requester.post("https://discordapp.com/api/guilds/${this.getId()}/channels", ["name": name, "type": "text"]))
	}

	/**
	 * Creates a new voice channel in the server.
	 * @param name - the name of the channel.
	 * @return a VoiceChannel object of the created voice channel.
	 */
	VoiceChannel createVoiceChannel(String name) {
		return new VoiceChannel(api, api.requester.post("https://discordapp.com/api/guilds/${this.getId()}/channels", ["name": name, "type": "voice"]))
	}

	/**
	 * @return a List of TextChannels in the server.
	 */
	List<TextChannel> getTextChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<TextChannel> channels = new ArrayList<TextChannel>()
		for (o in array){
			if (o["type"].equals("text")) channels.add(new TextChannel(api, o))
		}
		return channels
	}

	/**
	 * @return a List of VoiceChannels in the server.
	 */
	List<VoiceChannel> getVoiceChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<VoiceChannel> channels = new ArrayList<VoiceChannel>()
		for (o in array){
			if (o["type"].equals("voice")) channels.add(new VoiceChannel(api, o))
		}
		return channels
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	TextChannel getTextChannelById(String id){
		for (tc in this.getTextChannels()){
			if (tc.getId().equals(id)) return tc
		}
		return null
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	VoiceChannel getVoiceChannelById(String id){
		for (vc in this.getVoiceChannels()){
			if (vc.getId().equals(id)) return vc
		}
		return null
	}

	/**
	 * @return all channels in the server.
	 */
	List<Channel> getChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<Channel> channels = new ArrayList<Channel>()
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
		List<Role> roles = new ArrayList<Role>()
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
		List<Member> members = new ArrayList<Member>()
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
		List rolesArray = new ArrayList()
		for (r in roles){
			rolesArray.add(r.getId())
		}
		api.requester.patch("https://discordapp.com/api/guilds/${this.getId()}/members/${member.getId()}", ["roles": rolesArray])
	}

	/**
	 * Adds roles to a member.
	 * @param member - the member as a Member object.
	 * @param roles - the roles to add.
	 */
	void addRoles(Member member, List<Role> roles) {
		this.editRoles(member, member.getRoles().addAll(roles))
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
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.getId()}/bans"))
		List<User> bans = new ArrayList<User>()
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
		api.requester.put("https://discordapp.com/api/guilds/${this.getId()}/bans/${user.getId()}?delete-message-days=${days}")
	}

	/**
	 * Unban a user.
	 * @param user - the User object.
	 */
	void unban(User user) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.getId()}/bans/${user.getId()}")
	}

	/**
	 * Create a new role.
	 * @param data - a map containing data for the role. This can be: <br>
	 * [color: 0xFFFF00, hoist: true, name: "Hey there young fellow", permissions: 0]
	 * @return the created role.
	 */
	Role createRole(Map<String, Object> data) {
		Map defaultData = [color: 0, hoist: false, name: "new role", permissions: 0]
		Role createdRole = new Role(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/guilds/${this.getId()}/roles", [:])))
		return editRole(createdRole, defaultData << data)
	}

	/**
	 * Edits a role.
	 * @param role - the Role object.
	 * @param data - a map containing data for the role. This can be: <br>
	 * [color: 0xFFFF00, hoist: true, name: "Hey there young fellow", permissions: 0]
	 * @return the edited role.
	 */
	Role editRole(Role role, Map<String, Object> data) {
		return new Role(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.getId()}/roles/${role.getId()}", data)))
	}

	/**
	 * Deletes a role.
	 * @param role - the Role object.
	 */
	void deleteRole(Role role) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.getId()}/roles/${role.getId()}")
	}
}
