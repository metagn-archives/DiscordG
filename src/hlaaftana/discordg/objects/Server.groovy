package hlaaftana.discordg.objects

import java.net.URL;
import java.util.List
import java.util.Map

import org.json.JSONArray
import org.json.JSONObject

class Server extends Base {
	Server(API api, JSONObject object){
		super(api, object)
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
		return new TextChannel(api, api.getRequester().post("https://discordapp.com/api/guilds/${this.getID()}/channels", new JSONObject().put("name", name).put("type", "text")))
	}

	VoiceChannel createVoiceChannel(String name) {
		return new VoiceChannel(api, api.getRequester().post("https://discordapp.com/api/guilds/${this.getID()}/channels", new JSONObject().put("name", name).put("type", "voice")))
	}

	List<TextChannel> getTextChannels(){
		JSONArray array = new JSONArray(api.getRequester().get("https://discordapp.com/api/guilds/${this.getID()}/channels"))
		List<TextChannel> channels = new ArrayList<TextChannel>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				if (array.get(i).getString("type").equals("text"))
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
				if (array.get(i).getString("type").equals("voice"))
					channels.add(new VoiceChannel(api, array.get(i)))
			}catch (e){
				break
			}
		}
		return channels
	}

	List<Role> getRoles() {
		JSONArray array = object.getJSONArray("roles")
		List<Role> roles = new ArrayList<Role>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				roles.add(new Role(api, array.get(i)))
			}catch (e){
				break
			}
		}
		return roles
	}

	List<Member> getMembers() {
		JSONArray array = object.getJSONArray("members")
		List<Member> members = new ArrayList<Member>()
		for (int i = 0; i < Short.MAX_VALUE; i++){
			try{
				members.add(new Member(api, array.get(i)))
			}catch (e){
				break
			}
		}
		return members
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
