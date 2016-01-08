package ml.hlaaftana.discordg.objects

import java.util.List
import java.util.Map

import ml.hlaaftana.discordg.util.JSONUtil

class Client{
	API api
	Client(API api){ this.api = api }

	User getUser(){ return new User(api, api.readyData["user"]) }

	String getSessionId(){ return api.readyData["session_id"] }

	TextChannel createTextChannel(Server server, String name) {
		return server.createTextChannel(name)
	}

	VoiceChannel createVoiceChannel(Server server, String name) {
		return server.createVoiceChannel(name)
	}

	Channel editChannel(Channel channel, Map<String, Object> data) {
		channel.edit(data)
	}

	void deleteChannel(Channel channel) {
		channel.delete()
	}

	Server createServer(String name) {
		return new Server(api, JSONUtil.parse(api.getRequester().post("https://discordapp.com/api/guilds", ["name": name])))
	}

	Server editServer(Server server, String newName) {
		return server.edit(newName)
	}

	void leaveServer(Server server) {
		server.delete()
	}

	Message sendMessage(TextChannel channel, String content, boolean tts=false) {
		return channel.sendMessage(content, tts)
	}

	Message editMessage(Message message, String newContent) {
		return message.edit(newContent)
	}

	void deleteMessage(Message message) {
		message.delete()
	}

	// removed ack method because of discord dev request

	List<Server> getServers() {
		List array = api.readyData["guilds"]
		List<Server> servers = new ArrayList<Server>()
		for (o in array){
			servers.add(new Server(api, o))
		}
		return servers
	}

	List<TextChannel> getTextChannelsForServer(Server server) {
		return server.getTextChannels()
	}

	List<VoiceChannel> getVoiceChannelsForServer(Server server) {
		return server.getVoiceChannels()
	}

	List<User> getAllUsers() {
		List<User> users = new ArrayList<User>()
		for (s in this.getServers()){
			for (m in s.getMembers()){
				boolean isThere
				for (u in users){
					try{
						if (!u.getId().equals(m.getUser().getId())){ isThere = isThere || false }
					}catch (ex){
						isThere = isThere || false
					}
					if (isThere){ break }
				}
				if (!isThere){ users.add(m.getUser()) }
			}
		}
	}

	List<Member> getAllMembers() {
		List<Member> members = new ArrayList<Member>()
		for (s in this.getServers()){
			for (m in s.getMembers()){
				members.add(m)
			}
		}
		return members
	}

	void editRoles(Member member, List<Role> roles) {
		member.getServer().editRoles(member, roles)
	}

	void addRoles(Member member, List<Role> roles) {
		member.getServer().addRoles(member, roles)
	}

	void kickMember(Member member) {
		member.kick()
	}

	void ban(Server server, User user, int days=0) {
		server.ban(user, days)
	}

	void unban(Server server, User user) {
		server.unban(user)
	}

	Role createRole(Server server, Map<String, Object> data) {
		return server.createRole(data)
	}

	Role editRole(Server server, Role role, Map<String, Object> data) {
		return server.editRole(role, data)
	}

	void deleteRole(Server server, Role role) {
		server.deleteRole(role)
	}

	void changeStatus(Map<String, Object> data) {
		api.getWebSocketClient().send(["op": 3, "d": ["game": ["name": data["game"]], "idle_since": (data.get("idle") != null) ? System.currentTimeMillis() : null]])
	}

	Map editProfile(Map<String, Object> data) {
		return null
	}

	Invite acceptInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/invite/${Invite.parseId(link)}", [:])))
		else
			return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/invite/${link}", [:])))
	}

	Invite getInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(api, JSONUtil.parse(api.requester.get("https://discordapp.com/api/invite/${Invite.parseId(link)}")))
		else
			return new Invite(api, JSONUtil.parse(api.requester.get("https://discordapp.com/api/invite/${link}")))
	}

	Invite createInvite(def dest, Map data=[:]){
		String id = (dest instanceof Channel) ? dest.id : (dest instanceof Server) ? dest.defaultChannel.id : dest
		return new Invite(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/channels/${id}/invites", data)))
	}

	User getUserById(String id){
		for (u in this.getAllUsers()){
			if (u.getId().equals(id)) return u
		}
		return null
	}

	Server getServerById(String id){
		for (s in this.getServers()){
			if (s.getId().equals(id)) return s
		}
		return null
	}

	List<PrivateChannel> getPrivateChannels(){
		List channels = api.readyData["private_channels"]
		List<PrivateChannel> pcs = new ArrayList<PrivateChannel>()
		for (pc in channels){
			pcs.add(new PrivateChannel(api, pc))
		}
		return pcs
	}

	TextChannel getTextChannelById(String id){
		for (s in this.getServers()){
			for (c in s.getTextChannels()){
				if (c.getId().equals(id)) return c
			}
		}
		for (pc in this.getPrivateChannels()){
			if (pc.getId().equals(id)) return pc
		}
	}
}
