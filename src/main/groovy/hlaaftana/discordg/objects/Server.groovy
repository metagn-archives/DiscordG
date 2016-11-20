package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.util.*
import java.awt.Color
import java.util.List;
import java.util.concurrent.Callable

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import groovy.transform.Memoized
import groovy.transform.InheritConstructors

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
			http.jsonPatch("members/@me/nick", [nick: newNick])["nick"]
		}
	}
	String nick(String newNick){ changeNick(newNick) }
	String editNick(String newNick){ changeNick(newNick) }
	String resetNick(){ changeNick("") }

	Channel getDefaultChannel(){ channel(id) }
	Channel getAfkChannel(){ channel(object.afk_channel_id) }
	int getAfkTimeout(){ object.afk_timeout }
	Channel getWidgetChannel(){ channel(object.embed_channel_id) }
	boolean isWidgetEnabled(){ object.embed_enabled }
	boolean isLarge(){ object.large }
	boolean isUnavailable(){ object.unavailable }
	int getVerificationLevel(){ object.verification_level }
	int getMfaLevel(){ object.mfa_level }
	boolean isMfaRequiredForStaff(){ mfaLevel == MFALevelTypes.ELEVATED }

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
		new Server(client, object << http.jsonPatch("", copyOfData))
	}

	void leave() {
		client.http.delete("users/@me/guilds/${id}")
	}

	void delete() {
		http.delete("")
	}

	Channel createTextChannel(String name) {
		new Channel(client, http.jsonPost("channels", [name: name, type: 0]))
	}

	Channel createVoiceChannel(String name) {
		new Channel(client, http.jsonPost("channels", [name: name, type: 2]))
	}

	List<Channel> requestTextChannels(){
		requestChannels().findAll { it.text }
	}

	List<Channel> requestVoiceChannels(){
		requestChannels().findAll { it.voice }
	}

	Channel requestChannel(id){
		new Channel(client, http.jsonGet("channels/${this.id(id)}"))
	}

	List<Channel> requestChannels(){
		http.jsonGet("channels").collect { new Channel(client, it) }
	}

	List<Channel> getTextChannels(){ channels.findAll { it.text } }
	Map<String, Channel> getTextChannelMap(){ channelMap.findAll { k, v -> v.text } }

	List<Channel> getVoiceChannels(){ channels.findAll { it.voice } }
	Map<String, Channel> getVoiceChannelMap(){ channelMap.findAll { k, v -> v.voice } }

	Channel textChannel(id){ find(textChannels, id) }

	Channel voiceChannel(id){ find(voiceChannels, id) }

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
	Map<String, Presence> getPresenceMap(){ object["presences"].map }

	Presence presence(ass){ find(presences, ass) }

	void editRoles(member, List<Role> roles) {
		client.askPool("editMembers", id){
			http.patch("members/${id(member)}", ["roles": roles*.id])
		}
	}

	void addRoles(Member member, List<Role> roles) {
		editRoles(member, member.roles + roles)
	}

	void addRole(Member member, Role role){ addRoles(member, [role]) }

	void kick(member) {
		http.delete("members/${id(member)}")
	}

	List<User> getBans() {
		http.jsonGet("bans").collect { new User(client, it["user"]) }
	}

	List<VoiceState> getVoiceStates(){
		object["voice_states"].list
	}

	Map<String, VoiceState> getVoiceStateMap(){
		object["voice_states"].map
	}

	List<Invite> getInvites(){
		http.jsonGet("invites").collect { new Invite(client, it) }
	}

	List<Region> getRegions(){
		http.jsonGet("regions").collect { new Region(client, it) }
	}

	List<Integration> getIntegrations(){
		http.jsonGet("integrations").collect { new Integration(client, it) }
	}

	Integration createIntegration(String type, String id){
		new Integration(client, http.jsonPost("integrations", [type: type, id: id]))
	}

	String getRegion(){
		object["region"]
	}

	void ban(user, int days=0) {
		http.put("bans/${id(user)}?delete-message-days=$days", [:])
	}

	void unban(user) {
		http.delete("bans/${id(user)}")
	}

	int checkPrune(int days){
		http.jsonGet("prune?days=$days")["pruned"]
	}

	int prune(int days){
		http.jsonPost("prune?days=$days")["pruned"]
	}

	Role createRole(Map<String, Object> data) {
		Map defaultData = [color: 0, hoist: false, name: "new role", permissions: defaultRole.permissionValue]
		Role createdRole = new Role(client, http.jsonPost("roles", [:]))
		editRole(createdRole, defaultData << data)
	}

	Role editRole(Role role, Map<String, Object> data) {
		Map defaultData = [name: role.name, color: role.colorValue, permissions: role.permissionValue, hoist: role.hoist]
		if (data["color"] instanceof Color) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions) data["permissions"] = data["permissions"].value
		new Role(client, http.jsonPatch("roles/${role.id}", defaultData << data))
	}

	void deleteRole(role) {
		http.delete("roles/${id(role)}")
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
		http.jsonPatch("roles", hah).collect { new Role(client, it + [guild_id: id]) }
	}

	List<Webhook> requestWebhooks(){
		http.jsonGet("webhooks").collect { new Webhook(client, it) }
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
		http.patch("channels", hah)
	}

	List<Member> requestMembers(int max=1000, boolean updateCache=true){
		List members = http.jsonGet("members?limit=${max}")
		if (max > 1000){
			for (int m = 1; m < (int) Math.ceil(max / 1000) - 1; m++){
				members += http.jsonGet("members?after=${(m * 1000) + 1}&limit=1000")
			}
			members += http.jsonGet("members?after=${(int)((Math.ceil(max / 1000) - 1) * 1000)+1}&limit=1000")
		}
		if (updateCache){
			client.cache["guilds"][id]["members"] = new DiscordListCache(members.collect { it + ["guild_id": id] + it["user"] }, client, Member)
			client.cache["guilds"][id]["member_count"] = members.size()
		}
		members.collect { new Member(client, it + ["guild_id": id]) }
	}

	Member requestMember(id){ new Member(client, http.jsonGet("members/${DiscordObject.id(id)}")) }

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

	Embed getEmbed(){ new Embed(client, http.jsonGet("embed")) }

	static Map construct(Client client, Map g){
		g["members"] = new DiscordListCache(g.members.collect { it << [guild_id: g["id"]] << it["user"] }, client, Member)
		g["presences"] = new DiscordListCache(g.presences.collect { it << [guild_id: g["id"]] << it["user"] }, client, Presence)
		g["emojis"] = new DiscordListCache(g.emojis.collect { it << [guild_id: g["id"]] }, client, Emoji)
		g["roles"] = new DiscordListCache(g.roles.collect { it << [guild_id: g["id"]] }, client, Role)
		g["channels"] = new DiscordListCache(g.channels.collect { Channel.construct(client, it, g["id"]) }, client, Channel)
		g["voice_states"] = new DiscordListCache(g.voice_states.collect { it << [guild_id: g["id"], id: it["user_id"]] }, client, VoiceState)
		g
	}

	static class Embed extends DiscordObject {
		Embed(Client client, Map object){ super(client, object) }

		String getId(){ object["channel_id"] }
		String getName(){ channel.name }
		boolean isEnabled(){ object["enabled"] }
		Channel getChannel(){ client.channel(object["channel_id"]) }
		Server getServer(){ channel.server }
		Embed edit(Map data){
			Map json = [channel_id: (data["channel"] instanceof Channel) ? data["channel"].id : data["channel"].toString() ?: channel.id, enabled: data["enabled"] ?: enabled]
			new Embed(client, http.jsonPatch("embed", json))
		}
	}
}

class VoiceState extends DiscordObject {
	VoiceState(Client client, Map object){ super(client, object) }

	Channel getChannel(){ client.channel(object["channel_id"]) }
	Channel getVoiceChannel(){ client.channel(object["channel_id"]) }
	User getUser(){ client.user(object["user_id"]) }
	Server getServer(){ object["guild_id"] ? client.server(object["guild_id"]) : channel.server }
	Channel getParent(){ channel }
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

class Integration extends DiscordObject {
	Integration(Client client, Map object){ super(client, object, "guilds/${client.role(object["role_id"]).object["guild_id"]}/integrations/$object.id") }

	int getSubscriberCount(){ object["subscriber_count"] }
	boolean isSyncing(){ object["syncing"] }
	boolean isEnableEmoticons(){ object["enable_emoticons"] }
	int getExpireBehaviour(){ object["expire_behaviour"] }
	int getExpireGracePeriod(){ object["expire_grace_period"] }
	User getUser(){ new User(client, object["user"]) }
	DiscordObject getAccount(){ new DiscordObject(client, object["account"]) }
	boolean isEnabled(){ object["enabled"] }
	Role getRole(){ client.role(object["role_id"]) }
	Server getServer(){ role.server }
	String getRawSyncTime(){ object["synced_at"] }
	Date getSyncTime(){ ConversionUtil.fromJsonDate(object["synced_at"]) }
	String getType(){ object["type"] }
	Integration edit(Map data){
		new Integration(client, http.jsonPatch("", data))
	}
	void delete(){
		http.delete("")
	}
	void sync(){
		http.post("sync")
	}
}

@InheritConstructors
class Emoji extends DiscordObject {
	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }
	List<Role> getRoles(){ server.roles.findAll { it.id in object["roles"] } }
	boolean requiresColons(){ object["require_colons"] }
	boolean requireColons(){ object["require_colons"] }
	boolean isRequiresColons(){ object["require_colons"] }
	boolean isRequireColons(){ object["require_colons"] }
	boolean isManaged(){ object["managed"] }
	String getUrl(){ "https://cdn.discordapp.com/emojis/${id}.png" }
}

@InheritConstructors
class Role extends DiscordObject{
	static final Color DEFAULT = new Color(0)
	static final Color AQUA = new Color(0x1ABC9C)
	static final Color DARK_AQUA = new Color(0x11806a)
	static final Color GREEN = new Color(0x2ECC71)
	static final Color DARK_GREEN = new Color(0x1F8B4C)
	static final Color BLUE = new Color(0x3498DB)
	static final Color DARK_BLUE = new Color(0x206694)
	static final Color PURPLE = new Color(0x9B59B6)
	static final Color DARK_PURPLE = new Color(0x71368A)
	static final Color MAGENTA = new Color(0xE91E63)
	static final Color DARK_MAGENTA = new Color(0xAD1457)
	static final Color GOLD = new Color(0xF1C40F)
	static final Color DARK_GOLD = new Color(0xC27C0E)
	static final Color ORANGE = new Color(0xE67E22)
	static final Color DARK_ORANGE = new Color(0xA84300)
	static final Color RED = new Color(0xE74C3C)
	static final Color DARK_RED = new Color(0x992D22)
	static final Color LIGHT_GRAY = new Color(0x95A5A6)
	static final Color GRAY = new Color(0x607D8B)
	static final Color LIGHT_BLUE_GRAY = new Color(0x979C9F)
	static final Color BLUE_GRAY = new Color(0x546E7A)
	static final Color LIGHT_GREY = new Color(0x95A5A6)
	static final Color GREY = new Color(0x607D8B)
	static final Color LIGHT_BLUE_GREY = new Color(0x979C9F)
	static final Color BLUE_GREY = new Color(0x546E7A)
	static final MENTION_REGEX = { String id = /\d+/ -> /<@&$id>/ }

	int getColorValue(){ object["color"] }
	Color getColor(){ new Color(object["color"]) }
	boolean isLocked(){ isLockedFor(server.me) }
	boolean isLockedFor(user){
		position >= server.member(user).primaryRole.position
	}
	boolean isHoist(){ object["hoist"] }
	boolean isManaged(){ object["managed"] }
	boolean isMentionable(){ object["mentionable"] }
	Permissions getPermissions(){ new Permissions(object["permissions"]) }
	int getPermissionValue(){ object["permissions"] }
	int getPosition(){ object["position"] }

	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }

	String getMention(){ "<@&${id}>" }
	String getMentionRegex(){ MENTION_REGEX(id) }

	List<Member> getMembers(){ server.members.findAll { id in it.object.roles } }
	boolean isUsed(){ server.members*.object*.roles.flatten().contains(id) }
	Role edit(Map data){ server.editRole(this, data) }
	void delete(){ server.deleteRole(this) }
	void addTo(Member user){ server.addRole(user, this) }
	void addTo(List<Member> users){ users.each { server.addRole(it, this) } }
}

class Member extends User {
	Member(Client client, Map object){
		super(client, object + object["user"], "/guilds/${object["guild_id"]}/members/${object == client ? "@me" : object["id"]}")
	}

	User getUser(){ new User(client, object["user"]) }
	String getNick(){ object["nick"] ?: name }
	String getRawNick(){ object["nick"] }
	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }
	String getRawJoinDate(){ object["joined_at"] }
	Date getJoinDate(){ ConversionUtil.fromJsonDate(rawJoinDate) }
	List<Role> getRoles(){
		object["roles"].collect { server.role(it) }
	}

	Role role(ass){ find(roles, ass) }

	Game getGame(){
		presence?.game ?: null
	}

	Presence getPresence(){
		server.presenceMap[id]
	}

	void edit(Map data){
		client.askPool("editMembers", server.id){
			http.patch("", data)
		}
	}

	void changeNick(String newNick){
		id == client.cache["user"]["id"] ? server.nick(newNick) : edit(nick: newNick)
	}
	void nick(String newNick){ changeNick(newNick) }
	void editNick(String newNick){ changeNick(newNick) }
	void resetNick(){ changeNick("") }

	void mute(){ edit(mute: true) }
	void unmute(){ edit(mute: false) }
	void deafen(){ edit(deaf: true) }
	void undeafen(){ edit(deaf: false) }
	void ban(int days = 0){ server.ban this, days }
	void unban(){ server.unban this }
	boolean isMute(){ object["mute"] }
	boolean isDeaf(){ object["deaf"] }
	boolean isDeafened(){ object["deaf"] }

	String getStatus(){
		presence?.status ?: "offline"
	}

	Role getPrimaryRole(){ roles.max { it.position } }
	int getColorValue(){ roles.findAll { it.colorValue != 0 }.max { it.position }?.colorValue ?: 0 }
	Color getColor(){ new Color(colorValue) }
	Permissions getPermissions(Permissions initialPerms = Permissions.ALL_FALSE){
		Permissions full = initialPerms + server.defaultRole.permissions
		for (Permissions perms in roles*.permissions){
			if (perms["administrator"]){
				full += Permissions.ALL_TRUE
				break
			}else{
				full += perms
			}
		}
		full
	}

	Permissions fullPermissionsFor(Channel channel){
		permissionsFor(channel, permissions)
	}

	void editRoles(List<Role> roles) {
		server.editRoles(this, roles)
	}

	void addRoles(List<Role> roles) {
		server.addRoles(this, roles)
	}

	void addRole(Role role){ addRoles([role]) }

	void kick() {
		server.kick(this)
	}

	void moveTo(channel){
		http.patch("", [channel_id: id(channel)])
	}

	User toUser(){ new User(client, object["user"]) }
	def asType(Class target){
		if (target == User) toUser()
		else super.asType(target)
	}
	String toString(){ nick }
}

@InheritConstructors
class Region extends DiscordObject {
	String getSampleHostname(){ object["sample_hostname"] }
	int getSamplePort(){ object["sample_port"] }
	boolean isVip(){ object["vip"] }
	boolean isOptimal(){ object["optimal"] }
}

@InheritConstructors
class Presence extends DiscordObject {
	Game getGame(){ object["game"] ? new Game(client, object["game"]) : null }
	String getStatus(){ object["status"] }
	Server getServer(){ client.serverMap[object["guild_id"]] }
	Server getParent(){ server }
	Member getMember(){ server ? server.memberMap[id] : client.members(id)[0] }
	String getName(){ member.name }
}

@InheritConstructors
class Game extends DiscordObject {
	String getId(){ object["type"].toString() ?: "0" }
	int getType(){ object["type"] ?: 0 }
	String getUrl(){ object["url"] }
	String toString(){ type == 0 ? name : "$name ($url)" }
}