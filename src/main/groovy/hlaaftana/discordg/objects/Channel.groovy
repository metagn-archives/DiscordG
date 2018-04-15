package hlaaftana.discordg.objects

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordObject
import hlaaftana.discordg.Permissions
import hlaaftana.discordg.collections.DiscordListCache

import java.util.regex.Pattern

/**
 * A Discord channel.
 * @author Hlaaftana
 */
@InheritConstructors
@SuppressWarnings('GroovyUnusedDeclaration')
@CompileStatic
class Channel extends DiscordObject {
	static final Pattern MENTION_REGEX = ~/<#(\d+)>/

	String getMention() { "<#$id>" }
	Integer getPosition() { (Integer) object.position }
	Integer getType() { (Integer) object.type }
	/**
	 * Only for guild channels.
	 */
	boolean isText() { type == 0 }
	boolean isPrivate() { (boolean) object.is_private || dm || group }
	boolean isDm() { type == 1 }
	/**
	 * Only for guild channels.
	 */
	boolean isVoice() { type == 2 }
	boolean isGroup() { type == 3 }
	boolean isInGuild() { text || voice || category }
	boolean isCategory() { type == 4 }
	String getCategoryId() { (String) object.parent_id }
	Channel getCategory() { null == categoryId ? null : guild.channel(categoryId) }
	boolean isNsfw() { (boolean) object.nsfw }
	String getTopic() { (String) object.topic }
	Guild getGuild() { dm || group ? null : client.guildCache.at(guildId) }
	String getGuildId() { (String) object.guild_id }
	List<User> getUsers() {
		(List<User>) (inGuild ? members : (recipients + client.user))
	}
	DiscordListCache<User> getRecipientCache() { (DiscordListCache<User>) object.recipients }
	List<User> getRecipients() { recipientCache?.list() }
	Map<String, User> getRecipientMap() { recipientCache?.map() }
	User getUser() { recipients[0] }
	User getRecipient() { user }
	String getName() {
		dm ? user.name : group ? (recipientCache.rawList()*.get('name').join(', ')
				?: 'Unnamed') : (String) object.name
	}

	void addRecipient(user) {
		client.addChannelRecipient(this, user)
	}

	void removeRecipient(user) {
		client.removeChannelRecipient(this, user)
	}

	DiscordListCache<PermissionOverwrite> getPermissionOverwriteCache() {
		(DiscordListCache<PermissionOverwrite>) object.permission_overwrites
	}

	DiscordListCache<PermissionOverwrite> getOverwriteCache() { permissionOverwriteCache }

	List<PermissionOverwrite> getPermissionOverwrites() { overwrites }

	List<PermissionOverwrite> getOverwrites() { overwriteCache.list() }

	Map<String, PermissionOverwrite> getPermissionOverwriteMap() {
		overwriteCache.map()
	}

	Map<String, PermissionOverwrite> getOverwriteMap() { permissionOverwriteMap }

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
			doodle &= ~((int) everyoneOw.deny)
			doodle |= (int) everyoneOw.allow
		}
		def roleOws = new ArrayList<Map<String, Object>>()
		for (r in member.roleIds) if (owMap.containsKey(r)) roleOws.add(owMap[r])
		if (roleOws) for (r in roleOws) {
			doodle &= ~((int) r.deny)
			doodle |= (int) r.allow
		}
		if (owMap.containsKey(id(user))){
			def userOw = owMap[id(user)]
			doodle &= ~((int) userOw.deny)
			doodle |= (int) userOw.allow
		}
		new Permissions(doodle)
	}

	Permissions permissionsFor(user) {
		permissionsFor(user, guild.member(user).permissions)
	}

	boolean canSee(user) {
		if (this.private) recipientCache.containsKey(id(user))
		else permissionsFor(user)['readMessages']
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

	Message sendMessage(content, boolean tts=false) {
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
	Map<String, Message> getCachedLogMap() { (Map<String, Message>) (client.messages[id]?.map() ?: [:]) }

	def clear(int number = 100) { clear(logs(number)*.id) }

	def clear(int number = 100, Closure closure) { clear(logs(number).findAll { closure(it) }) }

	def clear(List ids) {
		client.bulkDeleteMessages(this, ids)
	}

	def clear(user, int number = 100) {
		clear(number) { Message it -> ((String) ((Map<String, Object>) it.object.author).id) == id(user) }
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

	String getLastMessageId() { (String) object.last_message_id }

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

	Map<String, VoiceState> getVoiceStateMap() {
		def vs = voiceStates
		def res = new HashMap<>(vs.size())
		for (v in vs) res.put(v.id, v)
		res
	}

	Map<String, Member> getMemberMap() {
		def vs = members
		def res = new HashMap<>(vs.size())
		for (v in vs) res.put(v.id, v)
		res
	}

	VoiceState voiceState(thing) { findBuilt(voiceStateMap, thing) }
	User member(thing) { findBuilt(memberMap, thing) }
	Call getOngoingCall() { client.ongoingCall(id) }

	int getBitrate() { (int) object.bitrate }
	int getUserLimit() { (int) object.user_limit }

	boolean isFull() {
		voiceStates.size() == userLimit
	}

	boolean canJoin() {
		Permissions ass = guild.me.permissionsFor(this)
		ass['connect'] && (userLimit ? (!full || ass['moveMembers']) : true )
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

	static Map construct(Client client, Map c, String guildId = null) {
		if (guildId) c.guild_id = guildId
		if (c.guild_id) {
			def po = c.permission_overwrites
			if (po instanceof List<Map<String, Object>>) {
				def a = new ArrayList<Map<String, Object>>(po.size())
				for (x in po) {
					def j = new HashMap((Map) x)
					j.put('channel_id', c.id)
					a.add(j)
				}
				c.permission_overwrites = new DiscordListCache(a, client, PermissionOverwrite)
			}
		} else if (c.recipients != null) {
			c.recipients = new DiscordListCache((List<Map>) c.recipients, client, User)
		}
		c
	}
}

@InheritConstructors
@CompileStatic
class PermissionOverwrite extends DiscordObject {
	int getAllowedValue() { def x = object.allow; null == x ? 0 : (int) x }
	int getDeniedValue() { def x = object.deny; null == x ? 0 : (int) x }
	Permissions getAllowed() { new Permissions(allowedValue) }
	Permissions getDenied() { new Permissions(deniedValue) }
	String getType() { (String) object.type }
	DiscordObject getAffected() {
		if (type == 'role') {
			channel.guild.role(id)
		} else if (type == 'member') {
			channel.guild.member(id)
		} else null
	}
	String getChannelId() { (String) object.channel_id }
	Channel getChannel() { client.channel(channelId) }
	Channel getParent() { channel }
	void edit(Map a) {
		def de = [allow: allowed, deny: denied]
		client.editChannelOverwrite(a, channelId, id)
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
	String getId() { (String) object.channel_id }
	String getMessageId() { (String) object.message_id }
	String getRegionId() { (String) object.region }
	List<String> getRingingUserIds() { (List<String>) object.ringing }
	List<User> getRingingUsers() { ringingUserIds.collect(client.&user) }
	List<VoiceState> getVoiceStates() { ((List<Map>) object.voice_states).collect { new VoiceState(client, it) } }
	boolean isUnavailable() { (boolean) object.unavailable }
}