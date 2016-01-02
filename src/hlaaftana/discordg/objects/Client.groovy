package hlaaftana.discordg.objects

import java.util.List
import java.util.Map

import org.json.JSONArray
import org.json.JSONObject

class Client{
	API api
	Client(API api){ this.api = api }

	User getUser() {
		return new User(api, api.readyData.getJSONObject("user"))
	}

	String getSessionId(){
		return api.readyData.getString("session_id")
	}

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
		return new Server(api, new JSONObject(api.getRequester().post("https://discordapp.com/api/guilds", new JSONObject().put("name", name))))
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

	void acknowledgeMessage(Message message) {
		message.acknowledge()
	}

	List<Server> getServers() {
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/users/@me/guilds"))
		List<Server> servers = new ArrayList<Server>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				servers.add(new Server(api, array.get(i)))
			}catch (e){
				break
			}
		}
		return servers
	}

	List<TextChannel> getTextChannelsForServer(Server server) {
		return server.getTextChannels()
	}

	List<VoiceChannel> getVoiceChannelsForServer(Server server) {
		return server.getVoiceChannels()
	}

	Member getMemberFromServer(Server server, User user) {
		return server.getMemberForUser(user)
	}

	List<User> getAllUsers() {

	}

	List<Member> getAllMembers() {
		return null
	}

	void editRoles(Member member, List<Role> roles) {

	}

	void addRoles(Member member, List<Role> roles) {

	}

	void removeRoles(Member member, List<Role> roles) {

	}

	void kickMember(Member member) {

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
		api.getWebSocketClient().send(new JSONObject().put("op", 3).put("d", new JSONObject().put("game", new JSONObject().put("name", data.get("game"))).put("idle_since", (data.get("idle") != null) ? System.currentTimeMillis() : JSONObject.NULL)))
	}

	Map<String, Object> editProfile(Map<String, Object> data) {
		return null
	}
}
