package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.MFALevelTypes
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.collections.DiscordListCache
import hlaaftana.discordg.util.*
import java.awt.Color

import groovy.transform.InheritConstructors

import java.util.regex.Pattern

/**
 * A Discord guild/guild.
 * @author Hlaaftana
 */
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Guild extends DiscordObject {
	Guild(Client client, Map object) {
		super(client, object)
	}

	String getRegionId() { (String) object.region }
	String getRawJoinedAt() { (String) object.joined_at }
	Date getJoinedAt() { ConversionUtil.fromJsonDate(rawJoinedAt) }
	String getIconHash() { (String) object.icon }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/icons/$id/${iconHash}.jpg" : ''
	}
	boolean hasIcon() { object.icon }
	InputStream newIconInputStream() { inputStreamFromDiscord(icon) }
	File downloadIcon(file) { downloadFileFromDiscord(icon, file) }

	String getOwnerId() { (String) object.owner_id }
	Member getOwner() { member(ownerId) }

	Member getMe() { member(client) }
	String changeNick(String newNick) { client.changeOwnGuildNick(id, newNick) }
	String nick(String newNick) { changeNick(newNick) }
	String editNick(String newNick) { changeNick(newNick) }
	String resetNick() { changeNick('') }

	Channel getDefaultChannel() { channel(id) }
	Channel getAfkChannel() { channel(object.afk_channel_id) }
	int getAfkTimeout() { (int) object.afk_timeout }
	Channel getWidgetChannel() { channel(object.embed_channel_id) }
	boolean isWidgetEnabled() { (boolean) object.embed_enabled }
	boolean isLarge() { (boolean) object.large }
	boolean isUnavailable() { (boolean) object.unavailable }
	int getVerificationLevel() { (int) object.verification_level }
	int getMfaLevel() { (int) object.mfa_level }
	boolean isMfaRequiredForStaff() { mfaLevel == MFALevelTypes.ELEVATED }

	Role getDefaultRole() { role(id) }

	Guild edit(Map data) {
		client.editGuild(data, id)
	}

	void leave() {
		client.leaveGuild(id)
	}

	void delete() {
		client.deleteGuild(id)
	}

	Channel createTextChannel(String name) {
		client.createTextChannel(this, name)
	}

	Channel createVoiceChannel(String name) {
		client.createVoiceChannel(this, name)
	}
	
	Channel createChannel(Map data = [:]) {
		client.createChannel(data, this)
	}

	Channel requestChannel(c) {
		client.requestGuildChannel(this, c)
	}

	List<Channel> requestChannels() {
		client.requestGuildChannels(this)
	}

	List<Channel> getTextChannels() { channels.findAll { it.text } }
	Map<String, Channel> getTextChannelMap() { channelMap.findAll { k, v -> v.text } }

	List<Channel> getVoiceChannels() { channels.findAll { it.voice } }
	Map<String, Channel> getVoiceChannelMap() { channelMap.findAll { k, v -> v.voice } }

	Channel textChannel(id) { findBuilt(textChannels, id) }

	Channel voiceChannel(id) { findBuilt(voiceChannels, id) }

	Channel channel(id) { find(channelCache, id) }

	DiscordListCache<Channel> getChannelCache() { (DiscordListCache<Channel>) object.channels }
	List<Channel> getChannels() { channelCache.list() }
	Map<String, Channel> getChannelMap() { channelCache.map() }

	DiscordListCache<Role> getRoleCache() { (DiscordListCache<Role>) object.roles }
	List<Role> getRoles() { roleCache.list() }
	Map<String, Role> getRoleMap() { roleCache.map() }
	Set<String> getUsedRoleIds() {
		def res = new HashSet()
		for (e in memberCache) res.addAll((List<String>) e.value.roles)
		res
	}

	Role role(ass) { find(roleCache, ass) }

	DiscordListCache<Member> getMemberCache() { (DiscordListCache<Member>) object.members }
	List<Member> getMembers() { memberCache.list() }
	Map<String, Member> getMemberMap() { memberCache.map() }

	DiscordListCache<Presence> getPresenceCache() { (DiscordListCache<Presence>) object.presences }
	List<Presence> getPresences() { presenceCache.list() }
	Map<String, Presence> getPresenceMap() { presenceCache.map() }

	Presence presence(ass) { find(presenceCache, ass) }

	void editRoles(member, List roles) {
		client.editRoles(this, member, roles)
	}

	void addRoles(member, List roles) {
		client.addRoles(this, member, roles)
	}

	void addRole(member, role) {
		client.addRole(this, member, role)
	}

	void removeRole(member, role) {
		client.removeRole(this, member, role)
	}

	void kick(member) {
		client.kick(this, member)
	}

	List<Ban> requestBans() {
		client.requestBans(this)
	}
	
	DiscordListCache<VoiceState> getVoiceStateCache() { (DiscordListCache<VoiceState>) object.voice_states }
	
	List<VoiceState> getVoiceStates() {
		voiceStateCache.list()
	}

	Map<String, VoiceState> getVoiceStateMap() {
		voiceStateCache.map()
	}

	List<Invite> requestInvites() {
		client.requestGuildInvites(this)
	}

	List<Region> requestRegions() {
		client.requestRegions(this)
	}

	List<Integration> requestIntegrations() {
		client.requestIntegrations(this)
	}

	Integration createIntegration(String type, String id) {
		client.createIntegration(this, type, id)
	}

	String getRegion() { (String) object.region }

	void ban(user, int days=0) {
		client.ban(this, user, days)
	}

	void unban(user) {
		client.unban(this, user)
	}

	int checkPrune(int days) {
		client.checkPrune(this, days)
	}

	int prune(int days) {
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

	List<Webhook> requestWebhooks() {
		client.requestGuildWebhooks(this)
	}
	
	List<Role> editRolePositions(Map mods) {
		client.editRolePositions(mods, this)
	}

	List<Role> moveRole(ro, int movement) {
		Role r = role(ro)
		def rp = Math.max(r.position + movement, 0)
		def rg = rp..<r.position
		def dif = rp <=> r.position
		Map x = [:]
		roles.findAll { it.position in rg }.each { x[it.id] = it.position + dif }
		editRolePositions(x)
	}

	List<Channel> moveChannel(chan, int movement) {
		Channel c = channel(chan)
		def cp = Math.max(c.position + movement, 0)
		def cg = cp..<c.position
		def dif = cp <=> c.position
		Map x = [:]
		channels.findAll { it.position in cg }.each { x[it.id] = it.position + dif }
		editChannelPositions(x)
	}
	
	List<Channel> editChannelPositions(Map mods) {
		client.editChannelPositions(mods, this)
	}

	List<Member> requestMembers(int max=1000, boolean updateCache=true) {
		client.requestMembers(this, max, updateCache)
	}

	Member requestMember(id) { client.requestMember(this, id) }

	Member getLastMember() { members.max { it.joinedAt } }
	Member getLatestMember() { members.max { it.joinedAt } }
	int getMemberCount() { (int) object.member_count }

	DiscordListCache<Emoji> getEmojiCache() { (DiscordListCache<Emoji>) object.emojis }
	List<Emoji> getEmojis() { emojiCache.list() }
	List<Emoji> getEmoji() { emojis }
	Map<String, Emoji> getEmojiIdMap() { emojiCache.map() }
	Map<String, Emoji> getEmojiNameMap() {
		def res = new HashMap<String, Emoji>()
		for (e in emojiCache) res.put((String) e.value.name, new Emoji(client, e.value))
		res
	}

	List<Emoji> requestEmojis() { client.requestEmojis(this) }

	Emoji createEmoji(Map data) {
		client.createEmoji(data, this)
	}

	Emoji editEmoji(Map data, emoji) {
		client.editEmoji(data, this, emoji)
	}

	Member member(user) { findMember(memberCache, user) }

	Message sendMessage(String message, boolean tts=false) { sendMessage(content: message, tts: tts) }
	Message sendMessage(Map data) { client.sendMessage(data, this) }
	Message sendFile(...args) { (Message) client.invokeMethod('sendFile', [this, *args]) }
	Message sendFile(Map data, ...args) { (Message) client.invokeMethod('sendFile', [data, this, *args]) }

	Embed requestEmbed() { client.requestEmbed(this) }

	static Map construct(Client client, Map g) {
		def gid = (String) g.id

		def m = (List<Map>) g.members, ml = new ArrayList<Map>(m.size())
		for (mo in m) {
			def a = new HashMap(mo)
			a.put('guild_id', gid)
			a.putAll((Map) a.user)
			ml.add(a)
		}
		g.members = new DiscordListCache(ml, client, Member)

		def p = (List<Map>) g.presences, pl = new ArrayList<Map>(p.size())
		for (po in p) {
			def a = new HashMap(po)
			a.put('guild_id', gid)
			a.putAll((Map) a.user)
			pl.add(a)
		}
		g.presences = new DiscordListCache(pl, client, Presence)

		def e = (List<Map>) g.emojis, el = new ArrayList<Map>(e.size())
		for (eo in e) {
			def a = new HashMap(eo)
			a.put('guild_id', gid)
			el.add(a)
		}
		g.emojis = new DiscordListCache(el, client, Emoji)

		def r = (List<Map>) g.roles, rl = new ArrayList<Map>(r.size())
		for (ro in r) {
			def a = new HashMap(ro)
			a.put('guild_id', gid)
			rl.add(a)
		}
		g.roles = new DiscordListCache(rl, client, Role)

		def c = (List<Map>) g.channels, cl = new ArrayList<Map>(c.size())
		for (co in c) cl.add(Channel.construct(client, new HashMap(co), gid))
		g.channels = new DiscordListCache(cl, client, Channel)

		def vs = (List<Map>) g.voice_states, vsl = new ArrayList<Map>(vs.size())
		for (vso in vs) {
			def a = new HashMap(vso)
			a.put('guild_id', gid)
			a.put('id', (String) a.user_id)
			vsl.add(a)
		}
		g.voice_states = new DiscordListCache(vsl, client, VoiceState)

		g
	}

	@InheritConstructors
	static class Embed extends DiscordObject {
		String getId() { (String) object.channel_id }
		String getName() { channel.name }
		boolean isEnabled() { (boolean) object.enabled }
		Channel getChannel() { client.channel(id) }
		Guild getGuild() { channel.guild }
		Embed edit(Map data) {
			client.editEmbed(data, guild, this)
		}
	}

	static class Ban extends User {
		Ban(Client client, Map object) { super(client, object + (Map) object.user) }

		User getUser() { new User(client, (Map) object.user) }
		String getReason() { (String) object.reason }
	}
}

@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class VoiceState extends DiscordObject {
	VoiceState(Client client, Map object) { super(client, object) }

	String getGuildId() { (String) object.guild_id }
	String getChannelId() { (String) object.channel_id }
	String getUserId() { (String) object.user_id }
	Channel getChannel() { client.channel(channelId) }
	User getUser() { client.user(userId) }
	Guild getGuild() { guildId ? client.guild(guildId) : channel.guild }
	Channel getParent() { channel }
	Member getMember() { guild.member(user) }
	boolean isDeaf() { (boolean) object.deaf }
	boolean isMute() { (boolean) object.mute }
	boolean isDeafened() { deaf }
	boolean isMuted() { mute }
	boolean isSelfDeaf() { (boolean) object.self_deaf }
	boolean isSelfMute() { (boolean) object.self_mute }
	boolean isSelfDeafened() { selfDeaf }
	boolean isSelfMuted() { selfMute }
	boolean isSuppress() { (boolean) object.suppress }
	String getToken() { (String) object.token }
	String getSessionId() { (String) object.session_id }
	String getName() { user.name }
}

@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Integration extends DiscordObject {
	Integration(Client client, Map object) { super(client, object) }

	int getSubscriberCount() { (int) object.subscriber_count }
	boolean isSyncing() { (boolean) object.syncing }
	boolean isEnableEmoticons() { (boolean) object.enable_emoticons }
	int getExpireBehaviour() { (int) object.expire_behaviour }
	int getExpireGracePeriod() { (int) object.expire_grace_period }
	User getUser() { new User(client, (Map) object.user) }
	DiscordObject getAccount() { new DiscordObject(client, (Map) object.account) }
	boolean isEnabled() { (boolean) object.enabled }
	String getRoleId() { (String) object.role_id }
	Role getRole() { client.role(roleId) }
	Guild getGuild() { role.guild }
	String getRawSyncedAt() { (String) object.synced_at }
	Date getSyncedAt() { ConversionUtil.fromJsonDate(rawSyncedAt) }
	String getType() { (String) object.type }
	Integration edit(Map data) { client.editIntegration(data, guild, this) }
	void delete() { client.deleteIntegration(guild, this) }
	void sync() { client.syncIntegration(guild, this) }
}

@InheritConstructors
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Emoji extends DiscordObject {
	static final Pattern REGEX = ~/<:(?<name>\w+):(?<id>\d+)>/

	String getGuildId() { (String) object.guild_id }
	Guild getGuild() { client.guild(guildId) }
	Guild getParent() { guild }
	List<String> getRoleIds() { (List<String>) object.roles }
	List<Role> getRoles() { roleIds.collect(guild.&role) }
	boolean requiresColons() { (boolean) object.require_colons }
	boolean requireColons() { (boolean) object.require_colons }
	boolean isRequiresColons() { (boolean) object.require_colons }
	boolean isRequireColons() { (boolean) object.require_colons }
	boolean isManaged() { (boolean) object.managed }
	String getUrl() { "https://cdn.discordapp.com/emojis/${id}.png" }
	InputStream newInputStream() { inputStreamFromDiscord(url) }
	File download(file) { downloadFileFromDiscord(url, file) }
}

@InheritConstructors
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
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
	static final Closure MENTION_REGEX = { String id = /\d+/ -> /<@&$id>/ }

	int getColorValue() { (int) object.color }
	Color getColor() { new Color(colorValue) }
	boolean isLocked() { isLockedFor(guild.me) }
	boolean isLockedFor(user) {
		guild.member(user).owner ? false :
			position >= guild.member(user).primaryRole.position
	}
	boolean isHoist() { (boolean) object.hoist }
	boolean isManaged() { (boolean) object.managed }
	boolean isMentionable() { (boolean) object.mentionable }
	Permissions getPermissions() { new Permissions(permissionValue) }
	int getPermissionValue() { (int) object.permissions }
	int getPosition() { (int) object.position }

	Guild getGuild() { client.guild(guildId) }
	String getGuildId() { (String) object.guild_id }
	Guild getParent() { guild }

	String getMention() { "<@&${id}>" }
	String getMentionRegex() { MENTION_REGEX(id) }

	List<Member> getMembers() {
		def x = ((DiscordListCache<Member>) client.guildCache[guildId].members).values()
		def r = []
		for (a in x) if (((List<String>) a.roles).contains(id)) r.add(new Member(client, a))
		r
	}

	List<String> getMemberIds() {
		def x = ((DiscordListCache<Member>) client.guildCache[guildId].members).values()
		def r = []
		for (a in x) if (((List<String>) a.roles).contains(id)) r.add(a.id)
		r
	}

	List<PermissionOverwrite> getPermissionOverwrites() {
		findAllNested((DiscordListCache<Channel>) client.guildCache[guildId].channels,
			'permission_overwrites', id)
	}
	List<PermissionOverwrite> getOverwrites() { permissionOverwrites }

	boolean isUsed() { guild.usedRoleIds.contains(id) }
	Role edit(Map data) { client.editRole(data, guildId, this) }
	void delete() { client.deleteRole(guildId, this) }
	void addTo(user) { client.addRole(guildId, user, this) }
	void addTo(Collection users) { users.each(this.&addTo) }
	void removeFrom(user) { client.removeRole(guildId, user, this) }
	void removeFrom(Collection users) { users.each(this.&removeFrom) }
	def move(int movement) { guild.moveRole(this, movement) }
}

@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Member extends User {
	Member(Client client, Map object) {
		super(client, object)
		if (object.user instanceof Map) object << ((Map) object.user)
		else if (object.user instanceof User) {
			object << (object.user = ((User) object.user).object)
		}
	}

	User getUser() { new User(client, (Map) object.user) }
	String getNick() { (String) object.nick ?: name }
	String getRawNick() { (String) object.nick }
	String getGuildId() { (String) object.guild_id }
	Guild getGuild() { client.guild(object.guild_id) }
	Guild getParent() { guild }
	String getRawJoinedAt() { (String) object.joined_at }
	Date getJoinedAt() { ConversionUtil.fromJsonDate(rawJoinedAt) }
	List<String> getRoleIds() { (List<String>) object.roles }
	List<Role> getRoles() {
		DiscordListCache<Role> x = (DiscordListCache<Role>) client.guildCache[guildId].roles
		def r = []
		for (ri in roleIds) r.add(x.at(ri))
		r
	}

	boolean isOwner() { guild.ownerId == id }

	Game getGame() {
		presence?.game ?: null
	}

	Presence getPresence() {
		guild.presence(id)
	}

	void edit(Map data) {
		client.editMember(data, guildId, this)
	}

	void changeNick(String newNick) {
		id == client.id ? client.changeOwnGuildNick(guildId, newNick) :
			edit(nick: newNick)
	}
	void nick(String newNick) { changeNick(newNick) }
	void editNick(String newNick) { changeNick(newNick) }
	void resetNick() { changeNick('') }

	void mute() { edit(mute: true) }
	void unmute() { edit(mute: false) }
	void deafen() { edit(deaf: true) }
	void undeafen() { edit(deaf: false) }
	void ban(int days = 0) { client.ban(guildId, this, days) }
	void unban() { client.unban(guildId, this) }
	boolean isMute() { (boolean) object.mute }
	boolean isDeaf() { (boolean) object.deaf }
	boolean isDeafened() { (boolean) object.deaf }

	String getStatus() {
		presence?.status ?: 'offline'
	}

	Role getPrimaryRole() { roles.max { it.position } }
	int getColorValue() { roles.findAll { it.colorValue != 0 }.max { it.position }?.colorValue ?: 0 }
	Color getColor() { new Color(colorValue) }
	Permissions getPermissions() {
		if (owner) return Permissions.ALL_TRUE
		int full = guild.defaultRole.permissionValue
		for (role in roles) {
			def perms = role.permissionValue
			if (((perms >> 3) & 1) == 1) return Permissions.ALL_TRUE
			full |= perms
		}
		new Permissions(full)
	}

	void editRoles(List roles) {
		client.editRoles(guildId, this, roles)
	}

	void addRoles(List roles) {
		client.addRoles(guildId, this, roles)
	}

	void addRole(role) {
		client.addRole(guildId, this, role)
	}

	void removeRole(role) {
		client.removeRole(guildId, this, role)
	}

	void kick() {
		client.kick(guildId, this)
	}

	void moveTo(channel) {
		client.moveMemberVoiceChannel(guildId, this, channel)
	}

	List<PermissionOverwrite> getPermissionOverwrites() {
		findAllNested((DiscordListCache<Channel>) client.guildCache[guildId].channels,
			'permission_overwrites', id)
	}
	List<PermissionOverwrite> getOverwrites() { permissionOverwrites }

	boolean isSuperior() {
		roles*.position.max() > guild.me.roles*.position.max()
	}
	
	boolean isSuperiorTo(user) {
		roles*.position.max() > guild.member(user).roles*.position.max()
	}
	
	User toUser() { new User(client, (Map) object.user) }
	def asType(Class target) {
		if (target == User) toUser()
		else super.asType(target)
	}
	String toString() { nick }
}

@InheritConstructors
@CompileStatic
class Region extends DiscordObject {
	String getSampleHostname() { (String) object.sample_hostname }
	int getSamplePort() { (int) object.sample_port }
	boolean isVip() { (boolean) object.vip }
	boolean isOptimal() { (boolean) object.optimal }
	boolean isDeprecated() { (boolean) object.deprecated }
	boolean isCustom() { (boolean) object.custom }
}

@InheritConstructors
@CompileStatic
class Presence extends DiscordObject {
	Game getGame() { null != object.game ? new Game(client, (Map) object.game) : null }
	String getStatus() { (String) object.status }
	Guild getGuild() { client.guildCache.at(guildId) }
	String getGuildId() { (String) object.guild_id }
	boolean isFromGuild() { null != guildId }
	Guild getParent() { guild }
	Member getMember() { guild ? guild.memberCache.at(id) : client.members(id)[0] }
	String getName() { member.name }
	long getLastModified() { (long) object.last_modified }
}

@InheritConstructors
@CompileStatic
class Game extends DiscordObject {
	String getId() { type.toString() }
	int getType() { null == object.type ? 0 : (int) object.type }
	String getUrl() { (String) object.url }
	String toString() { type == 0 ? name : "$name ($url)" }
}