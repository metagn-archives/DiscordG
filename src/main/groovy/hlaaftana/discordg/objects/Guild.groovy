package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.MFALevelTypes
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.Snowflake
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

	Channel getDefaultChannel() { channel(id) ?: channels[0] }
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
	Map<Snowflake, Channel> getTextChannelMap() { channelMap.findAll { k, v -> v.text } }

	List<Channel> getVoiceChannels() { channels.findAll { it.voice } }
	Map<Snowflake, Channel> getVoiceChannelMap() { channelMap.findAll { k, v -> v.voice } }

	Channel textChannel(id) { findBuilt(textChannels, id) }

	Channel voiceChannel(id) { findBuilt(voiceChannels, id) }

	Channel channel(id) { find(channelCache, id) }

	DiscordListCache<Channel> getChannelCache() { (DiscordListCache<Channel>) object.channels }
	List<Channel> getChannels() { channelCache.list() }
	Map<Snowflake, Channel> getChannelMap() { channelCache.map() }

	DiscordListCache<Role> getRoleCache() { (DiscordListCache<Role>) object.roles }
	List<Role> getRoles() { roleCache.list() }
	Map<Snowflake, Role> getRoleMap() { roleCache.map() }
	Set<Snowflake> getUsedRoleIds() {
		def res = new HashSet()
		for (e in memberCache) res.addAll(e.value.roleIds)
		res
	}

	Role role(ass) { find(roleCache, ass) }

	DiscordListCache<Member> getMemberCache() { (DiscordListCache<Member>) object.members }
	List<Member> getMembers() { memberCache.list() }
	Map<Snowflake, Member> getMemberMap() { memberCache.map() }

	DiscordListCache<Presence> getPresenceCache() { (DiscordListCache<Presence>) object.presences }
	List<Presence> getPresences() { presenceCache.list() }
	Map<Snowflake, Presence> getPresenceMap() { presenceCache.map() }

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

	Map<Snowflake, VoiceState> getVoiceStateMap() {
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
	Map<Snowflake, Emoji> getEmojiIdMap() { emojiCache.map() }
	Map<String, Emoji> getEmojiNameMap() {
		def res = new HashMap<String, Emoji>()
		for (e in emojiCache) res.put(e.value.name, e.value)
		res
	}

	List<Emoji> requestEmojis() { client.requestEmojis(this) }

	Emoji createEmoji(Map data) {
		client.createEmoji(data, this)
	}

	Emoji editEmoji(Map data, emoji) {
		client.editEmoji(data, this, emoji)
	}

	Member member(user) { find(memberCache, user) }

	Message sendMessage(String message, boolean tts=false) { sendMessage(content: message, tts: tts) }
	Message sendMessage(Map data) { client.sendMessage(data, this) }
	Message sendFile(...args) {
		def x = new Object[args.length + 1]
		x[0] = this
		System.arraycopy(args, 0, x, 1, args.length)
		(Message) client.invokeMethod('sendFile', x)
	}
	Message sendFile(Map data, ...args) {
		def x = new Object[args.length + 2]
		x[0] = data
		x[1] = this
		System.arraycopy(args, 0, x, 2, args.length)
		(Message) client.invokeMethod('sendFile', x)
	}

	Embed requestEmbed() { client.requestEmbed(this) }

	@InheritConstructors
	static class Embed extends DiscordObject {
		Snowflake channelId
		boolean enabled

		Snowflake getId() { channelId }
		String getName() { channel.name }
		Channel getChannel() { client.channel(channelId) }
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
	static final Pattern REGEX = ~/<:(?<name>\w+):(?<from>\d+)>/

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
class Role extends DiscordObject {
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

	Snowflake id, guildId
	String name
	int colorValue, permissionValue, position
	boolean hoist, managed, mentionable

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			color: 1, permissions: 2, position: 3, guild_id: 4, id: 5,
			hoist: 6, managed: 7, mentionable: 8, name: 9)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			colorValue = (int) value
		} else if (f == 2) {
			permissionValue = (int) value
		} else if (f == 3) {
			position = (int) value
		} else if (f == 4) {
			guildId = Snowflake.swornString(value)
		} else if (f == 5) {
			id = Snowflake.swornString(value)
		} else if (f == 6) {
			hoist = (boolean) value
		} else if (f == 7) {
			managed = (boolean) value
		} else if (f == 8) {
			mentionable = (boolean) value
		} else if (f == 9) {
			name = (String) value
		} else println("Unknown field number $field for ${this.class}")
	}

	Color getColor() { new Color(colorValue) }
	boolean isLocked() { isLockedFor(guild.me) }
	boolean isLockedFor(user) {
		guild.member(user).owner ? false :
			position >= guild.member(user).primaryRole.position
	}
	Permissions getPermissions() { new Permissions(permissionValue) }

	Guild getGuild() { client.guildCache[guildId] }
	Guild getParent() { guild }

	String getMention() { "<@&$id>" }
	String getMentionRegex() { MENTION_REGEX(id.toString()) }

	List<Member> getMembers() {
		def x = guild.memberCache.values()
		def r = []
		for (a in x) if (a.roleIds.contains(id)) r.add(a)
		r
	}

	List<Snowflake> getMemberIds() {
		def x = client.guildCache[guildId].memberCache.values()
		def r = []
		for (a in x) if (a.roleIds.contains(id)) r.add(a.id)
		r
	}

	List<PermissionOverwrite> getPermissionOverwrites() {
		findAllNested(client.guildCache[guildId].channelCache,
				'permissionOverwriteCache', id)
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
@InheritConstructors
@SuppressWarnings('GroovyUnusedDeclaration')
class Member extends DiscordObject {
	Snowflake guildId
	List<Snowflake> roleIds
	@Delegate User user
	String rawNick, rawJoinedAt
	boolean mute, deaf

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			user: 1, roles: 2, nick: 3, guild_id: 4, joined_at: 5,
			mute: 6, deaf: 7)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			final map = (Map) value
			if (null != user) user.fill map
			else user = new User(client, map)
		} else if (f == 2) {
			final raw = (List<String>) value
			roleIds = new ArrayList<>(raw.size())
			for (i in raw) roleIds.add(new Snowflake(i))
		} else if (f == 3) {
			rawNick = (String) value
		} else if (f == 4) {
			guildId = Snowflake.swornString(value)
		} else if (f == 5) {
			rawJoinedAt = (String) value
		} else if (f == 6) {
			mute = (boolean) value
		} else if (f == 7) {
			deaf = (boolean) value
		} else println("Unknown field number $field for ${this.class}")
	}

	String getNick() { rawNick ?: name }
	Guild getGuild() { client.guildCache[guildId] }
	Guild getParent() { guild }
	Date getJoinedAt() { ConversionUtil.fromJsonDate(rawJoinedAt) }
	List<Role> getRoles() {
		def x = guild.roleCache
		def r = []
		for (ri in roleIds) r.add(x.get(ri))
		r
	}

	boolean isOwner() { guild.ownerId == id }

	Game getGame() { presence?.game ?: null }
	Presence getPresence() { guild.presenceCache[id] }

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

	boolean isDeafened() { deaf }
	String getStatus() { presence?.status ?: 'offline' }

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
		findAllNested(client.guildCache[guildId].channelCache,
			'permissionOverwriteCache', id)
	}
	List<PermissionOverwrite> getOverwrites() { permissionOverwrites }

	boolean isSuperior() {
		roles*.position.max() > guild.me.roles*.position.max()
	}
	
	boolean isSuperiorTo(user) {
		roles*.position.max() > guild.member(user).roles*.position.max()
	}
	
	User toUser() { user }
	def asType(Class target) {
		if (target == User) toUser()
		else super.asType(target)
	}
	String toString() { nick }
}

@CompileStatic
class Region {
	String id, name
	boolean vip, optimal, deprecated, custom

	Region(Map map) {
		id = (String) map.id
		name = (String) map.name
		vip = (boolean) map.vip
		optimal = (boolean) map.optimal
		deprecated = (boolean) map.deprecated
		custom = (boolean) map.custom
	}
}

@InheritConstructors
@CompileStatic
class Presence extends DiscordObject {
	Snowflake guildId
	String status
	Game game
	User user
	long lastModified

	private static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			guild_id: 1, status: 2, game: 3, user: 4, last_modified: 5)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			guildId = Snowflake.swornString(value)
		} else if (f == 2) {
			status = (String) value
		} else if (f == 3) {
			game = new Game(client, (Map) value)
		} else if (f == 4) {
			if (null == user) user = new User(client)
			user.fill((Map) value)
		} else if (f == 5) {
			lastModified = (long) value
		} else println("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { user.id }
	String getName() { user.name }
	Guild getGuild() { client.guildCache.get(guildId) }
	boolean isFromGuild() { null != guildId }
	Guild getParent() { guild }
	Member getMember() { guild ? guild.memberCache.get(id) : client.members(id)[0] }
}

@InheritConstructors
@CompileStatic
class Game extends DiscordObject {
	String name, url
	int type

	private static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			name: 1, url: 2, type: 3)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			name = (String) value
		} else if (f == 2) {
			url = (String) value
		} else if (f == 3) {
			type = (int) value
		} else println("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { new Snowflake((long) type) }
	String toString() { type == 0 ? name : "$name ($url)" }
}