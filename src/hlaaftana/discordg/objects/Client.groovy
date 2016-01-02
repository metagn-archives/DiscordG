package hlaaftana.discordg.objects

import java.util.List
import java.util.Map

import org.json.JSONArray
import org.json.JSONObject

class Client{
	API api
	Client(API api){ this.api = api }

	User getUser() {
		return
	}

	TextChannel createTextChannel(Server server, String name) {
		return null
	}

	VoiceChannel createVoiceChannel(Server server, String name) {
		return null
	}

	Channel editChannel(Channel channel, Map<String, Object> data) {
		return null
	}

	void deleteChannel(Channel channel) {

	}

	Server createServer(String name) {
		return null
	}

	Server editServer(Server server, String newName) {
		return null
	}

	void deleteServer(Server server) {

	}

	Message sendMessage(TextChannel channel, String content, boolean tts) {
		return null
	}

	Message editMessage(Message message, String newContent) {
		return null
	}

	void deleteMessage(Message message) {

	}

	void acknowledgeMessage(Message message) {

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

	List<Channel> getChannelsForServer(Server server) {
		return server.getChannels()
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

	void ban(Server server, User user) {

	}

	void unban(Server server, User user) {

	}

	Role createRole(Server server, Map<String, Object> data) {
		return null
	}

	Role editRole(Server server, Role role, Map<String, Object> data) {
		return null
	}

	void deleteRole(Server server, Role role) {

	}

	void playGame(String name) {

	}

	void beIdle() {

	}

	Map<String, Object> editProfile(Map<String, Object> data) {
		return null
	}
}
