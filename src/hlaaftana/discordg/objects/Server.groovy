package hlaaftana.discordg.objects

import java.net.URL;
import java.util.List
import java.util.Map

import hlaaftana.discordg.util.JSONUtil

class Server extends Base {
	Server(API api, Map object){
		super(api, object)
	}

	String getRegion(){ return object["region"] }
	String getCreatedTimestamp(){ return object["joined_at"] }
	String getIconHash(){ return object["icon"] }
	String getIcon() {
		if (this.getIconHash != null){
			return "https://discordapp.com/api/users/${this.getId()}/icons/${this.getIconHash()}.jpg"
		}else{
			return ""
		}
	}
	URL getIconURL() { return new URL(this.getIcon()) }

	Member getOwner() {
		for (m in this.getMembers()){
			if (m.getId().equals(object["owner_id"])){
				return m
			}
		}
		return null
	}

	TextChannel getDefaultChannel(){
		return this.getTextChannels().find { it.id.equals(this.getId()) }
	}

	Role getDefaultRole(){ return this.getRoles().find { it.id.equals(this.getId()) } }

	Server edit(String newName) {
		return new Server(api, JSONUtil.parse(api.getRequester().patch("https://discordapp.com/api/guilds/${this.getId()}", new HashMap().put("name", newName))))
	}

	void delete() {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getId()}")
	}

	TextChannel createTextChannel(String name) {
		return new TextChannel(api, api.getRequester().post("https://discordapp.com/api/guilds/${this.getId()}/channels", ["name": name, "type": "text"]))
	}

	VoiceChannel createVoiceChannel(String name) {
		return new VoiceChannel(api, api.getRequester().post("https://discordapp.com/api/guilds/${this.getId()}/channels", ["name": name, "type": "voice"]))
	}

	List<TextChannel> getTextChannels(){
		List array = JSONUtil.parse(api.getRequester().get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<TextChannel> channels = new ArrayList<TextChannel>()
		for (o in array){
			if (o["type"].equals("text")) channels.add(new TextChannel(api, o))
		}
		return channels
	}

	List<VoiceChannel> getVoiceChannels(){
		List array = JSONUtil.parse(api.getRequester().get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<VoiceChannel> channels = new ArrayList<VoiceChannel>()
		for (o in array){
			if (o["type"].equals("voice")) channels.add(new VoiceChannel(api, o))
		}
		return channels
	}

	TextChannel getTextChannelById(String id){
		for (tc in this.getTextChannels()){
			if (tc.getId().equals(id)) return tc
		}
		return null
	}

	VoiceChannel getVoiceChannelById(String id){
		for (vc in this.getVoiceChannels()){
			if (vc.getId().equals(id)) return vc
		}
		return null
	}

	List<Channel> getChannels(){
		List array = JSONUtil.parse(api.getRequester().get("https://discordapp.com/api/guilds/${this.getId()}/channels"))
		List<Channel> channels = new ArrayList<Channel>()
		for (o in array){
			channels.add(new Channel(api, o))
		}
		return channels
	}

	List<Role> getRoles() {
		List array = object["roles"]
		List<Role> roles = new ArrayList<Role>()
		for (o in array){
			roles.add(new Role(api, o))
		}
		return roles
	}

	List<Member> getMembers() {
		List array = object["members"]
		List<Member> members = new ArrayList<Member>()
		for (o in array){
			members.add(new Member(api, o))
		}
		return members
	}

	void editRoles(Member member, List<Role> roles) {
		List rolesArray = new ArrayList()
		for (r in roles){
			rolesArray.add(r.getId())
		}
		api.getRequester().patch("https://discordapp.com/api/guilds/${this.getId()}/members/${member.getId()}", ["roles": rolesArray])
	}

	void addRoles(Member member, List<Role> roles) {
		this.editRoles(member, member.getRoles().addAll(roles))
	}

	void kickMember(Member member) {
		member.kick()
	}

	List<User> getBans() {
		List array = JSONUtil.parse(api.getRequester().get("https://discordapp.com/api/guilds/${this.getId()}/bans"))
		List<User> bans = new ArrayList<User>()
		for (o in array){
			bans.add(new User(api, o))
		}
		return bans
	}

	void ban(User user, int days=0) {
		api.getRequester().put("https://discordapp.com/api/guilds/${this.getId()}/bans/${user.getId()}?delete-message-days=${days}")
	}

	void unban(User user) {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getId()}/bans/${user.getId()}")
	}

	Role createRole(Map<String, Object> data) {
		Role createdRole = new Role(api, JSONUtil.parse(api.getRequester().post("https://discordapp.com/api/guilds/${this.getId()}/roles", [:])))
		return editRole(createdRole, data)
	}

	Role editRole(Role role, Map<String, Object> data) {
		return new Role(api, JSONUtil.parse(api.getRequester().patch("https://discordapp.com/api/guilds/${this.getId()}/roles/${role.getId()}", data)))
	}

	void deleteRole(Role role) {
		api.getRequester().delete("https://discordapp.com/api/guilds/${this.getId()}/roles/${role.getId()}")
	}
}
