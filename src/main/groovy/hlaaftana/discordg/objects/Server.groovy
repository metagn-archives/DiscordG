package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.DiscordListCache
import hlaaftana.discordg.util.*
import java.awt.Color
import java.io.File;
import java.io.InputStream;
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
		super(client, object)
	}

	String getRegionId(){ object["region"] }
	String getRawJoinedAt(){ object["joined_at"] }
	Date getJoinedAt(){ ConversionUtil.fromJsonDate(rawJoinedAt) }
	String getIconHash(){ object["icon"] }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/icons/$id/${iconHash}.jpg" : ""
	}
	boolean hasIcon(){ object["icon"] }
	InputStream getIconInputStream(){ inputStreamFromDiscord(icon) }
	File downloadIcon(file){ downloadFileFromDiscord(icon, file) }

	String getOwnerId(){ object["owner_id"] }
	Member getOwner(){ member(ownerId) }

	Member getMe(){ member(client) }
	String changeNick(String newNick){ client.changeOwnServerNick(id, newNick) }
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

	Role getDefaultRole(){ role(id) }

	Server edit(Map data) {
		client.editServer(data, id)
	}

	void leave() {
		client.leaveServer(id)
	}

	void delete() {
		client.deleteServer(id)
	}

	Channel createTextChannel(String name) {
		client.createTextChannel(this, name)
	}

	Channel createVoiceChannel(String name) {
		client.createVoiceChannel(this, name)
	}

	Channel requestChannel(c){
		client.requestServerChannel(this, c)
	}

	List<Channel> requestChannels(){
		client.requestServerChannels(this)
	}

	List<Channel> getTextChannels(){ channels.findAll { it.text } }
	Map<String, Channel> getTextChannelMap(){ channelMap.findAll { k, v -> v.text } }

	List<Channel> getVoiceChannels(){ channels.findAll { it.voice } }
	Map<String, Channel> getVoiceChannelMap(){ channelMap.findAll { k, v -> v.voice } }

	Channel textChannel(id){ find(textChannels, id) }

	Channel voiceChannel(id){ find(voiceChannels, id) }

	Channel channel(id){ find(object.channels, id) }

	List<Channel> getChannels(){ object["channels"].list }
	Map<String, Channel> getChannelMap(){ object["channels"].map }

	List<Role> getRoles(){ object["roles"].list }
	Map<String, Role> getRoleMap(){ object["roles"].map }
	List<String> getUsedRoleIds(){ object["members"].values()*.roles.flatten() }

	Role role(ass){ find(object.roles, ass) }

	List<Member> getMembers(){ object["members"].list }
	Map<String, Member> getMemberMap(){ object["members"].map }

	List<Presence> getPresences(){ object["presences"].list }
	Map<String, Presence> getPresenceMap(){ object["presences"].map }

	Presence presence(ass){ find(object.presences, ass) }

	void editRoles(member, List roles) {
		client.editRoles(this, member, roles)
	}

	void addRoles(member, List roles) {
		client.addRoles(this, member, roles)
	}

	void addRole(member, role){
		client.addRole(this, member, role)
	}

	void removeRole(member, role){
		client.removeRole(this, member, role)
	}

	void kick(member) {
		client.kick(this, member)
	}

	List<Ban> requestBans(){
		client.requestBans(this)
	}

	List<VoiceState> getVoiceStates(){
		object["voice_states"].list
	}

	Map<String, VoiceState> getVoiceStateMap(){
		object["voice_states"].map
	}

	List<Invite> requestInvites(){
		client.requestServerInvites(this)
	}

	List<Region> requestRegions(){
		client.requestRegions(this)
	}

	List<Integration> requestIntegrations(){
		client.requestIntegrations(this)
	}

	Integration createIntegration(String type, String id){
		client.createIntegration(this, type, id)
	}

	String getRegion(){
		object["region"]
	}

	void ban(user, int days=0) {
		client.ban(this, user, days)
	}

	void unban(user) {
		client.unban(this, user)
	}

	int checkPrune(int days){
		client.checkPrune(this, days)
	}

	int prune(int days){
		client.prune(this, days)
	}

	Role createRole(Map data) {
		client.createRole(data, this)
	}

	Role editRole(role, Map data) {
		client.editRole(data, this, role)
	}

	void deleteRole(role) {
		client.deleteRole(this, role)
	}

	List<Role> batchModifyRoles(Map mods){
		client.batchModifyRoles(mods, this)
	}

	List<Webhook> requestWebhooks(){
		client.requestServerWebhooks(this)
	}

	void batchModifyChannels(Map mods){
		client.batchModifyChannels(mods, this)
	}

	List<Member> requestMembers(int max=1000, boolean updateCache=true){
		client.requestMembers(this, max, updateCache)
	}

	Member requestMember(id){ client.requestMember(this, id) }

	Member getLastMember(){ members.max { it.joinDate } }
	Member getLatestMember(){ members.max { it.joinDate } }
	int getMemberCount(){ object["member_count"] }

	List<Emoji> getEmojis(){ object["emojis"].list }
	List<Emoji> getEmoji(){ emojis }
	Map<String, Emoji> getEmojiIdMap(){ object["emojis"].map }
	Map<String, Emoji> getEmojiNameMap(){
		object["emojis"].values().collectEntries { [(it.name): new Emoji(client, it)] }
	}

	List<Emoji> requestEmojis(){ client.requestEmojis(this) }

	Emoji createEmoji(Map data){
		client.createEmoji(data, this)
	}

	Emoji editEmoji(Map data, emoji){
		client.editEmoji(data, this, emoji)
	}

	Member member(user){ find(object.members, user) }

	Message sendMessage(String message, boolean tts=false){ sendMessage(content: message, tts: tts) }
	Message sendMessage(Map data){ client.sendMessage(data, this) }
	Message sendFile(...args){ client.sendFile(this, *args) }
	Message sendFile(Map data, ...args){ client.sendFile(data, this, *args) }

	Embed requestEmbed(){ client.requestEmbed(this) }

	static Map construct(Client client, Map g){
		g["members"] = new DiscordListCache(g.members.collect { it << [guild_id: g["id"]] << it["user"] }, client, Member)
		g["presences"] = new DiscordListCache(g.presences.collect { it << [guild_id: g["id"]] << it["user"] }, client, Presence)
		g["emojis"] = new DiscordListCache(g.emojis.collect { it << [guild_id: g["id"]] }, client, Emoji)
		g["roles"] = new DiscordListCache(g.roles.collect { it << [guild_id: g["id"]] }, client, Role)
		g["channels"] = new DiscordListCache(g.channels.collect { Channel.construct(client, it, g["id"]) }, client, Channel)
		g["voice_states"] = new DiscordListCache(g.voice_states.collect { it << [guild_id: g["id"], id: it["user_id"]] }, client, VoiceState)
		g
	}

	@InheritConstructors
	static class Embed extends DiscordObject {
		String getId(){ object["channel_id"] }
		String getName(){ channel.name }
		boolean isEnabled(){ object["enabled"] }
		Channel getChannel(){ client.channel(object["channel_id"]) }
		Server getServer(){ channel.server }
		Embed edit(Map data){
			client.editEmbed(data, server, this)
		}
	}

	static class Ban extends User {
		Ban(Client client, Map object){ super(client, object + object.user) }

		User getUser(){ new User(client, object.user) }
		String getReason(){ object.reason }
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
	Integration(Client client, Map object){ super(client, object) }

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
	String getRawSyncedAt(){ object["synced_at"] }
	Date getSyncedAt(){ ConversionUtil.fromJsonDate(object["synced_at"]) }
	String getType(){ object["type"] }
	Integration edit(Map data){ client.editIntegration(data, server, this) }
	void delete(){ client.deleteIntegration(server, this) }
	void sync(){ client.syncIntegration(server, this) }
}

@InheritConstructors
class Emoji extends DiscordObject {
	String getServerId(){ object.guild_id }
	Server getServer(){ client.server(serverId) }
	Server getParent(){ server }
	List<Role> getRoles(){ object.roles.collect { server.role(it) } }
	boolean requiresColons(){ object["require_colons"] }
	boolean requireColons(){ object["require_colons"] }
	boolean isRequiresColons(){ object["require_colons"] }
	boolean isRequireColons(){ object["require_colons"] }
	boolean isManaged(){ object["managed"] }
	String getUrl(){ "https://cdn.discordapp.com/emojis/${id}.png" }
	InputStream getInputStream(){ inputStreamFromDiscord(url) }
	File download(file){ downloadFileFromDiscord(url, file) }
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
		server.member(user).owner ? false :
			position >= server.member(user).primaryRole.position
	}
	boolean isHoist(){ object["hoist"] }
	boolean isManaged(){ object["managed"] }
	boolean isMentionable(){ object["mentionable"] }
	Permissions getPermissions(){ new Permissions(object["permissions"]) }
	int getPermissionValue(){ object["permissions"] }
	int getPosition(){ object["position"] }

	Server getServer(){ client.server(serverId) }
	String getServerId(){ object.guild_id }
	Server getParent(){ server }

	String getMention(){ "<@&${id}>" }
	String getMentionRegex(){ MENTION_REGEX(id) }

	List<Member> getMembers(){
		client.cache.guilds[object.guild_id].members.values()
			.findAll { it.roles.contains(id) }
			.collect { new Member(client, it) }
	}
	boolean isUsed(){ server.usedRoleIds.contains(id) }
	Role edit(Map data){ client.editRole(data, serverId, this) }
	void delete(){ client.deleteRole(serverId, this) }
	void addTo(user){ client.addRole(serverId, user, this) }
	void addTo(Collection users){ users.each(this.&addTo) }
	void removeFrom(user){ client.removeRole(serverId, user, this) }
	void removeFrom(Collection users){ users.each(this.&removeFrom) }
}

class Member extends User {
	Member(Client client, Map object){
		super(client, object + object["user"])
	}

	User getUser(){ new User(client, object["user"]) }
	String getNick(){ object["nick"] ?: name }
	String getRawNick(){ object["nick"] }
	String getServerId(){ object.guild_id }
	Server getServer(){ client.server(object["guild_id"]) }
	Server getParent(){ server }
	String getRawJoinedAt(){ object["joined_at"] }
	Date getJoinedAt(){ ConversionUtil.fromJsonDate(rawJoinedAt) }
	List<String> getRoleIds(){ object["roles"] }
	List<Role> getRoles(){
		object["roles"].collect { server.role(it) }
	}

	Role role(ass){ find(server.object.roles, client, ass) }

	boolean isOwner(){ server.ownerId == id }

	Game getGame(){
		presence?.game ?: null
	}

	Presence getPresence(){
		server.presence(id)
	}

	void edit(Map data){
		client.editMember(data, serverId, this)
	}

	void changeNick(String newNick){
		id == client.cache["user"]["id"] ? client.changeOwnNick(serverId, newNick) :
			edit(nick: newNick)
	}
	void nick(String newNick){ changeNick(newNick) }
	void editNick(String newNick){ changeNick(newNick) }
	void resetNick(){ changeNick("") }

	void mute(){ edit(mute: true) }
	void unmute(){ edit(mute: false) }
	void deafen(){ edit(deaf: true) }
	void undeafen(){ edit(deaf: false) }
	void ban(int days = 0){ client.ban(serverId, this, days) }
	void unban(){ client.unban(serverId, this) }
	boolean isMute(){ object["mute"] }
	boolean isDeaf(){ object["deaf"] }
	boolean isDeafened(){ object["deaf"] }

	String getStatus(){
		presence?.status ?: "offline"
	}

	Role getPrimaryRole(){ roles.max { it.position } }
	int getColorValue(){ roles.findAll { it.colorValue != 0 }.max { it.position }?.colorValue ?: 0 }
	Color getColor(){ new Color(colorValue) }
	Permissions getPermissions(){
		if (owner) return Permissions.ALL_TRUE
		Permissions full = server.defaultRole.permissions
		for (Permissions perms in roles*.permissions){
			if (perms["administrator"]){
				return Permissions.ALL_TRUE
			}else{
				full += perms
			}
		}
		full
	}

	void editRoles(List roles) {
		client.editRoles(serverId, this, roles)
	}

	void addRoles(List roles) {
		client.addRoles(serverId, this, roles)
	}

	void addRole(role){
		client.addRole(serverId, this, role)
	}

	void removeRole(role){
		client.removeRole(serverId, this, role)
	}

	void kick() {
		client.kick(serverId, this)
	}

	void moveTo(channel){
		client.moveMemberVoiceChannel(serverId, this, channel)
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
	boolean isFromServer(){ object.guild_id }
	Server getParent(){ server }
	Member getMember(){ server ? server.memberMap[id] : client.members(id)[0] }
	String getName(){ member.name }
	long getLastModified(){ object.last_modified }
}

@InheritConstructors
class Game extends DiscordObject {
	String getId(){ object["type"].toString() ?: "0" }
	int getType(){ object["type"] ?: 0 }
	String getUrl(){ object["url"] }
	String toString(){ type == 0 ? name : "$name ($url)" }
}