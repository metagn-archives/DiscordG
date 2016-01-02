package hlaaftana.discordg.objects

import java.net.URL;
import java.util.List
import java.util.Map

import org.json.JSONArray
import org.json.JSONObject

class Server extends Base {
	API api
	JSONObject object
	Server(API api, JSONObject object){
		super(object)
		this.api = api
		this.object = object
	}

	String getRegion(){ return object.getString("region") }
	String getCreatedTimestamp(){ return object.getString("joined_at") }
	String getIconHash(){ return object.getString("icon") }
	String getIcon() {
		if (this.getIconHash != null){
			return "https://discordapp.com/api/users/${this.getID()}/icons/${this.getIconHash()}.jpg"
		}else{
			return ""
		}
	}
	URL getIconURL() { return new URL(this.getIcon()) }
	public Member getOwner() {
		return
	}

	Server edit(String newName) {
		return new Server(api, new JSONObject(api.getRequester().patch("https://discordapp.com/api/guilds/${this.getID()}", new JSONObject().put("name", newName))))
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getID()}")
	}

	TextChannel createTextChannel(String name) {
		return null
	}

	VoiceChannel createVoiceChannel(String name) {
		return null
	}

	List<TextChannel> getTextChannels(){
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/guilds/${this.getID()}/channels"))
		List<TextChannel> channels = new ArrayList<TextChannel>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				channels.add(new TextChannel(api, array.get(i)))
			}catch (e){
				break
			}
		}
		return channels
	}

	List<VoiceChannel> getVoiceChannels(){
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/guilds/${this.getID()}/channels"))
		List<VoiceChannel> channels = new ArrayList<VoiceChannel>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				channels.add(new VoiceChannel(array.get(i)))
			}catch (e){
				break
			}
		}
		return channels
	}

	List<Role> getRoles() {
		List<Role> roles = new ArrayList<Role>()
		JSONArray roleArray = object.getJSONArray("roles")
		roleArray.forEach { s ->
			roles.add(new Role(s))
		}
		return roles
	}

	List<Member> getMembers() {
		List<Member> members = new ArrayList<Member>()
		JSONArray memberArray = object.getJSONArray("members")
		memberArray.forEach { s ->
			members.add(new Member(s))
		}
		return members
	}

	Member getMemberForUser(User user) {
		return null
	}

	void editRoles(Member member, List<Role> roles) {
		JSONObject object = new JSONObject()
		JSONArray rolesArray = new JSONArray()
		for (r in roles){
			rolesArray.put(r.getID())
		}
		api.getRequester().patch("https://discordapp.com/api/guilds/${this.getID()}/members/${member.getID()}", object.put("roles", rolesArray))
	}

	void addRoles(Member member, List<Role> roles) {

	}

	void removeRoles(Member member, List<Role> roles) {

	}

	void kickMember(Member member) {

	}

	List<User> getBans() {
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/guilds/${this.getID()}/bans"))
		List<User> bans = new ArrayList<User>()
		array.forEach { JSONObject s ->
			bans.add(new User(s.getJSONObject("user")))
		}
		return bans
	}

	void ban(User user, int days=0) {
		api.getRequester().put("https://discordapp.com/api/guilds/${this.getID()}/bans/${user.getID()}?delete-message-days=${days}")
	}

	void unban(User user) {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getID()}/bans/${user.getID()}")
	}

	Role createRole(Map<String, Object> data) {
		Role createdRole = new Role(new JSONObject(api.getRequester().post("https://discordapp.com/api/guilds/${this.getID()}/roles", null)))
		return editRole(createdRole, data)
	}

	Role editRole(Role role, Map<String, Object> data) {
		JSONObject obj = new JSONObject()
		for (k in data.keySet()){
			obj.put(k, data.get(k))
		}
		return new Role(new JSONObject(api.getRequester().patch("https://discordapp.com/api/guilds/${this.getID()}/roles/${role.getID()}", obj)))
	}

	void deleteRole(Role role) {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getID()}/roles/${role.getID()}")
	}
}
