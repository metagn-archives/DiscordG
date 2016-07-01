package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.*
import java.awt.Color
import java.util.concurrent.Callable

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import groovy.transform.Memoized

/**
 * A Discord server/guild.
 * @author Hlaaftana
 */
class Server extends DiscordObject {
	Server(Client client, Map object){
		super(client, object, "guilds/$object.id")
	}

	String getRegionId(){ object["region"] }
	String getRawJoinTime(){ object["joined_at"] }
	Date getJoinTime(){ ConversionUtil.fromJsonDate(object["joined_at"]) }
	String getIconHash(){ object["icon"] }
	String getIcon() {
		if (iconHash) "https://cdn.discordapp.com/icons/$id/${iconHash}.jpg"
		else ""
	}

	InputStream getIconInputStream(){ icon.toURL().openConnection().with {
			setRequestProperty("User-Agent", client.fullUserAgent)
			setRequestProperty("Accept", "*/*")
			delegate
		}.inputStream
	}

	File downloadIcon(File file){ file.withOutputStream { out ->
			out << iconInputStream
			new File(file.path)
		}
	}

	Member getOwner() { members.find { it.id == object["owner_id"] } }

	Member getMe(){ members.find { it == client.user } }
	String changeNick(String newNick){
		client.askPool("changeNick"){
			requester.jsonPatch("members/@me/nick", [nick: newNick])["nick"]
		}
	}
	String nick(String newNick){ changeNick(newNick) }
	String editNick(String newNick){ changeNick(newNick) }
	String resetNick(){ changeNick("") }

	TextChannel getDefaultChannel(){ textChannels.find { it.id == id } }
	VoiceChannel getAfkChannel(){ voiceChannels.find { it.id == object["afk_channel_id"] } }
	int getAfkTimeout(){ object["afk_timeout"] }
	Channel getWidgetChannel(){ channels.find { it.id == object["embed_channel_id"] } }
	boolean isWidgetEnabled(){ object["embed_enabled"] }
	boolean isLarge(){ object["large"] as boolean }
	boolean isUnavailable(){ object["unavailable"] as boolean }
	VerificationLevels getVerificationLevel(){ VerificationLevels.get(object["verification_level"]) }
	int getRawVerificationLevel(){ object["verification_level"] }

	Role getDefaultRole(){ roles.find { it.id == id } }

	Server edit(Map data) {
		Map copyOfData = [name: data.name, region: data.region, icon: data.icon, afk_channel_id: data.afkChannel?.id, afk_timeout: data.afkTimeout, owner_id: data.owner?.id, verification_level: data.verificationLevel?.level]
		Map copyOfCopyOfData = [:] << copyOfData // avoid concurrentmodificationexception
		copyOfData.entrySet().each {
			if (it.value == null){
				if (it.key == "name"){
					copyOfCopyOfData.name = name
				}else{
					copyOfCopyOfData.remove(it.key, it.value)
				}
			}
		}
		copyOfData = copyOfCopyOfData
		new Server(client, object << requester.jsonPatch("guilds/${id}", copyOfData))
	}

	void leave() {
		client.requester.delete("users/@me/guilds/${id}")
	}

	void delete() {
		requester.delete("")
	}

	TextChannel createTextChannel(String name) {
		new TextChannel(client, requester.jsonPost("channels", ["name": name, "type": "text"]))
	}

	VoiceChannel createVoiceChannel(String name) {
		new VoiceChannel(client, requester.jsonPost("channels", ["name": name, "type": "voice"]))
	}

	List<TextChannel> requestTextChannels(){
		List array = requester.jsonGet("channels")
		List<TextChannel> channels = []
		for (o in array){
			if (o["type"] == "text") channels.add(new TextChannel(client, o))
		}
		channels
	}

	List<VoiceChannel> requestVoiceChannels(){
		List array = requester.jsonGet("channels")
		List<VoiceChannel> channels = []
		for (o in array){
			if (o["type"] == "voice") channels.add(new VoiceChannel(client, o))
		}
		channels
	}

	TextChannel requestTextChannelById(id){
		new TextChannel(client, requester.jsonGet("channels/${DiscordObject.id(id)}"))
	}

	VoiceChannel requestVoiceChannelById(id){
		new VoiceChannel(client, requester.jsonGet("channels/${DiscordObject.id(id)}"))
	}

	List<Channel> requestChannels(){
		List array = requester.jsonGet("channels")
		List<Channel> channels = []
		for (o in array){
			channels.add(new Channel(client, o))
		}
		channels
	}

	List<TextChannel> getTextChannels(){ channels.findAll { it.type == "text" }
	}
	Map<String, TextChannel> getTextChannelMap(){ channelMap.findAll { k, v -> v.type == "text" } }

	List<VoiceChannel> getVoiceChannels(){ channels.findAll { it.type == "voice" }
	}
	Map<String, VoiceChannel> getVoiceChannelMap(){ channelMap.findAll { k, v -> v.type == "voice" } }

	TextChannel textChannel(id){ find(textChannels, id) }

	VoiceChannel voiceChannel(id){ find(voiceChannels, id) }

	Channel channel(id){ find(channels, id) }

	List<Channel> getChannels(){ object["channels"].list }
	Map<String, Channel> getChannelMap(){
		object["channels"].map
	}

	List<Role> getRoles(){ object["roles"].list }
	Map<String, Role> getRoleMap(){ object["roles"].map }

	Role role(ass){ find(roles, ass) }

	List<Member> getMembers(){ object["members"].list }
	Map<String, Member> getMemberMap(){ object["members"].map }

	List<Presence> getPresences(){ object["presences"].list }
	Map<String, Presence> getPresenceMap(){ object["presences"].map
	}

	Presence presence(ass){ find(presences, ass) }

	void editRoles(member, List<Role> roles) {
		client.askPool("editMembers", id){
			requester.patch("members/${id(member)}", ["roles": roles*.id])
		}
	}

	void addRoles(Member member, List<Role> roles) {
		editRoles(member, member.roles + roles)
	}

	void addRole(Member member, Role role){ addRoles(member, [role]) }

	void kick(member) {
		requester.delete("members/${id(member)}")
	}

	List<User> getBans() {
		requester.jsonGet("bans").collect { new User(client, it["user"]) }
	}

	List<VoiceState> getVoiceStates(){
		object["voice_states"].list
	}

	Map<String, VoiceState> getVoiceStateMap(){
		object["voice_states"].map
	}

	List<Invite> getInvites(){
		requester.jsonGet("invites").collect { new Invite(client, it) }
	}

	List<Region> getRegions(){
		requester.jsonGet("regions").collect { new Region(client, it) }
	}

	List<Integration> getIntegrations(){
		requester.jsonGet("integrations").collect { new Integration(client, it) }
	}

	Integration createIntegration(String type, String id){
		new Integration(client, requester.jsonPost("integrations", [type: type, id: id]))
	}

	String getRegion(){
		object["region"]
	}

	void ban(user, int days=0) {
		requester.put("bans/${id(user)}?delete-message-days=$days", [:])
	}

	void unban(user) {
		requester.delete("bans/${id(user)}")
	}

	int checkPrune(int days){
		requester.jsonGet("prune?days=$days")["pruned"]
	}

	int prune(int days){
		requester.jsonPost("prune?days=$days")["pruned"]
	}

	Role createRole(Map<String, Object> data) {
		Map defaultData = [color: 0, hoist: false, name: "new role", permissions: defaultRole.permissionValue]
		Role createdRole = new Role(client, requester.jsonPost("roles", [:]))
		editRole(createdRole, defaultData << data)
	}

	Role editRole(Role role, Map<String, Object> data) {
		Map defaultData = [name: role.name, color: role.colorValue, permissions: role.permissionValue, hoist: role.hoist]
		if (data["color"] instanceof Color) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions) data["permissions"] = data["permissions"].value
		new Role(client, requester.jsonPatch("roles/${role.id}", defaultData << data))
	}

	void deleteRole(role) {
		requester.delete("roles/${id(role)}")
	}

	List<Role> batchModifyRoles(Closure closure){
		closure.delegate = this
		Thread ass = new Thread({
			use(Modifier, closure)
		} as Runnable)
		ass.start()
		ass.join()
		List hah = []
		Modifier.getAllModifications(ass).each { k, v ->
			if (v["color"] instanceof Color) v["color"] = v["color"].value
			if (v["permissions"] instanceof Permissions) v["permissions"] = v["permissions"].value
			hah.add(v + [id: id(k)])
		}
		requester.jsonPatch("roles", hah).collect { new Role(client, it + [guild_id: id]) }
	}

	void batchModifyChannels(Closure closure){
		closure.delegate = this
		Thread ass = new Thread({
			use(Modifier, closure)
		} as Runnable)
		ass.start()
		ass.join()
		List hah = []
		Modifier.getAllModifications(ass).each { k, v ->
			hah.add(v + [id: id(k)])
		}
		requester.patch("channels", hah)
	}

	List<Member> requestMembers(int max=1000, boolean updateCache=true){
		List members = requester.jsonGet("members?limit=${max}")
		if (max > 1000){
			for (int m = 1; m < (int) Math.ceil(max / 1000) - 1; m++){
				members += requester.jsonGet("members?after=${(m * 1000) + 1}&limit=1000")
			}
			members += requester.jsonGet("members?after=${(int)((Math.ceil(max / 1000) - 1) * 1000)+1}&limit=1000")
		}
		if (updateCache){
			client.cache["guilds"][id]["members"] = new DiscordListCache(members.collect { it + ["guild_id": id] + it["user"] }, client, Member)
			client.cache["guilds"][id]["member_count"] = members.size()
		}
		members.collect { new Member(client, it + ["guild_id": id]) }
	}

	Member requestMember(id){ new Member(client, requester.jsonGet("members/${DiscordObject.id(id)}")) }

	Member getLastMember(){ members.max { it.joinDate } }
	Member getLatestMember(){ members.max { it.joinDate } }
	int getMemberCount(){ object["member_count"] }
	List<Emoji> getEmojis(){ object["emojis"].list }
	List<Emoji> getEmoji(){ emojis }

	Member getMember(user){ find(members, user) }
	Member member(user){ find(members, user) }

	Message sendMessage(String message, boolean tts=false){ defaultChannel.sendMessage(message, tts) }
	Message sendFile(File file){ defaultChannel.sendFile(file) }
	Message sendFile(String filePath){ defaultChannel.sendFile(filePath) }

	Embed getEmbed(){ new Embed(client, requester.jsonGet("embed")) }

	static Map construct(Client client, Map g){
		g["members"] = new DiscordListCache(g.members.collect { it << [guild_id: g["id"]] << it["user"] }, client, Member)
		g["presences"] = new DiscordListCache(g.presences.collect { it << [guild_id: g["id"]] << it["user"] }, client, Presence)
		g["emojis"] = new DiscordListCache(g.emojis.collect { it << [guild_id: g["id"]] }, client, Emoji)
		g["roles"] = new DiscordListCache(g.roles.collect { it << [guild_id: g["id"]] }, client, Role)
		g["channels"] = new ChannelListCache(g.channels.collect { Channel.construct(client, it, g["id"]) }, client)
		g["voice_states"] = new DiscordListCache(g.voice_states.collect { it << [guild_id: g["id"], id: it["user_id"]] }, client, VoiceState)
		g
	}

	static class Embed extends APIMapObject {
		Embed(Client client, Map object){ super(client, object) }

		boolean isEnabled(){ object["enabled"] }
		Channel getChannel(){ client.channels.find { it.id == object["channel_id"] } }
		Server getServer(){ channel.server }
		Embed edit(Map data){
			Map fullData = [channel_id: (data["channel"] instanceof Channel) ? data["channel"].id : data["channel"].toString() ?: channel.id, enabled: data["enabled"] ?: enabled]
			new Embed(client, requester.jsonPatch("embed", fullData))
		}
	}

	static class VoiceState extends DiscordObject {
		VoiceState(Client client, Map object){ super(client, object) }

		VoiceChannel getChannel(){ client.voiceChannel(object["channel_id"]) }
		VoiceChannel getVoiceChannel(){ client.voiceChannel(object["channel_id"]) }
		User getUser(){ client.user(object["user_id"]) }
		Server getServer(){ object["guild_id"] ? client.server(object["guild_id"]) : channel.server }
		VoiceChannel getParent(){ channel }
		Member getMember(){ server.member(user) }
		boolean isDeaf(){ object["deaf"] }
		boolean isMute(){ object["mute"] }
		boolean isDeafened(){ object["deaf"] }
		boolean isMuted(){ object["mute"] }
		boolean isSelfDeaf(){ object["self_deaf"] }
		boolean isSelfMute(){ object["self_mute"] }
		boolean isSelfDeafened(){ object["self_deaf"] }
		boolean isSelfMuted(){ object["self_mute"] }
		boolean isSuppress(){ object["suppress"] }
		String getToken(){ object["token"] }
		String getSessionId(){ object["session_id"] }
		String getName(){ user.name }
	}

	static enum VerificationLevels {
		NONE(0),
		LOW(1),
		MEDIUM(2),
		HIGH(3),
		TABLEFLIP(3),

		int level
		VerificationLevels(int level){ this.level = level }

		@Memoized
		static VerificationLevels get(int level){ VerificationLevels.class.enumConstants.find { it.level == level } }
	}
}
