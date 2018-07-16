package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.Snowflake
import hlaaftana.discordg.collections.Cache

import java.util.regex.Pattern

/**
 * A Discord channel.
 * @author Hlaaftana
 */
@SuppressWarnings('GroovyUnusedDeclaration')
@CompileStatic
@InheritConstructors
class Channel extends DiscordObject {
	static final Pattern MENTION_REGEX = ~/<#(\d+)>/

	Snowflake id, guildId, lastMessageId
	Integer position, type, bitrate, userLimit
	boolean isPrivateField, nsfw
	String name, categoryId, topic
	Cache<User> recipientCache
	Cache<PermissionOverwrite> permissionOverwriteCache

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, name: 2, topic: 3, guild_id: 4, last_message_id: 5,
			position: 6, type: 7, bitrate: 8, user_limit: 9,
			is_private: 10, nsfw: 11, recipients: 12, permission_overwrites: 13)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			name = (String) value
		} else if (f == 3) {
			topic = (String) value
		} else if (f == 4) {
			guildId = Snowflake.swornString(value)
		} else if (f == 5) {
			lastMessageId = Snowflake.swornString(value)
		} else if (f == 6) {
			position = (int) value
		} else if (f == 7) {
			type = (int) value
		} else if (f == 8) {
			bitrate = (int) value
		} else if (f == 9) {
			userLimit = (int) value
		} else if (f == 10) {
			isPrivateField = (boolean) value
		} else if (f == 11) {
			nsfw = (boolean) value
		} else if (f == 12) {
			if (null == recipientCache)
				recipientCache = new Cache<>()
			final rs = (List<Map>) value
			for (r in rs) recipientCache.add(new User(client, r))
		} else if (f == 13) {
			if (null == permissionOverwriteCache)
				permissionOverwriteCache = new Cache<>()
			final rs = (List<Map>) value
			for (r in rs) {
				def po = new PermissionOverwrite(client, r)
				po.channelId = id
				permissionOverwriteCache.add(po)
			}
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	String getMention() { "<#$id>" }
	/**
	 * Only for guild channels.
	 */
	boolean isText() { type == 0 }
	boolean isPrivate() { isPrivateField || dm || group }
	boolean isDm() { type == 1 }
	/**
	 * Only for guild channels.
	 */
	boolean isVoice() { type == 2 }
	boolean isGroup() { type == 3 }
	boolean isInGuild() { text || voice || category }
	boolean isCategory() { type == 4 }
	Channel getCategory() { null == categoryId ? null : guild.channel(categoryId) }
	Guild getGuild() { dm || group ? null : client.guildCache.get(guildId) }
	List<User> getUsers() {
		(List<User>) (inGuild ? members : (recipients + client.user))
	}
	List<User> getRecipients() { recipientCache?.list() }
	Map<Snowflake, User> getRecipientMap() { recipientCache?.map() }
	User getUser() { recipients[0] }
	User getRecipient() { user }
	String getName() {
		dm ? user.name : group ? (recipientCache.list()*.name.join(', ')
				?: 'Unnamed') : this.@name
	}

	void addRecipient(user) {
		client.addChannelRecipient(this, user)
	}

	void removeRecipient(user) {
		client.removeChannelRecipient(this, user)
	}

	Cache<PermissionOverwrite> getOverwriteCache() { permissionOverwriteCache }

	List<PermissionOverwrite> getPermissionOverwrites() { overwrites }

	List<PermissionOverwrite> getOverwrites() { overwriteCache.list() }

	Map<Snowflake, PermissionOverwrite> getPermissionOverwriteMap() {
		overwriteCache.map()
	}

	Map<Snowflake, PermissionOverwrite> getOverwriteMap() { permissionOverwriteMap }

	PermissionOverwrite permissionOverwrite(ass) {
		(PermissionOverwrite) find(overwriteCache, ass)
	}

	PermissionOverwrite overwrite(ass) {
		permissionOverwrite(ass)
	}

	Permissions permissionsFor(user, Permissions initialPerms) {
		if (this.private) return Permissions.PRIVATE_CHANNEL
		Member member = guild.member(user)
		if (!member) return Permissions.ALL_FALSE
		def doodle = initialPerms.value
		def owMap = overwriteCache
		def everyoneOw = owMap[id]
		if (everyoneOw) {
			doodle &= ~everyoneOw.deniedValue
			doodle |= everyoneOw.allowedValue
		}
		def roleOws = new ArrayList<PermissionOverwrite>()
		for (r in member.roleIds) if (owMap.containsKey(r)) roleOws.add(owMap[r])
		if (roleOws) for (r in roleOws) {
			doodle &= ~r.deniedValue
			doodle |= r.allowedValue
		}
		final userOw = owMap[member.id]
		if (null != userOw) {
			doodle &= ~userOw.deniedValue
			doodle |= userOw.allowedValue
		}
		new Permissions(doodle)
	}

	Permissions permissionsFor(user) {
		permissionsFor(user, guild.member(user).permissions)
	}

	boolean canSee(user) {
		if (this.private) recipientCache.containsKey(Snowflake.from(user))
		else permissionsFor(user)[Permissions.BitOffsets.READ_MESSAGES]
	}

	List<Invite> requestInvites() {
		client.requestChannelInvites(this)
	}

	Invite createInvite(Map data = [:]) {
		client.createInvite(data, this)
	}

	void startTyping() {
		client.startTyping(this)
	}

	void delete() {
		client.deleteChannel(this)
	}

	Channel edit(Map data = [:]) {
		client.editChannel(data, this)
	}

	def move(int movement) { guild.moveChannel(this, movement) }

	void editPermissions(Map d, t) { client.editChannelOverwrite(d, this, t) }
	void editPermissions(t, Map d) { client.editChannelOverwrite(d, this, t) }
	void addPermissions(Map d, t) { client.editChannelOverwrite(d, this, t) }
	void addPermissions(t, Map d) { client.editChannelOverwrite(d, this, t) }
	void createPermissions(Map d, t) { client.editChannelOverwrite(d, this, t) }
	void createPermissions(t, Map d) { client.editChannelOverwrite(d, this, t) }

	void deletePermissions(t) { client.deleteChannelOverwrite(this, t) }

	Webhook createWebhook(Map data = [:]) {
		client.createWebhook(data, this)
	}

	List<Webhook> requestWebhooks() {
		client.requestChannelWebhooks(this)
	}

	Message sendMessage(content, boolean tts = false) {
		client.sendMessage(content: content, tts: tts, this)
	}

	Message sendMessage(Map data) {
		client.sendMessage(data, this)
	}

	Message send(Map data) { sendMessage(data) }
	Message send(content, boolean tts = false) { sendMessage(content, tts) }

	Message editMessage(message, content) {
		editMessage(message, content: content)
	}

	Message editMessage(Map data, message) {
		editMessage(message, data)
	}

	Message editMessage(message, Map data) {
		client.editMessage(data, this, message)
	}

	void deleteMessage(message) {
		client.deleteMessage(this, message)
	}

	Message sendFile(Map data, implicatedFile, filename = null) {
		client.sendFile(data, this, implicatedFile, filename)
	}

	Message sendFile(implicatedFile, filename = null) {
		sendFile([:], implicatedFile, filename)
	}

	Message requestMessage(message, boolean atc = true) {
		client.requestMessage(this, message, atc)
	}

	Message message(message, boolean aa = true) {
		client.message(this, message, aa)
	}

	def pinMessage(message) { client.pinMessage(this, message) }
	def pin(message) { pinMessage(message) }

	def unpinMessage(message) { client.unpinMessage(this, message) }
	def unpin(message) { unpinMessage(message) }

	Collection<Message> requestPinnedMessages() { client.requestPinnedMessages(this) }
	Collection<Message> requestPins() { requestPinnedMessages() }

	void react(message, emoji) { client.reactToMessage(this, message, emoji) }
	void unreact(m, e, u = '@me') { client.unreactToMessage(this, m, e, u) }

	List<User> requestReactors(m, e, int l = 100) { client.requestReactors(this, m, e, l) }

	List<Message> requestLogs(int m = 100, b = null,
		bt = 'before') { client.requestChannelLogs(this, m, b, bt.toString()) }
	List<Message> logs(int m = 100, b = null,
		bt = 'before') { requestLogs(m, b, bt) }
	List<Message> forceRequestLogs(int m = 100, b = null, String bt = 'before') {
		client.forceRequestChannelLogs(this, m, b, bt) }

	List<Message> getCachedLogs() { (List<Message>) (client.messages[id]?.list() ?: []) }
	Map<Snowflake, Message> getCachedLogMap() { (Map<Snowflake, Message>) (client.messages[id]?.map() ?: [:]) }

	def clear(int number = 100) { clear(logs(number)*.id) }

	def clear(int number = 100, Closure closure) { clear(logs(number).findAll { closure(it) }) }

	def clear(List ids) {
		client.bulkDeleteMessages(this, ids)
	}

	def clear(user, int number = 100) {
		clear(number) { Message it -> it.author.id == Snowflake.from(user) }
	}

	Message find(int number = 100, int maxTries = 10,
	             @ClosureParams(value = SimpleType, options = "hlaaftana.discordg.objects.Message") Closure closure) {
		List<Message> messages = logs(number)
		Message ass = messages.find(closure)
		if (ass) ass
		else {
			while (!ass) {
				if (((messages.size() - 100) / 50) > maxTries) return null
				maxTries++
				messages = logs(number, messages.min { it.id })
				ass = messages.find(closure)
			}
			ass
		}
	}

	List<VoiceState> getVoiceStates() {
		int hash = id.hashCode()
		guild.voiceStates.findAll {
			def i = it.channel.id
			hash == i.hashCode() && id == i
		}
	}

	List<Member> getMembers() {
		inGuild ? (text ? guild.members.findAll { canSee(it) } : voiceStates*.member) : null
	}

	Map<Snowflake, VoiceState> getVoiceStateMap() {
		def vs = voiceStates
		def res = new HashMap<>(vs.size())
		for (v in vs) res.put(v.id, v)
		res
	}

	Map<Snowflake, Member> getMemberMap() {
		def vs = members
		def res = new HashMap<>(vs.size())
		for (v in vs) res.put(v.id, v)
		res
	}

	VoiceState voiceState(thing) { findBuilt(voiceStateMap, thing) }
	Member member(thing) { findBuilt(memberMap, thing) }
	Call getOngoingCall() { client.ongoingCall(id) }

	boolean isFull() {
		voiceStates.size() == userLimit
	}

	boolean canJoin() {
		Permissions ass = guild.me.permissionsFor(this)
		ass[Permissions.BitOffsets.CONNECT] && (userLimit ? (!full || ass[Permissions.BitOffsets.MOVE_MEMBERS]) : true )
	}

	void move(member) {
		client.moveMemberVoiceChannel(guildId, member, this)
	}

	/*VoiceClient join(Map opts = [:]) {
		if (!canJoin()) throw new Exception(full ? 'Channel is full' : "Insufficient permissions to join voice channel ${inspect()}")
		VoiceClient vc = new VoiceClient(client, this, opts)
		client.voiceClients[guild] = vc
		vc.connect()

		Thread.start {
			while (vc.@endpoint == null) {}
			WebSocketClient wsc = new WebSocketClient(new SslContextFactory())
			VoiceWSClient socket = new VoiceWSClient(vc)
			wsc.start()
			wsc.connect(socket, new URI(vc.endpoint), new ClientUpgradeRequest())
			vc.ws = socket
		}
		vc
	}*/
}

@InheritConstructors
@CompileStatic
class PermissionOverwrite extends DiscordObject {
	int allowedValue, deniedValue
	String type
	Snowflake channelId, id

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			id: 1, channel_id: 2, allow: 3, deny: 4, type: 5)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			id = Snowflake.swornString(value)
		} else if (f == 2) {
			channelId = Snowflake.swornString(value)
		} else if (f == 3) {
			allowedValue = (int) value
		} else if (f == 4) {
			deniedValue = (int) value
		} else if (f == 5) {
			type = (String) value
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	Permissions getAllowed() { new Permissions(allowedValue) }
	Permissions getDenied() { new Permissions(deniedValue) }
	DiscordObject getAffected() {
		if (type == 'role') {
			channel.guild.role(id)
		} else if (type == 'member') {
			channel.guild.member(id)
		} else null
	}
	Channel getChannel() { client.channel(channelId) }
	Channel getParent() { channel }
	void edit(Map a) {
		def de = [allow: allowed, deny: denied]
		client.editChannelOverwrite(de << a, channelId, id)
	}
	void delete() { client.deleteChannelOverwrite(channelId, id) }
	String getName() { affected.name }
	boolean involves(DiscordObject involved) {
		if (id == channel.guildId) true
		else if (involved instanceof User)
			affected instanceof Role ? ((Role) affected).memberIds.contains(involved.id) : involved == affected
		else if (involved instanceof Role) involved == affected
		else false
	}
	boolean isRole() { type == 'role' }
	boolean isMember() { type == 'member' }
	boolean isUser() { type == 'member' }
}

@InheritConstructors
@CompileStatic
class Call extends DiscordObject {
	Snowflake channelId, messageId
	String regionId
	Set<Snowflake> ringingUserIds
	List<VoiceState> voiceStates
	boolean unavailable

	static final Map<String, Integer> FIELDS = Collections.unmodifiableMap(
			channel_id: 1, message_id: 2, region_id: 3, ringing: 4, voice_states: 5)

	void jsonField(String name, value) {
		jsonField(FIELDS.get(name), value)
	}

	void jsonField(Integer field, value) {
		if (null == field) return
		int f = field.intValue()
		if (f == 1) {
			channelId = Snowflake.swornString(value)
		} else if (f == 2) {
			messageId = Snowflake.swornString(value)
		} else if (f == 3) {
			regionId = (String) value
		} else if (f == 4) {
			if (null == ringingUserIds) ringingUserIds = new HashSet<>()
			ringingUserIds.addAll(Snowflake.swornStringSet(value))
		} else if (f == 5) {
			if (null == voiceStates) voiceStates = new ArrayList<>(voiceStates.size())
			for (m in ((List<Map>) value)) voiceStates.add(new VoiceState(client, m))
		} else client.log.warn("Unknown field number $field for ${this.class}")
	}

	Snowflake getId() { channelId }
	String getName() { null }
	List<User> getRingingUsers() { ringingUserIds.collect(client.&user) }
}