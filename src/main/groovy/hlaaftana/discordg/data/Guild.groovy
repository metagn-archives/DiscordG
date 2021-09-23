package hlaaftana.discordg.data

import groovy.transform.CompileStatic
import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.util.*
import java.awt.Color

import groovy.transform.InheritConstructors

import java.util.regex.Pattern

/**
 * A Discord guild/guild.
 * @author Hlaaftana
 */
@CompileStatic
@InheritConstructors
@SuppressWarnings('GroovyUnusedDeclaration')
class Guild extends DiscordObject {
	Snowflake id, ownerId, afkChannelId, embedChannelId, systemChannelId, ownerApplicationId
	String name, regionId, rawJoinedAt, iconHash, splashHash
	int afkTimeout, verificationLevel, mfaLevel, memberCount, defaultMessageNotificationLevel, explicitContentFilterLevel
	boolean embedEnabled, large, unavailable, ownerClient, lazy
	List<String> features
	Cache<Channel> channelCache
	Cache<Role> roleCache
	Cache<Member> memberCache
	Cache<Presence> presenceCache
	Cache<VoiceState> voiceStateCache
	Cache<Emoji> emojiCache

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, owner_id: 2, afk_channel_id: 3, widget_channel_id: 4, region: 5, joined_at: 6,
			icon: 7, splash: 8, name: 9, system_channel_id: 10, application_id: 11,
			afk_timeout: 12, verification_level: 13, mfa_level: 14, member_count: 15,
			default_message_notifications: 16, explicit_content_filter: 17, widget_enabled: 18,
			large: 19, unavailable: 20, owner: 21, features: 22, channels: 23, roles: 24, members: 25,
			presences: 26, voice_states: 27, emojis: 28, lazy: 29)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			ownerId = Snowflake.swornString(value)
		} else if (f == 3) {
			afkChannelId = Snowflake.swornString(value)
		} else if (f == 4) {
			embedChannelId = Snowflake.swornString(value)
		} else if (f == 5) {
			regionId = (String) value
		} else if (f == 6) {
			rawJoinedAt = (String) value
		} else if (f == 7) {
			iconHash = (String) value
		} else if (f == 8) {
			splashHash = (String) value
		} else if (f == 9) {
			name = (String) value
		} else if (f == 10) {
			systemChannelId = Snowflake.swornString(value)
		} else if (f == 11) {
			ownerApplicationId = Snowflake.swornString(value)
		} else if (f == 12) {
			afkTimeout = (int) value
		} else if (f == 13) {
			verificationLevel = (int) value
		} else if (f == 14) {
			mfaLevel = (int) value
		} else if (f == 15) {
			memberCount = (int) value
		} else if (f == 16) {
			defaultMessageNotificationLevel = (int) value
		} else if (f == 17) {
			explicitContentFilterLevel = (int) value
		} else if (f == 18) {
			embedEnabled = (boolean) value
		} else if (f == 19) {
			large = (boolean) value
		} else if (f == 20) {
			unavailable = (boolean) value
		} else if (f == 21) {
			ownerClient = (boolean) value
		} else if (f == 22) {
			features = (List<String>) value
		} else if (f == 23) {
			if (null == channelCache) channelCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new Channel(client)
				obj.guildId = id
				obj.fill(m)
				channelCache.add(obj)
			}
		} else if (f == 24) {
			if (null == roleCache) roleCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new Role(client)
				obj.guildId = id
				obj.fill(m)
				roleCache.add(obj)
			}
		} else if (f == 25) {
			if (null == memberCache) memberCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new Member(client)
				obj.guildId = id
				obj.fill(m)
				memberCache.add(obj)
			}
		} else if (f == 26) {
			if (null == presenceCache) presenceCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new Presence(client)
				obj.guildId = id
				obj.fill(m)
				presenceCache.add(obj)
			}
		} else if (f == 27) {
			if (null == voiceStateCache) voiceStateCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new VoiceState(client)
				obj.guildId = id
				obj.fill(m)
				voiceStateCache.add(obj)
			}
		} else if (f == 28) {
			if (null == emojiCache) emojiCache = new Cache<>()
			final lis = (List<Map>) value
			for (m in lis) {
				def obj = new Emoji(client)
				obj.guildId = id
				obj.fill(m)
				emojiCache.add(obj)
			}
		} else if (f == 29) {
			lazy = (boolean) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Date getJoinedAt() { ConversionUtil.fromJsonDate(rawJoinedAt) }
	String getIcon() {
		iconHash ? "https://cdn.discordapp.com/icons/$id/${iconHash}.jpg" : ''
	}
	boolean hasIcon() { iconHash }
	InputStream newIconInputStream() { inputStreamFromDiscord(icon) }
	File downloadIcon(file) { downloadFileFromDiscord(icon, file) }

	Member getOwner() { memberCache[ownerId] }

	Member getMe() { memberCache[client.id] }
	String changeNick(String newNick) { client.changeOwnGuildNick(id, newNick) }
	String nick(String newNick) { changeNick(newNick) }
	String editNick(String newNick) { changeNick(newNick) }
	String resetNick() { changeNick('') }

	Channel getDefaultChannel() { channelCache[id] ?: channels[0] }
	Channel getAfkChannel() { channelCache[afkChannelId] }
	Channel getWidgetChannel() { channelCache[embedChannelId] }
	boolean isMfaRequiredForStaff() { mfaLevel == MFALevelTypes.ELEVATED }

	Role getDefaultRole() { roleCache[id] }

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

	List<Channel> getChannels() { channelCache.list() }
	Map<Snowflake, Channel> getChannelMap() { channelCache.map() }

	List<Role> getRoles() { roleCache.list() }
	Map<Snowflake, Role> getRoleMap() { roleCache.map() }
	Set<Snowflake> getUsedRoleIds() {
		def res = new HashSet()
		for (e in memberCache) res.addAll(e.roleIds)
		res
	}

	Role role(ass) { find(roleCache, ass) }

	List<Member> getMembers() { memberCache.list() }
	Map<Snowflake, Member> getMemberMap() { memberCache.map() }

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

	String getRegion() { regionId }

	void ban(user, int days = 0) {
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
		for (it in roles) if (rg.contains(it.position)) x[it.id] = it.position + dif
		editRolePositions(x)
	}

	List<Channel> moveChannel(chan, int movement) {
		Channel c = channel(chan)
		def cp = Math.max(c.position + movement, 0)
		def cg = cp..<c.position
		def dif = cp <=> c.position
		Map x = [:]
		for (it in channels) if (cg.contains(it.position)) x[it.id] = it.position + dif
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

	List<Emoji> getEmojis() { emojiCache.list() }
	List<Emoji> getEmoji() { emojis }
	Map<Snowflake, Emoji> getEmojiIdMap() { emojiCache.map() }
	Map<String, Emoji> getEmojiNameMap() {
		def res = new HashMap<String, Emoji>()
		for (e in emojiCache) res.put(e.name, e)
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

		void jsonField(String name, value) {
			if (name == 'channel_id') channelId = Snowflake.swornString(name)
			else if (name == 'enabled') enabled = (boolean) value
			else client.log.debug("Unknown field number $name for ${this.class}")
		}

		Snowflake getId() { channelId }
		String getName() { channel.name }
		Channel getChannel() {
			for (final g : client.guildCache) {
				final ch = g.channelCache[channelId]
				if (null != ch) return ch
			}
			(Channel) null
		}
		Guild getGuild() { channel.guild }
		Embed edit(Map data) {
			client.editEmbed(data, guild, this)
		}
	}

	@InheritConstructors
	static class Ban extends DiscordObject {
		@Delegate(excludes = ['getClass', 'toString']) User user
		String reason

		void jsonField(String name, value) {
			if (name == 'user') {
				user = new User(client)
				user.fill((Map) value)
			}
			else if (name == 'reason') reason = (String) value
			else client.log.debug("Unknown field number $name for ${this.class}")
		}
	}
}

@CompileStatic
@InheritConstructors
@SuppressWarnings('GroovyUnusedDeclaration')
class VoiceState extends DiscordObject {
	Snowflake guildId, channelId, userId
	String token, sessionId
	boolean deaf, mute, selfDeaf, selfMute, suppress

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			guild_id: 1, channel_id: 2, user_id: 3, token: 4, session_id: 5,
			deaf: 6, mute: 7, self_deaf: 8, self_mute: 9, suppress: 10)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			guildId = Snowflake.swornString(value)
		} else if (f == 2) {
			channelId = Snowflake.swornString(value)
		} else if (f == 3) {
			userId = Snowflake.swornString(value)
		} else if (f == 4) {
			token = (String) value
		} else if (f == 5) {
			sessionId = (String) value
		} else if (f == 6) {
			deaf = (boolean) value
		} else if (f == 7) {
			mute = (boolean) value
		} else if (f == 8) {
			selfDeaf = (boolean) value
		} else if (f == 9) {
			selfMute = (boolean) value
		} else if (f == 10) {
			suppress = (boolean) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { null }
	Channel getChannel() {
		if (guildId) return getGuild(true).channelCache[channelId]
		for (final e : client.guildCache) {
			final ch = e.channelCache[userId]
			if (null != ch) return ch
		}
		null
	}
	User getUser() { guildId ? getGuild(true).memberCache[userId].user : client.user(userId) }
	Guild getGuild(boolean anyGuildId = guildId) { anyGuildId ? client.guildCache[guildId] : channel.guild }
	Channel getParent() { channel }
	Member getMember() {
		if (guildId) return getGuild(true).memberCache[userId]
		for (final e : client.guildCache) {
			final mem = e.memberCache[userId]
			if (null != mem) return mem
		}
		null
	}
	boolean isDeafened() { deaf }
	boolean isMuted() { mute }
	boolean isSelfDeafened() { selfDeaf }
	boolean isSelfMuted() { selfMute }
	String getName() { user.name }
}

@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Integration extends DiscordObject {
	Integration(Client client, Map object) { super(client, object) }

	Snowflake roleId
	String integrationId, name, rawSyncedAt, type
	int subscriberCount, expireBehavior, expireGracePeriod
	User user
	Map account
	boolean syncing, enableEmoticons, enabled

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			role_id: 1, id: 2, name: 3, synced_at: 4, type: 5, subscriber_count: 6,
			expire_behavior: 7, expire_grace_period: 8, user: 9, account: 10, syncing: 11,
			enable_emoticons: 12, enabled: 13)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			roleId = Snowflake.swornString(value)
		} else if (f == 2) {
			integrationId = (String) value
		} else if (f == 3) {
			name = (String) value
		} else if (f == 4) {
			rawSyncedAt = (String) value
		} else if (f == 5) {
			type = (String) value
		} else if (f == 6) {
			subscriberCount = (int) value
		} else if (f == 7) {
			expireBehavior = (int) value
		} else if (f == 8) {
			expireGracePeriod = (int) value
		} else if (f == 9) {
			user = new User(client)
			user.fill((Map) value)
		} else if (f == 10) {
			account = (Map) value
		} else if (f == 11) {
			syncing = (boolean) value
		} else if (f == 12) {
			enableEmoticons = (boolean) value
		} else if (f == 13) {
			enabled = (boolean) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { null }
	int getExpireBehaviour() { expireBehavior }
	Role getRole() {
		for (final g : client.guildCache) {
			final r = g.roleCache[roleId]
			if (null != r) return r
		}
		(Role) null
	}
	Guild getGuild() { role.guild }
	Date getSyncedAt() { ConversionUtil.fromJsonDate(rawSyncedAt) }
	Integration edit(Map data) { client.editIntegration(data, guild, this) }
	void delete() { client.deleteIntegration(guild, this) }
	void sync() { client.syncIntegration(guild, this) }
}

@InheritConstructors
@CompileStatic
@SuppressWarnings('GroovyUnusedDeclaration')
class Emoji extends DiscordObject {
	static final Pattern REGEX = ~/<:(?<name>\w+):(?<from>\d+)>/

	Snowflake id, guildId
	String name
	Set<Snowflake> roleIds
	boolean requiresColons, managed, animated

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			roles: 1, require_colons: 2, managed: 3, guild_id: 4,
			id: 5, name: 6, animated: 7)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			roleIds = Snowflake.swornStringSet(value)
		} else if (f == 2) {
			requiresColons = (boolean) value
		} else if (f == 3) {
			managed = (boolean) value
		} else if (f == 4) {
			guildId = Snowflake.swornString(value)
		} else if (f == 5) {
			id = Snowflake.swornString(value)
		} else if (f == 6) {
			name = (String) value
		} else if (f == 7) {
			animated = (boolean) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Guild getGuild() { client.guildCache[guildId] }
	Guild getParent() { guild }
	List<Role> getRoles() { guild.roleCache.scoop(roleIds) }
	boolean requiresColons() { requiresColons }
	boolean requireColons() { requiresColons }
	boolean isRequireColons() { requiresColons }
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
	int colorValue, position
	long permissionValue
	boolean hoist, managed, mentionable

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			color: 1, permissions: 2, position: 3, guild_id: 4, id: 5,
			hoist: 6, managed: 7, mentionable: 8, name: 9)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			colorValue = (int) value
		} else if (f == 2) {
			permissionValue = value instanceof String ? value.toLong() : (long) value
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
		} else client.log.debug("Unknown field number $field for ${this.class}")
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
		def r = new ArrayList<Member>()
		for (final a : client.guildCache[guildId].memberCache) if (a.roleIds.contains(id)) r.add(a)
		r
	}

	List<Snowflake> getMemberIds() {
		def r = new ArrayList<Snowflake>()
		for (final a : client.guildCache[guildId].memberCache) if (a.roleIds.contains(id)) r.add(a.id)
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
@SuppressWarnings('GroovyUnusedDeclaration')
class Member extends DiscordObject {
	Snowflake guildId
	Set<Snowflake> roleIds
	@Delegate(includes = ["getId", "getName", "getUsername",
		"getDiscriminator", "permissionsFor", "getUnique",
		"getDiscrim", "getNameAndDiscrim", "getUniqueName",
		"getAvatarHash", "getRawAvatarHash"])
	User user
	String getDiscriminator() { user.discriminator }
	String rawNick, rawJoinedAt
	boolean mute, deaf

	Member(Client c) {
		super(c)
	}

	Member(Client c, Map<?, ?> obj) {
		super(c)
		if (null != obj.id) jsonField('id', obj.id)
		if (null != obj.user) jsonField('user', obj.user)
		fill(obj)
	}

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			user: 1, roles: 2, nick: 3, guild_id: 4, joined_at: 5,
			mute: 6, deaf: 7, id: 8, username: 9, discriminator: 10, avatar: 11)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else super.getClient().log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			if (null == user) user = new User(super.getClient())
			user.fill((Map) value)
		} else if (f == 2) {
			roleIds = Snowflake.swornStringSet(value)
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
		} else if (f == 8) {
			if (null == user) user = new User(client)
			user.id = Snowflake.swornString(value)
		} else if (f == 9) {
			if (null == user) user = new User(client)
			user.username = (String) value
		} else if (f == 10) {
			if (null == user) user = new User(client)
			user.avatarHash = (String) value
		} else if (f == 11) {
			if (null == user) user = new User(client)
			user.discriminator = (String) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
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
		def full = new Permissions(guild.defaultRole.permissionValue)
		for (role in roles) {
			def perms = role.permissions
			if (perms.has(Permission.ADMINISTRATOR)) return Permissions.ALL_TRUE
			full.add(perms)
		}
		full
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
	String status, nick
	Set<Snowflake> roleIds
	Game game
	User user
	long lastModified

	private static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			guild_id: 1, status: 2, game: 3, user: 4, last_modified: 5, nick: 6, roles: 7)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			guildId = Snowflake.swornString(value)
		} else if (f == 2) {
			status = (String) value
		} else if (f == 3) {
			if (null == value) game = null
			else {
				game = new Game(client)
				game.fill((Map) value)
			}
		} else if (f == 4) {
			if (null == user) user = new User(client)
			user.fill((Map) value)
		} else if (f == 5) {
			lastModified = (long) value
		} else if (f == 6) {
			nick = (String) value
		} else if (f == 7) {
			roleIds = Snowflake.swornStringSet(value)
		} else client.log.debug("Unknown field number $field for ${this.class}")
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
	Map timestamps

	private static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			name: 1, url: 2, type: 3, timestamps: 4)

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null != field) jsonField(field, value)
		else client.log.debug("Unknown field $name for ${this.class}")
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
		} else if (f == 4) {
			timestamps = (Map) value
		} else client.log.debug("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { new Snowflake((long) type) }
	String toString() { type == 0 ? name : "$name ($url)" }
}