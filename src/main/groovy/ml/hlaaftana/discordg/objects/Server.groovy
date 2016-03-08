package ml.hlaaftana.discordg.objects

import java.net.URL;
import java.util.List
import java.util.Map

import ml.hlaaftana.discordg.util.*

/**
 * A Discord server/guild.
 * @author Hlaaftana
 */
class Server extends DiscordObject {
	Server(API api, Map object){
		super(api, object)
	}

	/**
	 * @return the region the server is in.
	 */
	String getRegionId(){ return this.object["region"] }
	/**
	 * @return the timestamp of when this server was created.
	 */
	String getRawCreateTime(){ return this.object["joined_at"] }
	/**
	 * @return the timestamp of when this server was created.
	 */
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.object["joined_at"]) }
	/**
	 * @return the hash/ID of this server's icon.
	 */
	String getIconHash(){ return this.object["icon"] }
	/**
	 * @return the URL of the icon of this server as a string.
	 */
	String getIcon() {
		if (this.iconHash != null){
			return "https://discordapp.com/api/guilds/${this.id}/icons/${this.iconHash}.jpg"
		}else{
			return ""
		}
	}

	/**
	 * @return the owner of this server as a Member object.
	 */
	Member getOwner() { return this.members.find { it.id == this.object["owner_id"] } }

	/**
	 * @return the default text channel for this server.
	 */
	TextChannel getDefaultChannel(){ return this.textChannels.find { it.id == this.id } }
	VoiceChannel getAfkChannel(){ return this.voiceChannels.find { it.id == this.object["afk_channel_id"] } }
	int getAfkTimeout(){ return this.object["afk_timeout"] }
	Channel getWidgetChannel(){ return this.channels.find { it.id == this.object["embed_channel_id"] } }
	boolean isWidgetEnabled(){ return this.object["embed_enabled"] }
	boolean isLarge(){ return this.object["large"] }
	VerificationLevels getVerificationLevel(){ return VerificationLevels.get(this.object["verification_level"]) }
	int getRawVerificationLevel(){ return this.object["verification_level"] }

	/**
	 * @return the {@literal @everyone} role for this server.
	 */
	Role getDefaultRole(){ return this.roles.find { it.id == this.id } }

	/**
	 * Edit this server. This can be:
	 * [name: "Boy oh boy oh boy", region: "london", icon: ConversionUtil.encodeToBase64(new File("image.jpg")), afkChannel: voiceChannel, afkTimeout: 60 * 60 * 1000, owner: user]
	 * @return the edited server as a Server object.
	 */
	Server edit(Map data) {
		Map copyOfData = [name: data.name, region: data.region, icon: data.icon, afk_channel_id: data.afkChannel?.id, afk_timeout: data.afkTimeout, owner_id: data.owner?.id, verification_level: data.verificationLevel?.level]
		Map copyOfCopyOfData = [:] << copyOfData // avoid concurrentmodificationexception
		copyOfData.entrySet().each {
			if (it.value == null){
				if (it.key == "name"){
					copyOfCopyOfData.name = this.name
				}else{
					copyOfCopyOfData.remove(it.key, it.value)
				}
			}
		}
		copyOfData = copyOfCopyOfData
		return new Server(api, this.object << JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.id}", copyOfData)))
	}

	/**
	 * Leaves the server.
	 */
	void leave() {
		api.requester.delete("https://discordapp.com/api/users/@me/guilds/${this.id}")
	}

	/**
	 * Deletes the server.
	 */
	void delete() {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}")
	}

	/**
	 * Creates a new text channel in the server.
	 * @param name - the name of the channel.
	 * @return a TextChannel object of the created text channel.
	 */
	TextChannel createTextChannel(String name) {
		return new TextChannel(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/guilds/${this.id}/channels", ["name": name, "type": "text"])))
	}

	/**
	 * Creates a new voice channel in the server.
	 * @param name - the name of the channel.
	 * @return a VoiceChannel object of the created voice channel.
	 */
	VoiceChannel createVoiceChannel(String name) {
		return new VoiceChannel(api, JSONUtil.parse(api.requester.post("https://discordapp.com/api/guilds/${this.id}/channels", ["name": name, "type": "voice"])))
	}

	/**
	 * @return a List of TextChannels in the server.
	 */
	List<TextChannel> requestTextChannels(){
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
	List<VoiceChannel> requestVoiceChannels(){
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
	TextChannel requestTextChannelById(String id){
		return this.requestTextChannels().find { it.id == id }
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	VoiceChannel requestVoiceChannelById(String id){
		return this.requestVoiceChannels().find { it.id == id }
	}

	/**
	 * @return all channels in the server.
	 */
	List<Channel> requestChannels(){
		List array = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/channels"))
		List<Channel> channels = []
		for (o in array){
			channels.add(new Channel(api, o))
		}
		return channels
	}

	/**
	 * @return a List of TextChannels in the server.
	 */
	List<TextChannel> getTextChannels(){
		return this.channels.findAll { it.type == "text" }.collect { new TextChannel(api, it.object) }
	}

	/**
	 * @return a List of VoiceChannels in the server.
	 */
	List<VoiceChannel> getVoiceChannels(){
		return this.channels.findAll { it.type == "voice" }.collect { new VoiceChannel(api, it.object) }
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	TextChannel getTextChannelById(String id){
		return this.textChannels.find { it.id == id }
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID of the channel.
	 * @return the channel.
	 */
	VoiceChannel getVoiceChannelById(String id){
		return this.voiceChannels.find { it.id == id }
	}

	/**
	 * @return all channels in the server.
	 */
	List<Channel> getChannels(){
		return this.object["channels"].collect { (it.type == "text") ? new TextChannel(api, it) : new VoiceChannel(api, it) }
	}

	/**
	 * @return all roles in the server.
	 */
	List<Role> getRoles() {
		List array = this.object["roles"].collect { it }
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
		List array = this.object["members"].collect { it }
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
		this.editRoles(member, member.roles + roles)
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

	List<VoiceState> getVoiceStates(){
		return this.object["voice_states"].collect { new VoiceState(api, it) }
	}

	List<Invite> getInvites(){
		return JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/invites")).collect { new Invite(api, it) }
	}

	List<Region> getRegions(){
		return JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/regions")).collect { new Region(api, it) }
	}

	Region getRegion(){
		return this.regions.find { it.id == this.regionId }
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
		if (data["color"] instanceof Color) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions) data["permissions"] = data["permissions"].value
		return new Role(api, JSONUtil.parse(api.requester.patch("https://discordapp.com/api/guilds/${this.id}/roles/${role.id}", data)))
	}

	/**
	 * Deletes a role.
	 * @param role - the Role object.
	 */
	void deleteRole(Role role) {
		api.requester.delete("https://discordapp.com/api/guilds/${this.id}/roles/${role.id}")
	}

	List<Member> requestMembers(int limit=this.memberCount, boolean updateReady=true){
		List members = JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/members?limit=${limit}"))
		if (updateReady){
			api.readyData["guilds"].find { it.id == this.id }["members"] = members
			api.readyData["guilds"].find { it.id == this.id }["member_count"] = members.size()
		}
		return members.collect { new Member(api, it + ["guild_id": this.id]) }
	}

	Member getMemberInfo(String id){ return new Member(api, JSONUtil.parse(api.requester.get("https://discordapp.com/api/guilds/${this.id}/members/${id}"))) }
	Member memberInfo(String id){ this.getMemberInfo(id) }

	int getMemberCount(){ return this.object["member_count"] }
	List<Emoji> getEmojis(){ return this.object["emojis"].collect { new Emoji(api, it + [guild_id: this.id]) } }
	List<Emoji> getEmoji(){ return this.object["emojis"].collect { new Emoji(api, it + [guild_id: this.id]) } }

	Member getMember(User user){ return this.members.find { it.id == user.id } }
	Member member(User user){ return this.members.find { it.id == user.id } }

	Message sendMessage(String message, boolean tts=false){ this.defaultChannel.sendMessage(message, tts) }
	Message sendFile(File file){ this.defaultChannel.sendFile(file) }
	Message sendFile(String filePath){ this.defaultChannel.sendFile(filePath) }

	static class VoiceState extends DiscordObject {
		VoiceState(API api, Map object){ super(api, object) }

		VoiceChannel getChannel(){ return api.client.getVoiceChannelById(this.object["channel_id"]) }
		VoiceChannel getVoiceChannel(){ return api.client.getVoiceChannelById(this.object["channel_id"]) }
		User getUser(){ return api.client.getUserById(this.object["user_id"]) }
		Server getServer(){ return api.client.getServerById(this.object["guild_id"]) }
		Member getMember(){ return this.server.member(this.user) }
		boolean isDeaf(){ return this.object["deaf"] }
		boolean isMute(){ return this.object["mute"] }
		boolean isDeafened(){ return this.object["deaf"] }
		boolean isMuted(){ return this.object["mute"] }
		boolean isSelfDeaf(){ return this.object["self_deaf"] }
		boolean isSelfMute(){ return this.object["self_mute"] }
		boolean isSelfDeafened(){ return this.object["self_deaf"] }
		boolean isSelfMuted(){ return this.object["self_mute"] }
		boolean isSuppress(){ return this.object["suppress"] }
		String getToken(){ return this.object["token"] }
		String getSessionId(){ return this.object["session_id"] }
	}

	static enum VerificationLevels {
		NONE(0),
		LOW(1),
		MEDIUM(2),
		HIGH(3),
		TABLEFLIP(3),

		int level
		VerificationLevels(int level){ this.level = level }

		static VerificationLevels get(int level){ return VerificationLevels.class.enumConstants.find { it.level == level } }
	}
}
