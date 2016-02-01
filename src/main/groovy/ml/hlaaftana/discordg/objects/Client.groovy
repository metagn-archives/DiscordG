package ml.hlaaftana.discordg.objects

import java.util.List
import java.util.Map

import ml.hlaaftana.discordg.util.JSONUtil

/**
 * The Discord client.
 * @author Hlaaftana
 */
class Client{
	API api
	Client(API api){ this.api = api }

	/**
	 * @return the user which the client is logged in to.
	 */
	User getUser(){ return new User(api, api.readyData["user"]) }

	/**
	 * @return the session ID for the session.
	 */
	String getSessionId(){ return api.readyData["session_id"] }

	/**
	 * See Server#createTextChannel.
	 */
	TextChannel createTextChannel(Server server, String name) {
		return server.createTextChannel(name)
	}

	/**
	 * See Server#createVoiceChannel.
	 */
	VoiceChannel createVoiceChannel(Server server, String name) {
		return server.createVoiceChannel(name)
	}

	/**
	 * See Channel#edit.
	 */
	Channel editChannel(Channel channel, Map<String, Object> data) {
		channel.edit(data)
	}

	/**
	 * See Channel#delete.
	 */
	void deleteChannel(Channel channel) {
		channel.delete()
	}

	/**
	 * Creates a new server.
	 * @param name - the name of the server.
	 * @return the created server.
	 */
	Server createServer(String name) {
		return new Server(api, JSONUtil.parse(api.getRequester().post("https://discordapp.com/api/guilds", ["name": name])))
	}

	/**
	 * See Server#edit.
	 */
	Server editServer(Server server, Map data) {
		return server.edit(data)
	}

	/**
	 * See Server#leave.
	 */
	void leaveServer(Server server) {
		server.leave()
	}

	/**
	 * See TextChannel#sendMessage.
	 */
	Message sendMessage(TextChannel channel, String content, boolean tts=false) {
		return channel.sendMessage(content, tts)
	}

	/**
	 * See Message#edit.
	 */
	Message editMessage(Message message, String newContent) {
		return message.edit(newContent)
	}

	/**
	 * See Message#delete.
	 */
	void deleteMessage(Message message) {
		message.delete()
	}

	// removed ack method because of discord dev request

	/**
	 * @return a List of Servers the client is connected to.
	 */
	List<Server> getServers() {
		List array = api.readyData["guilds"]
		List<Server> servers = []
		for (o in array){
			servers.add(new Server(api, o))
		}
		return servers
	}

	List<Server> requestServers(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/users/@me/guilds"))
		List<Server> servers = []
		for (s in array){
			def serverInReady = api.readyData["guilds"].find { it["id"] == s["id"] }
			servers.add(new Server(api, s << ["channels": serverInReady["channels"], "members": serverInReady["members"], "presences": serverInReady["presences"], "voice_states": serverInReady["voice_states"], "large": serverInReady["large"]]))
		}
		return servers
	}

	/**
	 * See Server#getTextChannels.
	 */
	List<TextChannel> getTextChannelsForServer(Server server) {
		return server.textChannels
	}

	/**
	 * See Server#getVoiceChannels.
	 */
	List<VoiceChannel> getVoiceChannelsForServer(Server server) {
		return server.voiceChannels
	}

	/**
	 * @return a List of Users the client can see.
	 */
	List<User> getAllUsers() {
		List users = []
		for (s in this.servers){
			for (m in s.members){
				try{
					if (!(m.user.id in users*.id)){
						users.add(m.user)
					}
				}catch (ex){}
			}
		}
		return users
	}

	/**
	 * @return a List of Members the client can see. Same users can be different member objects.
	 */
	List<Member> getAllMembers() {
		List<Member> members = new ArrayList<Member>()
		for (s in this.servers){
			for (m in s.members){
				members.add(m)
			}
		}
		return members
	}

	List<User> getUsers(){ return this.allUsers }
	List<Member> getMembers(){ return this.allMembers }

	/**
	 * See Member#editRoles.
	 */
	void editRoles(Member member, List<Role> roles) {
		member.server.editRoles(member, roles)
	}

	/**
	 * See Member#delete.
	 */
	void addRoles(Member member, List<Role> roles) {
		member.server.addRoles(member, roles)
	}

	/**
	 * See Member#kick.
	 */
	void kickMember(Member member) {
		member.kick()
	}

	/**
	 * See Server#ban.
	 */
	void ban(Server server, User user, int days=0) {
		server.ban(user, days)
	}

	/**
	 * See Server#unban.
	 */
	void unban(Server server, User user) {
		server.unban(user)
	}

	/**
	 * See Server#createRole.
	 */
	Role createRole(Server server, Map<String, Object> data) {
		return server.createRole(data)
	}

	/**
	 * See Server#editRole.
	 */
	Role editRole(Server server, Role role, Map<String, Object> data) {
		return server.editRole(role, data)
	}

	/**
	 * See Server#deleteRole.
	 */
	void deleteRole(Server server, Role role) {
		server.deleteRole(role)
	}

	/**
	 * Updates the client's presence.
	 * @param data - The data to send. Can be: <br>
	 * [game: "FL Studio 12", idle: "anything. as long as it's defined in the map, the user will become idle"]
	 */
	void changeStatus(Map<String, Object> data) {
		api.wsClient.send(["op": 3, "d": ["game": ["name": data["game"]], "idle_since": (data["idle"] != null) ? System.currentTimeMillis() : null]])
		for (s in this.servers){
			api.dispatchEvent(new Event("PRESENCE_UPDATE", [
				"fullData": [
					"game": (data["game"] != null) ? ["name": data["game"]] : null,
					"status": (data["idle"] != null) ? "online" : "idle",
					"guild_id": s.id, "user": this.user.object
					],
				"server": s,
				"guild": s,
				"member": s.members.find { try{ it.id == this.user.id }catch (ex){ false } },
				"game": (data["game"] != null) ? data["game"] : null,
				"status": (data["idle"] != null) ? "online" : "idle"
				]))
		}
	}

	/**
	 * Accepts an invite and joins a new server.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return a new Invite object of the accepted invite.
	 */
	Invite acceptInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/invite/${Invite.parseId(link)}", [:])))
		else
			return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/invite/${link}", [:])))
	}

	/**
	 * Gets an Invite object from a link/ID.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return the gotten invite.
	 */
	Invite getInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(api, JSONUtil.parse(api.requester.get("https://discordapp.com/api/invite/${Invite.parseId(link)}")))
		else
			return new Invite(api, JSONUtil.parse(api.requester.get("https://discordapp.com/api/invite/${link}")))
	}

	/**
	 * Creates an invite.
	 * @param dest - The destination for the invite. Can be a Server, a Channel, or the ID of a channel.
	 * @return the created invite.
	 */
	Invite createInvite(def dest, Map data=[:]){
		String id = (dest instanceof Channel) ? dest.id : (dest instanceof Server) ? dest.defaultChannel.id : dest
		return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/channels/${id}/invites", data)))
	}

	void moveToChannel(Member member, VoiceChannel channel){
		api.requester.patch("https://discordapp.com/api/guilds/${member.server.id}/members/{member.id}", ["channel_id": channel.id])
	}

	/**
	 * Edits the user's profile.
	 * @param data - the data to edit with. Be really careful with this. Can be: <br>
	 * [email: "newemail@dock.org", new_password: "oopsaccidentallygavesomeonemypass", username: "New name new me"] <br>
	 * Note that you can also have an avatar property in the map above, but I'm not encouraging it until I provide a utility function for that.
	 * @return a User object for the edited profile.
	 */
	User editProfile(Map data){
		Map map = ["avatar": this.user.avatarHash, "email": api.email, "password": api.password, "username": this.user.username]
		Map response = JSONUtil.parse api.requester.patch("https://discordapp.com/api/users/@me", map << data)
		api.email = response.email
		api.password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : api.password
		api.token = response.token
		api.readyData["user"]["email"] = response.email
		api.readyData["user"]["verified"] = response.verified
		return new User(api, response)
	}

	/**
	 * Gets a user by its ID.
	 * @param id - the ID.
	 * @return the user. null if not found.
	 */
	User getUserById(String id){
		for (u in this.getAllUsers()){
			if ({ try{ u.id == id }catch (ex){ false } }() as boolean) return u
		}
		return null
	}

	/**
	 * Gets a server by its ID.
	 * @param id - the ID.
	 * @return the server. null if not found.
	 */
	Server getServerById(String id){
		this.servers.find { it.id == id }
	}

	/**
	 * @return all private channels.
	 */
	List<PrivateChannel> getPrivateChannels(){
		List channels = api.readyData["private_channels"]
		List<PrivateChannel> pcs = new ArrayList<PrivateChannel>()
		for (pc in channels){
			pcs.add(new PrivateChannel(api, pc))
		}
		return pcs
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID.
	 * @return the text channel. null if not found.
	 */
	TextChannel getTextChannelById(String id){
		for (s in this.servers){
			for (c in s.textChannels){
				if (c.id == id) return c
			}
		}
		for (pc in this.privateChannels){
			if (pc.id == id) return pc
		}
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID.
	 * @return the voice channel. null if not found.
	 */
	VoiceChannel getVoiceChannelById(String id){
		for (s in this.servers){
			for (c in s.voiceChannels){
				if (c.id == id) return c
			}
		}
	}
}
