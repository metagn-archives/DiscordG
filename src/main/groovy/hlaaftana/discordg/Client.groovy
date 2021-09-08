package hlaaftana.discordg

import com.mashape.unirest.http.Unirest
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.data.Intents
import hlaaftana.discordg.data.Permissions
import hlaaftana.discordg.data.Snowflake
import hlaaftana.discordg.exceptions.MessageInvalidException

import java.math.MathContext
import java.math.RoundingMode

import static hlaaftana.discordg.logic.ActionPool.create as newPool
import hlaaftana.discordg.logic.*
import hlaaftana.discordg.net.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.data.*

import java.awt.Color
import java.util.regex.Pattern

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * The Discord client.
 * @author Hlaaftana
 */
@SuppressWarnings('GroovyUnusedDeclaration')
@CompileStatic
class Client extends User {
	/**
	 * Map of uppercase event names to be mapped to real discord event names in parseEvent.
	 */
	static Map<String, String> eventAliases = [MESSAGE: 'MESSAGE_CREATE',
		NEW_MESSAGE: 'MESSAGE_CREATE', MESSAGE_DELETED: 'MESSAGE_DELETE',
		MESSAGE_BULK_DELETE: 'MESSAGE_DELETE_BULK',
		MESSAGE_BULK_DELETED: 'MESSAGE_DELETE_BULK',
		MESSAGE_UPDATED: 'MESSAGE_UPDATE', CHANNEL: 'CHANNEL_CREATE',
		NEW_CHANNEL: 'CHANNEL_CREATE', CHANNEL_UPDATED: 'CHANNEL_UPDATE',
		CHANNEL_DELETED: 'CHANNEL_DELETE', BAN: 'GUILD_BAN_ADD',
		UNBAN: 'GUILD_BAN_REMOVE', GUILD: 'GUILD_CREATE',
		GUILD_JOIN: 'GUILD_CREATE', GUILD_JOINED: 'GUILD_CREATE',
		NEW_GUILD: 'GUILD_CREATE', JOIN_GUILD: 'GUILD_CREATE', JOINED_GUILD: 'GUILD_CREATE',
		GUILD_CREATE: 'GUILD_CREATE', GUILD_CREATED: 'GUILD_CREATE',
		GUILD_UPDATED: 'GUILD_UPDATE', GUILD_UPDATE: 'GUILD_UPDATE',
		GUILD_DELETE: 'GUILD_DELETE', GUILD_DELETED: 'GUILD_DELETE',
		MEMBER: 'GUILD_MEMBER_ADD',
		MEMBER_JOINED: 'GUILD_MEMBER_ADD', NEW_MEMBER: 'GUILD_MEMBER_ADD',
		MEMBER_UPDATE: 'GUILD_MEMBER_UPDATE',
		MEMBER_UPDATED: 'GUILD_MEMBER_UPDATE', MEMBER_LEFT: 'GUILD_MEMBER_REMOVE',
		MEMBER_KICKED: 'GUILD_MEMBER_REMOVE', MEMBER_REMOVED: 'GUILD_MEMBER_REMOVE',
		MEMBER_LEAVE: 'GUILD_MEMBER_REMOVE',
		MEMBER_KICK: 'GUILD_MEMBER_REMOVE', MEMBER_REMOVE: 'GUILD_MEMBER_REMOVE',
		ROLE: 'GUILD_ROLE_CREATE', NEW_ROLE: 'GUILD_ROLE_CREATE',
		ROLE_UPDATE: 'GUILD_ROLE_UPDATE', ROLE_UPDATED: 'GUILD_ROLE_UPDATE',
		GUILD_ROLE_UPDATE: 'GUILD_ROLE_UPDATE', ROLE_DELETE: 'GUILD_ROLE_DELETE',
		ROLE_DELETED: 'GUILD_ROLE_DELETE', PRESENCE: 'PRESENCE_UPDATE',
		TYPING: 'TYPING_START', REACTION: 'MESSAGE_REACTION_ADD',
		REACTION_ADD: 'MESSAGE_REACTION_ADD', NEW_REACTION: 'MESSAGE_REACTION_ADD',
		REACTION_REMOVED: 'MESSAGE_REACTION_REMOVE',
		REACTION_DELETED: 'MESSAGE_REACTION_REMOVE',
		REACTION_REMOVE: 'MESSAGE_REACTION_REMOVE',
		REACTION_DELETE: 'MESSAGE_REACTION_REMOVE',
		MESSAGE_REACTION_REMOVED: 'MESSAGE_REACTION_REMOVE',
		MESSAGE_REACTION_DELETED: 'MESSAGE_REACTION_REMOVE',
		MESSAGE_REACTION_DELETE: 'MESSAGE_REACTION_REMOVE',
		NOTE_UPDATE: 'USER_NOTE_UPDATE', NOTE_UPDATED: 'USER_NOTE_UPDATE',
		NOTE: 'USER_NOTE_UPDATE', CALL: 'CALL_CREATE', CALL_STARTED: 'CALL_CREATE',
		CALL_UPDATED: 'CALL_UPDATE', CALL_ENDED: 'CALL_DELETE',
		RECIPIENT: 'CHANNEL_RECIPIENT_ADD', RECIPIENT_ADD: 'CHANNEL_RECIPIENT_ADD',
		RECIPIENT_ADDED: 'CHANNEL_RECIPIENT_ADD',
		NEW_RECIPIENT: 'CHANNEL_RECIPIENT_ADD',
		RECIPIENT_REMOVE: 'CHANNEL_RECIPIENT_REMOVE',
		RECIPIENT_REMOVED: 'CHANNEL_RECIPIENT_REMOVE', RELATIONSHIP: 'RELATIONSHIP_ADD',
		NEW_RELATIONSHIP: 'RELATIONSHIP_ADD', RELATIONSHIP_ADDED: 'RELATIONSHIP_ADD',
		RELATIONSHIP_REMOVED: 'RELATIONSHIP_REMOVE']
	/**
	 * Events Discord is known to send.
	 */
	static List<String> knownDiscordEvents = ['READY', 'MESSAGE_ACK', 'GUILD_INTEGRATIONS_UPDATE',
		'GUILD_EMOJIS_UPDATE', 'VOICE_STATE_UPDATE', 'VOICE_GUILD_UPDATE', 'USER_UPDATE',
		'USER_GUILD_SETTINGS_UPDATE', 'USER_SETTINGS_UPDATE', 'GUILD_MEMBERS_CHUNK',
		'GUILD_SYNC', 'CHANNEL_PINS_UPDATE', 'CHANNEL_PINS_ACK',
		'MESSAGE_REACTION_REMOVE_ALL', 'WEBHOOKS_UPDATE', 'RESUMED'] +
			(eventAliases.values() as ArrayList).toSet()
	/**
	 * Events DiscordG might send.
	 */
	static List<String> knownEvents = ['INITIAL_GUILD_CREATE', 'UNRECOGNIZED', 'ALL'] + knownDiscordEvents

	String customUserAgent = ''
	String getFullUserAgent() {
		userAccount ? customUserAgent : "$DiscordG.USER_AGENT $customUserAgent"
	}

	String tokenPrefix = 'Bot'
	String rawToken
	boolean botField
	String email, emailField
	String getEmail() { emailField ?: email }
	String password
	boolean confirmedBot
	void setBot(boolean x) { if (x && !confirmedBot) confirmedBot = true }
	
	/** if the key is a string, it calls .replace
	 * if the key is a pattern, it calls .replaceAll
     */ 
	Map<Object, Object> messageFilters = new HashMap<Object, Object>(
		'@everyone': '@\u200beveryone',
		'@here': '@\u200bhere'
	)
	/**
	 * name in log
	 */
	String logName = 'DiscordG'
	/**
	 * discord gateway version, dont change unless you know what it is and want to
	 */
	int gatewayVersion = DiscordG.DEFAULT_GATEWAY_VERSION
	/**
	 * cache tokens from email and password logins. dont turn this off.
	 */
	boolean cacheTokens = true
	/**
	 * path to the token cache file
	 */
	String tokenCachePath = 'token.json'
	/**
	 * maximum amount of events that can be handled at the same time
	 * increasing might help with lag but takes up more CPU
	 */
	int threadPoolSize = 3
	/**
	 * number of maximum members in a guild until discord doesnt send offline members
	 * set lower for a possible RAM decrease
	 */
	int largeThreshold = 250 
	/** 
	 * request offline members after discord gives us the online ones for large guilds 
	 * set this to false if youre changing large threshold or if it uses too much RAM
	 */
	boolean requestMembersOnReady = true
	/**
	 * adds the READY raw event data to the cache
	 * set to false for possible RAM decrease
	 */
	boolean copyReady = true
	/**
	 * retry a request on a 5xx status code
	 * generally harmless, 5xx status codes are usually nothing
	 */
	boolean retryOn502 = true
	/**
	 * requests a member on PRESENCE_UPDATE for the joined_at data for a newly discovered member
	 * dont turn it on unless you need join dates and cant request offline members
	 */
	boolean allowMemberRequesting = false
	/**
	 * spread message bulk delete events to separate message delete events
	 * true by default for easier handling by bots
	 */
	boolean spreadBulkDelete = true
	/**
	 * timeout for waiting for guild_create events after the READY
	 * only for bots and accounts at or over 100 guilds
	 */
	long guildTimeout = 30_000
	boolean permissionsAreStrings = true
	/**
	 * whitelisted events
	 */
	List<String> eventWhitelist = []
	/**
	 * blacklisted events
	 */
	List<String> eventBlacklist = ['TYPING_START']
	/**
	 * for shards, [shardId, shardCount]
	 */
	Tuple2 shardTuple
	Intents intents
	boolean userAccount = false
	
	Log logObj
	Map<String, Object> extraIdentifyData = [:]
	Set<Snowflake> mutedChannels = []
	Map<String, ActionPool> pools = [
		sendMessages: newPool(5, 5_000),
		deleteMessages: newPool(5, 1_000),
		bulkDeleteMessages: newPool(1, 1_000),
		editMembers: newPool(10, 10_000),
		changeNick: newPool(1, 1_000),
		changePresence: newPool(5, 60_000),
		wsAnything: newPool(120, 60_000),
		login: newPool(1, 60_000),
		connect: newPool(1, 30_000)
	]
	
	List<DiscordRawWSListener> rawListeners = []
	@Delegate(excludes = ['hashCode', 'equals', 'toString', 'listenerError', 'parseEvent'])
	ListenerSystem<Map<String, Object>> listenerSystem = new ClientListenerSystem()
	class ClientListenerSystem extends ListenerSystem<Map<String, Object>> {
		String parseEvent(param) {
			Client.this.parseEvent(param)
		}

		void listenerError(event, Throwable ex, Closure closure, Map<String, Object> data) {
			Client.this.listenerError((String) event, ex, closure, data)
		}
	}
	HTTPClient http = new HTTPClient(this)
	WSClient ws
	Map readyData = new HashMap<>()
	boolean cacheMessages = true
	Map<Snowflake, Cache<Message>> messages = Collections.synchronizedMap(new HashMap<Snowflake, Cache<Message>>())
	boolean cacheReactions = false
	Map<Snowflake, List<Reaction>> reactions = Collections.synchronizedMap(new HashMap<Snowflake, List<Reaction>>())
	Cache<Call> calls = new Cache<>()
	Cache<Guild> guildCache
	Cache<Presence> presenceCache
	Cache<Channel> privateChannelCache
	Cache<Relationship> relationshipCache
	Map<Snowflake, Map<String, Object>> userGuildSettingCache
	Map<String, Object> userObject
	String gateway
	String sessionId
	boolean gatewayClosed

	Client() {
		super(null, [:])
		client = this
		addCacher()
	}

	Log getLog() {
		if (null == logObj) logObj = new Log(logName)
		logObj
	}

	void setLogName(String name) {
		this.@logName = name
		if (null != logObj) logObj.name = name
	}

	void setRequestMembersOnReady(boolean doso) {
		if (doso) requestMembersOnReady() else dontRequestMembersOnReady()
	}

	boolean getRequestMembersOnReady() {
		rawListeners.contains(memberRequesterOnReady)
	}

	String getToken() {
		tokenPrefix ? "$tokenPrefix $rawToken" : rawToken
	}

	void setToken(String newToken) {
		rawToken = newToken.startsWith('Bot ') ? newToken.substring(4) : newToken
	}
	
	void setConfirmedBot(boolean ass) {
		if (ass) tokenPrefix = 'Bot'
		else tokenPrefix = ''
		this.@confirmedBot = ass
	}
	
	boolean isBot() {
		boolean ass = confirmedBot || botField
		tokenPrefix = ass ? 'Bot' : ''
		ass
	}

	WSClient getWebSocketClient() { ws }

	void jsonField(String name, value) {
		final field = FIELDS.get(name)
		if (null == field) return
		super.jsonField(field, value)
	}

	def blacklist(event) { eventBlacklist.add(parseEvent(event)) }
	def whitelist(event) { eventWhitelist.add(parseEvent(event)) }

	void login(String email, String password) {
		log.info 'Getting token...'
		this.email = email
		this.password = password
		File tokenCache = new File(tokenCachePath)
		if (!cacheTokens) token = requestToken(email, password)
		else try {
			token = (String) ((Map) JSONUtil.parse(tokenCache)[(email)]).token
		} catch (ignored) {
			JSONUtil.modify(tokenCache,
				[(email): [token: requestToken(email, password)]])
			token = (String) ((Map) JSONUtil.parse(tokenCache)[(email)]).token
		}
		log.info 'Got token.'
		login(token, false)
	}

	void login(String token, boolean bot = true) {
		confirmedBot = bot
		this.token = token
		connectGateway(true)
	}

	void login(String customBotName, Closure requestToken) {
		confirmedBot = true
		File tokenFile = new File(tokenCachePath)
		if (!tokenFile.exists()) JSONUtil.dump(tokenFile, [bots: [:]])
		Map original = (Map<String, Map<String, Map<String, String>>>) JSONUtil.parse(tokenFile)
		if (null == original.bots) original.bots = [:]
		if (null == original.bots[customBotName]) original.bots[customBotName] = [:]
		def x = original.bots[customBotName]
		if (null != x.token) {
			try {
				token = x.token
			} catch (ignored) {
				final newToken = requestToken()
				JSONUtil.dump(tokenFile, original)
				token = newToken
			}
		} else {
			String newToken = requestToken()
			x.token = newToken
			JSONUtil.dump(tokenFile, original)
			token = newToken
		}
		connectGateway(true)
	}

	void login() {
		if (token) login(token)
		else if (email && password) login(email, password)
		else throw new IllegalArgumentException('Can\'t login without credentials')
	}

	void connectGateway(boolean requestGateway = true) {
		if (requestGateway) {
			log.info 'Requesting gateway...'
			final s = new StringBuilder((String) http.jsonGet('gateway').url)
			if (s.charAt(s.length() - 1) != (char) '/') s.append((char) '/')
			s.append('?encoding=json&v=').append(gatewayVersion)
			gateway = s.toString()
		}
		WebSocketClient cl = new WebSocketClient(new SslContextFactory())
		if (null == ws) ws = new WSClient(this)
		log.info 'Starting websocket connection...'
		gatewayClosed = false
		cl.start()
		cl.connect(ws, new URI(gateway), new ClientUpgradeRequest())
	}

	void logout() {
		if (!bot) http.discardPost('auth/logout', [token: token])
		closeGateway()
		ws = null
	}

	void closeGateway() {
		gatewayClosed = true
		if (null != ws) {
			ws.heartbeatThread?.interrupt()
			ws.session?.close(1000, 'Close')
		}
	}

	static String parseEvent(str) {
		def r = str.toString().toUpperCase().replaceAll(/\s+/, '_')
			.replace('CHANGE', 'UPDATE').replaceAll(/^(?!VOICE_)SERVER/, 'GUILD')
		knownEvents.contains(r) ? r : (eventAliases[r] ?: 'UNRECOGNIZED')
	}

	void listenerError(String event, Throwable ex, Closure closure, data) {
		ex.printStackTrace()
		log.info "Ignoring exception from event $event"
	}

	boolean isLoaded() {
		rawToken && ws && ws.loaded && !(null == guildCache || guildCache.isEmpty()) &&
			!(bot && anyUnavailableGuilds())
	}

	boolean anyUnavailableGuilds() {
		for (g in guildCache) if (g.unavailable) return true
		false
	}

	String requestToken(String email, String password) {
		askPool('login') {
			http.jsonPost('auth/login',
				[email: email, password: password]).token
		}
	}

	User getUser() { new User(this, userObject) }

	Region getOptimalRegion() {
		requestRegions().find { it.optimal }
	}

	boolean isPrivateChannel(c) {
		privateChannelCache.containsKey(Snowflake.from(c))
	}

	List<Channel> getPrivateChannels() {
		(List<Channel>) (privateChannelCache?.list() ?: [])
	}

	Map<Snowflake, Channel> getPrivateChannelMap() {
		privateChannelCache?.map() ?: Collections.emptyMap()
	}

	Channel privateChannel(c) { find(privateChannelCache, c) }
	List<Channel> privateChannels(c) { findAll(privateChannelCache, c) }

	Cache<Channel> getUserDmChannelCache() {
		def dlc = new Cache<Channel>()
		for (e in privateChannelCache) if (e.type == 1)
			dlc.put(e.recipientCache.keySet()[0], e)
		dlc
	}

	Map<Snowflake, Channel> getUserDmChannelMap() {
		userDmChannelCache.map()
	}

	Cache<Channel> getDmChannelCache() {
		def dlc = new Cache<Channel>()
		for (e in privateChannelCache) if (e.type == 1) dlc.add(e)
		dlc
	}

	List<Channel> getDmChannels() {
		privateChannels.findAll { it.dm }
	}

	Map<Snowflake, Channel> getDmChannelMap() {
		privateChannelMap.findAll { k, v -> v.dm }
	}

	Channel dmChannel(c) { channels(c).find { it.dm } }
	List<Channel> dmChannels(c) { channels(c).findAll { it.dm } }
	Channel userDmChannel(u) { find(userDmChannelCache, u) }

	List<Channel> getGroups() {
		privateChannels.findAll { it.group }
	}

	Map<Snowflake, Channel> getGroupMap() {
		privateChannelMap.findAll { k, v -> v.group }
	}

	Channel group(c) { channels(c).find { it.group } }
	List<Channel> groups(c) { channels(c).findAll { it.group } }

	List<Channel> getChannels() {
		(List<Channel>) (guildChannels + privateChannels ?: [])
	}

	Map<Snowflake, Channel> getChannelMap() {
		(Map<Snowflake, Channel>) (guildChannelMap + privateChannelMap ?: [:])
	}

	List<Channel> getGuildChannels() {
		def result = new ArrayList<Channel>()
		for (e in guildCache) result.addAll(e.channelCache.list())
		result
	}

	Map<Snowflake, Channel> getGuildChannelMap() {
		def result = new HashMap<Snowflake, Channel>()
		for (e in guildCache) result.putAll(e.channelCache.map())
		result
	}

	Map<Snowflake, Snowflake> getChannelGuildIdMap() {
		def result = new HashMap<Snowflake, Snowflake>()
		for (e in guildCache) for (ce in (Cache<Channel>) e.channels) {
			result.put ce.id, e.id
		}
		result
	}

	Member member(t) { (Member) findNested(guildCache, "memberCache", t) }
	List<Member> members(t) { findAllNested(guildCache, "memberCache", t) }
	Member member(s, u) { this.<Member>find(guildCache[Snowflake.from(s)].memberCache, u) }
	List<Member> members(s, u) { this.<Member>findAll(guildCache[Snowflake.from(s)].memberCache, u) }

	User user(u) { (User) (member(u)?.user ?:
			findNested(privateChannelCache, 'recipientCache', u) ?:
					relation(u)?.user) }
	Relationship relationship(u) { find(relationshipCache, u) }
	Relationship relation(u) { relationship(u) }

	Guild guild(s) { find(guildCache, s) }

	Channel textChannel(...args) { ((List<Channel>) invokeMethod('channels', args)).find { it.text } }
	List<Channel> textChannels(...args) { ((List<Channel>) invokeMethod('channels', args)).findAll { it.text } }
	Channel voiceChannel(...args) { ((List<Channel>) invokeMethod('channels', args)).find { it.voice } }
	List<Channel> voiceChannels(...args) { ((List<Channel>) invokeMethod('channels', args)).findAll { it.voice } }

	Channel channel(c) { guildChannel(c) ?: privateChannel(c) }
	List<Channel> channels(c) { guildChannels(c) ?: privateChannels(c) }
	Channel guildChannel(c) { (Channel) findNested(guildCache, "channelCache", c) }
	List<Channel> guildChannels(c) { findAllNested(guildCache, "channelCache", c) }
	Channel channel(s, c) { find(guildCache[Snowflake.from(s)].channelCache, c) }
	List<Channel> channels(s, c) { findAll(guildCache[Snowflake.from(s)].channelCache, c) }

	Role role(r) { (Role) findNested(guildCache, "roleCache", r) }
	List<Role> roles(r) { findAllNested(guildCache, "roleCache", r) }
	Role role(s, r) { this.<Role>find(guildCache[Snowflake.from(s)].roleCache, r) }
	List<Role> roles(s, r) { this.<Role>findAll(guildCache[Snowflake.from(s)].roleCache, r) }

	Call ongoingCall(c) { calls[Snowflake.from(c)] }

	List<Relationship> getRelationships() { relationshipCache.list() }
	Map<Snowflake, Relationship> getRelationshipMap() { relationshipCache.map() }

	List<Guild> getGuilds() { (List<Guild>) (guildCache?.list() ?: []) }
	Map<Snowflake, Guild> getGuildMap() { (Map<Snowflake, Guild>) (guildCache?.map() ?: [:]) }

	List<Presence> getGlobalPresences() { (List<Presence>) (presenceCache?.list() ?: []) }
	Map<Snowflake, Presence> getGlobalPresenceMap() { (Map<Snowflake, Presence>) (presenceCache?.map() ?: [:]) }

	List<User> getAllUsers() { userMap.values().toList() }
	List<Member> getAllMembers() { (List<Member>) memberMap.values()*.values().flatten().toList() }
	List<Role> getAllRoles() { (List<Role>) roleMap.values().flatten().toList() }
	List<Emoji> getAllEmojis() { (List<Emoji>) emojiIdMap.values().flatten().toList() }

	List<User> getUsers() { allUsers }
	List<Member> getMembers() { allMembers }
	List<Role> getRoles() { allRoles }
	List<Emoji> getEmojis() { allEmojis }

	Map<Snowflake, User> getUserMap() {
		def result = new HashMap<Snowflake, User>()
		for (e in guildCache) {
			def m = e.memberCache
			for (me in m) result.put(me.id, m.get(me.id).user)
		}
		for (e in privateChannelCache) {
			def m = e.recipientCache
			for (me in m) result.put(me.id, m.get(me.id))
		}
		if (null != relationshipCache) for (e in relationshipCache) {
			def r = relationshipCache.get(e.id).user
			result.put(r.id, r)
		}
		result
	}

	Map<Snowflake, Map<Snowflake, Member>> getMemberMap() {
		def doo = new HashMap<Snowflake, Map<Snowflake, Member>>()
		for (e in guilds) doo[e.id] = e.memberMap
		doo
	}

	Map<Snowflake, Role> getRoleMap() {
		def result = new HashMap<Snowflake, Role>()
		for (e in guilds) result.putAll(e.roleMap)
		result
	}

	Map<Snowflake, Emoji> getEmojiIdMap() {
		def result = new HashMap<Snowflake, Emoji>()
		for (e in guilds) result.putAll(e.emojiIdMap)
		result
	}

	Map<String, Emoji> getEmojiNameMap() {
		def result = new HashMap<String, Emoji>()
		for (e in guilds) result.putAll(e.emojiNameMap)
		result
	}

	boolean isVerified() { userObject.verified as boolean }

	def <T> T askPool(String name, String bucket = '$', Closure<T> action) {
		pools[name].ask bucket, action
	}

	def syncGuilds(g) {
		final g1 = g instanceof Collection ?
			g.collect(Snowflake.&from)*.toString().collate(25) :
			[[Snowflake.from(g).toString()]]
		for (c in g1) {
			ws.send op: 12, d: c
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(guilds, String query = '', int limit = 0) {
		final g = guilds instanceof Collection ?
			guilds.collect(Snowflake.&from)*.toString().collate(25) :
			[Snowflake.from(guilds).toString()]
		for (it in g) {
			ws.send op: 8, d: [
				guild_id: it,
				query: query,
				limit: limit
			]
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(guilds, int limit, String query = '') {
		chunkMembersFor(guilds, query, limit)
	}

	String filterMessage(cnt) {
		String a = cnt.toString()
		for (e in messageFilters) {
			if (e.key instanceof Pattern) a = a.invokeMethod('replaceAll', [((Pattern) e.key).pattern(), e.value]).toString()
			else a = a.invokeMethod('replace', [e.key.toString(), e.value]).toString()
		}
		a
	}

	static String getApplicationLink(app, Permissions perms = null) {
		"https://discordapp.com/oauth2/authorize?client_id=${Snowflake.from(app)}&scope=bot" +
			(perms ? "&permissions=$perms.value" : '')
	}

	static String getAppLink(app, Permissions perms = null) { getApplicationLink(app, perms) }
	static String applicationLink(app, Permissions perms = null) { getApplicationLink(app, perms) }
	static String appLink(app, Permissions perms = null) { getApplicationLink(app, perms) }

	void mute(t) {
		if (t.class.array || t instanceof Collection) mutedChannels.addAll(t.collect(Snowflake.&from))
		else mutedChannels.add(Snowflake.from(t))
	}

	void changePresence(Map<String, Object> data) {
		def oldPresence = presences.find { it.id == id }
		def dg = data.game
		def game = dg instanceof Game ? (Game) dg : null == dg ? oldPresence?.game : new Game(this)
		if (dg instanceof String) game.name = dg
		else if (dg instanceof Map) game.fill(dg)
		else if (null != dg) game.name = dg.toString()
		def payload = [
			status: (String) data.status ?: oldPresence?.status ?: 'online',
			game: null == game ? null : [name: game.name, type: game.type],
			since: (int) data.since ?: 0,
			afk: (boolean) data.afk ?: false,
		]
		askPool('changePresence') {
			ws.send(3, payload)
		}

		for (s in guildCache) {
			def presence = new Presence(this)
			presence.user = this
			presence.game = game
			presence.status = (String) payload.status
			presence.guildId = s.id
			s.presenceCache.add(presence)
		}
		def presence = new Presence(this)
		presence.user = this
		presence.game = game
		presence.status = (String) payload.status
		presence.lastModified = System.currentTimeMillis()
		presenceCache.add(presence)
	}

	void status(status) { changePresence(status: status) }
	void play(game) { changePresence(game: game) }
	void playGame(game) { changePresence(game: game) }

	// WEBSOCKET LISTENERS

	void addListener(DiscordRawWSListener listener) { rawListeners.add(listener) }
	void removeListener(DiscordRawWSListener listener) { rawListeners.remove(listener) }

	@Memoized DiscordRawWSListener getCacher() {
		new DiscordRawWSListener() {
			@Override
			@CompileStatic
			void fire(String type, Map<String, Object> d) {
				final g = Snowflake.from(d.guild_id)
				final anyG = null != g
				final gu = anyG ? guildCache[g] : (Guild) null
				final ch = Snowflake.from(d.channel_id)
				switch (type) {
				case 'GUILD_MEMBER_ADD':
					gu.memberCache.add(new Member(Client.this, d))
					gu.memberCount++
					break
				case 'GUILD_MEMBER_REMOVE':
					if (anyG) {
						gu.memberCache.remove(Snowflake.swornString(((Map) d.user).id))
						gu.memberCount--
					}
					break
				case 'GUILD_ROLE_CREATE':
					def r = new Role(Client.this)
					r.guildId = g
					r.fill((Map) d.role)
					gu.roleCache.add(r)
					break
				case 'GUILD_ROLE_DELETE':
					gu.roleCache.remove(Snowflake.swornString(d.role_id))
					break
				case 'CHANNEL_CREATE':
					(anyG ? gu.channelCache : privateChannelCache).add(new Channel(Client.this, d))
					break
				case 'CHANNEL_DELETE':
					(anyG ? gu.channelCache : privateChannelCache).remove(Snowflake.swornString(d.id))
					break
				case 'CHANNEL_UPDATE':
					(anyG ? gu.channelCache : privateChannelCache)[Snowflake.swornString(d.id)].fill(d)
					break
				case 'MESSAGE_CREATE':
					if (!cacheMessages) break
					final m = messages[ch]
					if (null != m) {
						final id = Snowflake.swornString(d.id)
						m[id] = new Message(Client.this, d)
					} else {
						def msg = new Message(Client.this, d)
						def a = new Cache([msg])
						messages[ch] = a
					}
					break
				case 'MESSAGE_UPDATE':
					if (!cacheMessages) break
					final m = messages[ch]
					if (null != m) {
						final id = Snowflake.swornString(d.id)
						final msg = m[id] ?: (m[id] = new Message(Client.this))
						msg.fill(d)
					} else {
						def msg = new Message(Client.this, d)
						def a = new Cache([msg])
						messages[ch] = a
					}
					break
				case 'MESSAGE_DELETE':
					if (!cacheMessages) break
					messages[ch]?.remove(Snowflake.swornString(d.id))
					break
				case 'GUILD_CREATE':
					final guild = guildCache[Snowflake.swornString(d.id)]
					if (null == guild) guildCache.add(new Guild(Client.this, d))
					else guild.fill(d)
					break
				case 'GUILD_DELETE':
					guildCache.remove(Snowflake.swornString(d.id))
					break
				case 'GUILD_MEMBER_UPDATE':
					gu.memberCache[Snowflake.swornString(((Map) d.user).id)]?.fill(d)
					break
				case 'GUILD_ROLE_UPDATE':
					def r = Snowflake.swornString(((Map) d.role).id)
					def rc = gu.roleCache
					final role = rc[r]
					if (null != role) role.fill((Map) d.role)
					else {
						def rol = new Role(Client.this)
						rol.guildId = g
						rol.fill((Map) d.role)
						rc.add(rol)
					}
					break
				case 'GUILD_UPDATE':
					guildCache[Snowflake.swornString(d.id)].fill d
					break
				case 'PRESENCE_UPDATE':
					if (null == g) {
						def m = (Map) d.user
						def i = Snowflake.swornString(m.id)
						if (null == user(i)) for (e in privateChannelCache) {
							def v = e.recipientCache
							if (v.containsKey(i)) v[i] = new User(Client.this, m)
						}
						if (d.status == 'offline') presenceCache.remove(i)
						else presenceCache[i] = new Presence(Client.this, m)
					} else {
						if (guildCache[g].unavailable) break
						def id = Snowflake.swornString(((Map) d.user).id)
						if (((Map) d.user).avatar) {
							def m = guildCache[g].memberCache
							def mem = m[id]
							if (null != mem) mem.fill((Map) d.user)
							else m.add(new Member(Client.this, d))
						}
						def pc = guildCache[g].presenceCache
						if (d.status == 'offline') pc?.remove(id)
						else pc?.add(new Presence(Client.this, d))
					}
					break
				case 'CHANNEL_RECIPIENT_ADD':
					privateChannelCache[ch].recipientCache.add(new User(Client.this, (Map) d.user))
					break
				case 'CHANNEL_RECIPIENT_REMOVE':
					privateChannelCache[ch].recipientCache.remove(Snowflake.swornString(((Map) d.user).id))
					break
				case 'VOICE_STATE_UPDATE':
					def gi = g
					if (!anyG) {
						for (e in guildCache) if (e.channelCache.containsKey(ch)) gi = e.id
					}
					def x = guildCache[gi].voiceStateCache
					def id = Snowflake.swornString(d.id)
					if (null != ch) {
						final it = x[id]
						if (null != it) it.fill d
						else x[id] = new VoiceState(Client.this, d)
					} else x.remove(id)
					break
				case 'GUILD_EMOJIS_UPDATE':
					gu.jsonField('emojis', d.emojis)
					break
				case 'USER_UPDATE':
					userObject.putAll d
					fill d
					break
				case 'GUILD_MEMBERS_CHUNK':
					for (x in (List<Map>) d.members) {
						def mem = new Member(Client.this)
						mem.guildId = g
						mem.fill x
						gu.memberCache.add(mem)
					}
					break
				case 'GUILD_SYNC':
					for (x in (List<Map>) d.members) {
						def mem = new Member(Client.this)
						mem.guildId = g
						mem.fill x
						gu.memberCache.add(mem)
					}
					for (x in (List<Map>) d.presences) {
						def pres = new Presence(Client.this)
						pres.guildId = g
						pres.fill x
						gu.presenceCache.add(pres)
					}
					guildCache[Snowflake.swornString(d.id)].large = (boolean) d.large
					break
				case 'USER_NOTE_UPDATE':
					def n = ((Map<String, Object>) readyData.notes)
					d.note ? n.put((String) d.id, d.note) : n.remove((String) d.id)
					break
				case 'MESSAGE_REACTION_ADD':
					if (!cacheReactions) break
					final mi = Snowflake.swornString(d.message_id)
					def reacs = reactions[mi]
					def reac = new Reaction(Client.this)
					reac.userId = Snowflake.swornString(d.user_id)
					reac.jsonField('emoji', d.emoji)
					if (null == reacs) reactions[mi] = [reac]
					else reacs.add(reac)
					break
				case 'MESSAGE_REACTION_REMOVE':
					if (!cacheReactions) break
					def x = reactions[Snowflake.swornString(d.message_id)]
					if (null == x) break
					int i = 0
					def id = Snowflake.swornString(((Map) d.emoji).id)
					def userId = Snowflake.swornString(d.user_id)
					for (a in x) {
						if (a.userId == userId && a.id == id) {
							x.remove(i)
							break
						}
						i++
					}
					break
				case 'MESSAGE_REACTION_REMOVE_ALL':
					if (!cacheReactions) break
					reactions.remove(Snowflake.swornString(d.message_id))
					break
				case 'RELATIONSHIP_ADD':
					relationshipCache.add(new Relationship(Client.this, d))
					break
				case 'RELATIONSHIP_REMOVE':
					relationshipCache.remove(Snowflake.swornString(d.id))
					break
				case 'CALL_CREATE':
					calls[Snowflake.swornString(d.channel_id)] = new Call(Client.this, d)
					break
				case 'CALL_UPDATE':
					calls[Snowflake.swornString(d.channel_id)].fill d
					break
				case 'CALL_DELETE':
					calls.remove((String) d.channel_id)
					break
				case 'USER_GUILD_SETTINGS_UPDATE':
					def x = userGuildSettingCache[g]
					if (x) x.putAll(d)
					else userGuildSettingCache[g] = d
					break
				case 'USER_SETTINGS_UPDATE':
					((Map) readyData.user_settings) << d
					break
				}
			}
		}
	}

	void removeCacher() { removeListener(cacher) }
	void addCacher() { addListener(cacher) }

	@Memoized DiscordRawWSListener getMemberRequesterOnReady() {
		new DiscordRawWSListener() {
			@CompileStatic
			void fire(String type, Map data) {
				if (type == 'READY') chunkMembersFor(guilds.findAll { it.large })
			}
		}
	}

	def requestMembersOnReady() { addListener(memberRequesterOnReady) }
	def dontRequestMembersOnReady() { removeListener(memberRequesterOnReady) }

	// add this yourself
	void addReconnector() {
		addListener('close') { Map it ->
			if (!gatewayClosed && ((int) it.code) < 4000) ws.reconnect()
		}
	}

	// HTTP REQUESTS

	List<Guild> requestGuilds(boolean checkCache = true) {
		final g = http.jsonGets('users/@me/guilds')
		def result = new ArrayList<Guild>(g.size())
		for (object in g) {
			def gu = new Guild(this, object)
			if (checkCache && !object.unavailable) {
				def cached = guildCache[Snowflake.swornString(object.id)]
				gu.memberCache = cached.memberCache
				gu.channelCache = cached.channelCache
				gu.presenceCache = cached.presenceCache
				gu.voiceStateCache = cached.voiceStateCache
				gu.roleCache = cached.roleCache
				gu.emojiCache = cached.emojiCache
				gu.large = cached.large
			}
			result.add(gu)
		}
		result
	}

	List<Channel> requestPrivateChannels() {
		http.jsonGets('users/@me/channels').collect { new Channel(this, it) }
	}

	void moveMemberVoiceChannel(s, u, vc) {
		editMember(channel_id: Snowflake.from(vc), s, u)
	}

	void editRoles(s, u, Collection r) {
		editMember(roles: r.collect(Snowflake.&from), s, u)
	}

	void addRoles(s, u, Collection r) {
		editRoles(s, u, guildCache[Snowflake.from(s)].memberCache[Snowflake.from(u)].roleIds + r)
	}

	void addRole(s, m, r) {
		http.discardPut("guilds/${Snowflake.from(s)}/members/${Snowflake.from(m)}/roles/${Snowflake.from(r)}")
	}

	void removeRole(s, m, r) {
		http.discardDelete("guilds/${Snowflake.from(s)}/members/${Snowflake.from(m)}/roles/${Snowflake.from(r)}")
	}

	String changeOwnGuildNick(s, String nick) {
		askPool('changeNick') {
			http.jsonPatch("guilds/${Snowflake.from(s)}/members/@me/nick",
				[nick: nick]).nick
		}
	}

	void changeGuildNick(s, m, String nick) {
		editMember(nick: nick, s, m)
	}

	void kick(s, m) {
		http.discardDelete("guilds/${Snowflake.from(s)}/members/${Snowflake.from(m)}")
	}

	List<Guild.Ban> requestBans(s) {
		http.jsonGets("guilds/${Snowflake.from(s)}/bans").collect { new Guild.Ban(this, it) }
	}

	List<Invite> requestGuildInvites(s) {
		http.jsonGets("guilds/${Snowflake.from(s)}/invites").collect { new Invite(this, it) }
	}

	List<Region> requestRegions(s) {
		http.jsonGets("guilds/${Snowflake.from(s)}/regions").collect { new Region(it) }
	}

	List<Integration> requestIntegrations(s) {
		http.jsonGets("guilds/${Snowflake.from(s)}/integrations").collect { new Integration(this, it) }
	}

	Integration createIntegration(s, type, id) {
		new Integration(this, http.jsonPost("guilds/${Snowflake.from(s)}/integrations",
			[type: type.toString(), id: id.toString()]))
	}

	void ban(s, u, int d = 0) {
		http.discardPut("guilds/${Snowflake.from(s)}/bans/${Snowflake.from(u)}?delete-message-days=$d", [:])
	}

	void unban(s, u) {
		http.discardDelete("guilds/${Snowflake.from(s)}/bans/${Snowflake.from(u)}")
	}

	int checkPrune(s, int d) {
		(int) http.jsonGet("guilds/${Snowflake.from(s)}/prune?days=$d").pruned
	}

	int prune(s, int d) {
		(int) http.jsonPost("guilds/${Snowflake.from(s)}/prune?days=$d").pruned
	}

	Role createRole(Map data, s) {
		if (data.color instanceof Color) data.color = ((Color) data.color).RGB
		if (data.permissions instanceof Permissions) {
			final perms = (Permissions) data.permissions
			data.permissions = permissionsAreStrings ? perms.toString() : perms.toLong()
		}
		Role createdRole = new Role(this, http.jsonPost("guilds/${Snowflake.from(s)}/roles", [:]))
		editRole(data, s, createdRole)
	}

	Role editRole(Map data, s, r) {
		if (data.color instanceof Color) data.color = ((Color) data.color).RGB
		if (data.permissions instanceof Permissions) {
			final perms = (Permissions) data.permissions
			data.permissions = permissionsAreStrings ? perms.toString() : perms.toLong()
		}
		new Role(this, http.jsonPatch("guilds/${Snowflake.from(s)}/roles/${Snowflake.from(r)}", data))
	}

	void deleteRole(s, r) {
		http.discardDelete("guilds/${Snowflake.from(s)}/roles/${Snowflake.from(r)}")
	}
	
	List<Role> editRolePositions(Map mods, s) {
		http.jsonPatches("guilds/${Snowflake.from(s)}/roles", mods.collect { k, v ->
			[id: Snowflake.from(k), position: v]}).collect { new Role(this, it + [guild_id: (Object) Snowflake.from(s)]) }
	}
	
	List<Channel> editChannelPositions(Map mods, s) {
		def sn = Snowflake.from(s)
		http.jsonPatches("guilds/$sn/channels", mods.collect { k, v ->
			[id: Snowflake.from(k), position: v]}).collect {
			def ch = new Channel(this, it)
			ch.guildId = sn
			ch
		}
	}

	List<Webhook> requestGuildWebhooks(s) {
		http.jsonGets("guilds/${s}/webhooks").collect { new Webhook(this, it) }
	}

	private static final MathContext ceilmathcontext = new MathContext(0, RoundingMode.CEILING)
	private static final BigDecimal ceil(BigDecimal dec) { dec.round(ceilmathcontext) }

	List<Member> requestMembers(s, int max=1000, boolean updateCache=true) {
		def i = Snowflake.from(s)
		final members = http.jsonGets("guilds/$i/members?limit=${max}")
		if (max > 1000) {
			for (int m = 1; m < ceil(max / 1000).intValue() - 1; m++) {
				members.addAll http.jsonGets("guilds/$i/members?after=${(m * 1000) + 1}&limit=1000")
			}
			members.addAll http.jsonGets("guilds/$i/members?after=${((ceil(max / 1000).intValue() - 1) * 1000)+1}&limit=1000")
		}
		def mems = new ArrayList<Member>(members.size())
		for (it in members) {
			def r = new Member(this)
			r.guildId = i
			r.fill(it)
			mems.add(r)
		}
		if (updateCache) {
			def cac = guildCache[i]
			cac.memberCache = new Cache(mems)
			cac.memberCount = members.size()
		}
		mems
	}

	Member requestMember(s, m) { new Member(this,
		http.jsonGet("guilds/${Snowflake.from(s)}/members/${Snowflake.from(m)}")) }

	List<Emoji> requestEmojis(s) { http.jsonGets("guilds/${Snowflake.from(s)}/emojis")
		.collect { new Emoji(this, it) } }

	Emoji createEmoji(Map data, s) {
		new Emoji(this, http.jsonPost("guilds/${Snowflake.from(s)}/emojis",
			patchData(data, 'image')))
	}

	Emoji editEmoji(Map data, s, emoji) {
		new Emoji(this, http.jsonPatch("guilds/${Snowflake.from(s)}/emojis/${Snowflake.from(emoji)}", data))
	}

	Webhook requestGuildWebhook(s, w) {
		new Webhook(this, http.jsonGet("guilds/${Snowflake.from(s)}/webhooks/${Snowflake.from(w)}"))
	}

	Invite acceptInvite(i) {
		new Invite(this, http.jsonPost("invite/${Invite.parseId(i)}", [:]))
	}

	Invite requestInvite(i) {
		new Invite(this, http.jsonGet("invite/${Invite.parseId(i)}"))
	}

	Invite createInvite(Map data = [:], c) {
		new Invite(this, http.jsonPost("channels/${Snowflake.from(c)}/invites", data))
	}
	
	void deleteInvite(i) {
		http.discardDelete("invite/${Invite.parseId(i)}")
	}

	Guild.Embed requestEmbed(s) { new Guild.Embed(this, http.jsonGet("guilds/${Snowflake.from(s)}/embed")) }

	Guild.Embed editEmbed(Map data, s, e) { new Guild.Embed(this,
		http.jsonPatch("guilds/${Snowflake.from(s)}/embed", data)) }

	List<Connection> requestConnections() {
		http.jsonGets('users/@me/connections').collect { new Connection(this, it) }
	}

	User edit(Map data) {
		Map map = [avatar: user.avatarHash, email: this?.email,
			password: this?.password, username: user.username]
		Map response = http.jsonPatch('users/@me', map <<
			patchData(data))
		email = response.email
		password = (data.new_password != null && data.new_password instanceof String) ? data.new_password : password
		token = ((String) response.token)
			.replaceFirst(Pattern.quote(tokenPrefix), '')
			.trim()
		userObject.email = response.email
		userObject.verified = response.verified
		new User(this, response)
	}

	Profile requestProfile(a) {
		new Profile(this, http.jsonGet("users/${Snowflake.from(a)}/profile"))
	}

	Application requestApplication() {
		new Application(this, http.jsonGet('oauth2/applications/@me'))
	}

	List<Application> requestApplications() {
		http.jsonGets('oauth2/applications').collect { new Application(this, it) }
	}

	Application requestApplication(a) {
		new Application(this, http.jsonGet("oauth2/applications/${Snowflake.from(a)}"))
	}

	Application editApplication(Map data, a) {
		new Application(this, http.jsonPut("oauth2/applications/${Snowflake.from(a)}", patchData(data)))
	}

	void deleteApplication(a) {
		http.discardDelete("oauth2/applications/${Snowflake.from(a)}")
	}

	Application createApplication(Map data) {
		Map map = [icon: null, description: '', redirect_uris: [], name: '']
		new Application(this, http.jsonPost('oauth2/applications', map <<
			patchData(data)))
	}

	Map createApplicationBotAccount(a, String oldAccountToken = null) {
		http.jsonPost("oauth2/applications/${Snowflake.from(a)}/bot",
			(oldAccountToken == null) ? [:] : [token: oldAccountToken])
	}

	List<Region> requestRegions() {
		http.jsonGets('voice/regions').collect { new Region(it) }
	}

	List<User> queueUsers(String query, int limit=25) {
		http.jsonGets("users?q=${URLEncoder.encode(query, 'UTF-8')}&limit=$limit")
			.collect { new User(this, it) }
	}

	User requestUser(i) { new User(this, http.jsonGet("users/${Snowflake.from(i)}")) }
	Guild requestGuild(i) { new Guild(this, http.jsonGet("guilds/${Snowflake.from(i)}")) }
	Channel requestChannel(i) { new Channel(this, http.jsonGet("channels/${Snowflake.from(i)}")) }

	Guild editGuild(Map data, s) {
		new Guild(this, http.jsonPatch("guilds/${Snowflake.from(s)}",
			patchData(data)))
	}

	void leaveGuild(s) {
		http.discardDelete("users/@me/guilds/${Snowflake.from(s)}", [:])
	}

	void deleteGuild(s) {
		http.discardDelete("guilds/${Snowflake.from(s)}")
	}

	Channel createTextChannel(s, String name) {
		new Channel(this, http.jsonPost("guilds/${Snowflake.from(s)}/channels",
			[name: name, type: 0]))
	}

	Channel createVoiceChannel(s, String name) {
		new Channel(this, http.jsonPost("guilds/${Snowflake.from(s)}/channels",
			[name: name, type: 2]))
	}
	
	Channel createChannel(Map data = [:], s) {
		new Channel(this, http.jsonPost("guilds/${Snowflake.from(s)}/channels", data))
	}

	Channel requestGuildChannel(s, c) {
		new Channel(this, http.jsonGet("guilds/${Snowflake.from(s)}/channels/${Snowflake.from(c)}"))
	}

	List<Channel> requestGuildChannels(s) {
		http.jsonGets("guilds/${Snowflake.from(s)}/channels").collect { new Channel(this, it) }
	}

	Integration editIntegration(Map data, s, i) {
		new Integration(this,
			http.jsonPatch("guilds/${Snowflake.from(s)}/integrations/${Snowflake.from(i)}", data))
	}

	void deleteIntegration(s, i) {
		http.discardDelete("guilds/${Snowflake.from(s)}/integrations/${Snowflake.from(i)}")
	}

	void syncIntegration(s, i) {
		http.discardPost("guilds/${Snowflake.from(s)}/integrations/${Snowflake.from(i)}/sync")
	}

	void editMember(Map data, s, m) {
		final j = Snowflake.from(s).toString()
		askPool('editMembers', j) {
			http.discardPatch("guilds/$j/members/${Snowflake.from(m)}", data)
		}
	}

	void addChannelRecipient(c, u) {
		http.discardPut("channels/${Snowflake.from(c)}/recipients/${Snowflake.from(u)}")
	}

	void removeChannelRecipient(c, u) {
		http.discardDelete("channels/${Snowflake.from(c)}/recipients/${Snowflake.from(u)}")
	}

	void addRelationship(u, type) {
		http.discardPut("users/@me/relationships/${Snowflake.from(u)}", [type: type.invokeMethod('toInteger', null)])
	}

	void removeRelationship(u) {
		http.discardDelete("users/@me/relationships/${Snowflake.from(u)}")
	}

	List<Invite> requestChannelInvites(c) {
		http.jsonGets("channels/${Snowflake.from(c)}/invites").collect { new Invite(this, it) }
	}

	void startTyping(c) {
		http.discardPost("channels/${Snowflake.from(c)}/typing")
	}

	void deleteChannel(c) {
		http.discardDelete("channels/${Snowflake.from(c)}")
	}

	Channel editChannel(Map data, c) {
		new Channel(this, http.jsonPatch("channels/${Snowflake.from(c)}", patchData(data)))
	}

	void editChannelOverwrite(Map data, c, o) {
		final i = Snowflake.from(o)
		String type = (String) data.type ?: (role(i) ? 'role' : 'member')
		int allowBytes = null == data.allow ? 0 : (int) data.allow.invokeMethod('toInteger', null)
		int denyBytes = null == data.deny ? 0 : (int) data.deny.invokeMethod('toInteger', null)
		http.discardPut("channels/${Snowflake.from(c)}/permissions/${i}",
			[allow: allowBytes, deny: denyBytes, id: i, type: type])
	}

	void deleteChannelOverwrite(c, o) {
		http.discardDelete("channels/${Snowflake.from(c)}/permissions/${Snowflake.from(o)}")
	}

	Webhook createWebhook(Map data = [:], c) {
		new Webhook(this, http.jsonPost("channels/${Snowflake.from(c)}/webhooks",
			patchData(data)))
	}

	List<Webhook> requestChannelWebhooks(c) {
		http.jsonGets("channels/${Snowflake.from(c)}/webhooks").collect { new Webhook(this, it) }
	}

	Webhook requestWebhook(w) {
		new Webhook(this, http.jsonGet("webhooks/${Snowflake.from(w)}"))
	}

	Webhook requestWebhook(id, token) {
		new Webhook(this, http.jsonGet("webhooks/${Snowflake.from(id)}/$token"))
	}

	void checkMessageData(Map data) {
		if (data.containsKey('content')) {
			def s = data.content.toString()
			s = filterMessage(s)
			if (!s || s.size() > 2000) throw new MessageInvalidException(s)
			data.content = s
		}
	}

	Message sendMessage(Map data, c) {
		checkMessageData(data)
		boolean isWebhook = MiscUtil.defaultValueOnException new Closure(this) {
			@CompileDynamic
			def call() {
				data.webhook && c?.id && c?.token
			}
		}
		Closure<Message> clos = {
			new Message(this, http.jsonPost(isWebhook ?
				"webhooks/${Snowflake.from(c)}/${c instanceof Map ? c.token : ((Webhook) c).token}" :
				"channels/${Snowflake.from(c)}/messages", data))
		}
		isWebhook ? clos() : poolMessage(c, clos)
	}

	Message sendMessage(c, content, tts = false) { sendMessage(c, content: content, tts: tts) }

	void discardSendMessage(Map data, c) {
		checkMessageData(data)
		boolean isWebhook = MiscUtil.defaultValueOnException new Closure(this) {
			@CompileDynamic
			def call() {
				data.webhook && c?.id && c?.token
			}
		}
		final clos = {
			http.discardPost(isWebhook ?
					"webhooks/${Snowflake.from(c)}/${c instanceof Map ? c.token : ((Webhook) c).token}" :
					"channels/${Snowflake.from(c)}/messages", data)
		}
		isWebhook ? clos() : poolMessage(c, clos)
	}

	void discardSendMessage(c, content, tts = false) { discardSendMessage(c, content: content, tts: tts) }

	def <T> T poolMessage(c, Closure<T> closure) {
		askPool('sendMessages', getChannelQueueName(c), closure)
	}

	Message editMessage(Map data, c, m) {
		checkMessageData(data)
		poolMessage(c) { // that's right, they're in the same bucket
			new Message(this, http.jsonPatch("channels/${Snowflake.from(c)}/messages/${Snowflake.from(m)}", data))
		}
	}

	void discardEditMessage(Map data, c, m) {
		checkMessageData(data)
		poolMessage(c) { // that's right, they're in the same bucket
			http.discardPatch("channels/${Snowflake.from(c)}/messages/${Snowflake.from(m)}", data)
		}
	}

	void deleteMessage(c, m) {
		askPool('deleteMessages',
			getChannelQueueName(c)) { http.discardDelete("channels/${Snowflake.from(c)}/messages/${Snowflake.from(m)}") }
	}

	Map sendFileRaw(Map data = [:], c, file) {
		boolean isWebhook = MiscUtil.defaultValueOnException new Closure(this) {
			@CompileDynamic
			def call() {
				data.webhook && c?.id && c?.token
			}
		}
		File f = null
		String fn = null
		byte[] bytes = null
		if (file instanceof File) {
			if (data.filename) {
				bytes = ((File) file).bytes
				fn = (String) data.filename
			} else f = (File) file
		} else {
			bytes = ConversionUtil.getBytes(file)
			if (!data.filename) throw new IllegalArgumentException("Tried to send non-file class ${file.class} and gave no filename")
			fn = (String) data.filename
		}
		def url = http.baseUrl + (isWebhook ?
			"webhooks/${Snowflake.from(c)}/${((Webhook) c).token}" :
			"channels/${Snowflake.from(c)}/messages")
		def aa = Unirest.post(url)
			.header('Authorization', token)
			.header('User-Agent', fullUserAgent)
			.field('content', data.content == null ? '' : data.content.toString())
			.field('tts', data.tts as boolean)
		if (null != f) {
			aa = aa.field('file', f)
		} else {
			aa = aa.field('file', bytes, fn)
		}
		Closure<Map> clos = {
			(Map) JSONUtil.parse(aa.asString().body)
		}
		isWebhook ? clos() : poolMessage(c, clos)
	}

	Message sendFile(Map data, c, implicatedFile, filename) {
		def file
		if (implicatedFile.class in [File, String]) file = implicatedFile as File
		else file = ConversionUtil.getBytes(implicatedFile)
		new Message(this, sendFileRaw((filename ? [filename: filename] : [:]) << data, c, file))
	}

	Message sendFile(Map data, c, implicatedFile) {
		sendFile(data, c, implicatedFile, null)
	}

	Message sendFile(c, implicatedFile, filename) {
		sendFile([:], c, implicatedFile, filename)
	}

	Message sendFile(c, implicatedFile) {
		sendFile([:], c, implicatedFile, null)
	}

	Message requestMessage(c, ms, boolean addToCache = true) {
		def ch = Snowflake.from(c)
		def m = new Message(this,
			http.jsonGet("channels/$ch/messages/${Snowflake.from(ms)}"))
		if (addToCache) {
			if (messages[ch]) messages[ch].add(m)
			else messages[ch] = new Cache<>([m])
		}
		m
	}

	Message message(c, m, boolean request = true) {
		final ci = Snowflake.from(c)
		final mi = Snowflake.from(m)
		final chancache = messages[ci]
		final cache = null == chancache ? (Message) null : chancache[mi]
		if (null != cache) cache
		else if (request) requestMessage(ci, mi)
		else null
	}

	String getChannelQueueName(c) {
		guildChannel(Snowflake.from(c)) ?: 'dm'
	}

	def pinMessage(c, m) {
		http.discardPut("channels/${Snowflake.from(c)}/pins/${Snowflake.from(m)}")
	}

	def unpinMessage(c, m) {
		http.discardDelete("channels/${Snowflake.from(c)}/pins/${Snowflake.from(m)}")
	}

	Collection<Message> requestPinnedMessages(c) {
		http.jsonGets("channels/${Snowflake.from(c)}/pins").collect { new Message(this, it) }
	}

	void reactToMessage(c, m, e) {
		http.discardPut("channels/${Snowflake.from(c)}/messages/${Snowflake.from(m)}/" +
			"reactions/${translateEmoji(e)}/@me")
	}

	void unreactToMessage(c, m, e, u = '@me') {
		http.discardDelete("channels/{from(c)}/messages/${Snowflake.from(m)}/" +
			"reactions/${translateEmoji(e)}/${Snowflake.from(u)}")
	}

	List<User> requestReactors(c, m, e, int limit = 100) {
		http.jsonGets("channels/${Snowflake.from(c)}/messages/${Snowflake.from(m)}/reactions/" +
			translateEmoji(e) + "?limit=$limit").collect { new User(this, it) }
	}

	String translateEmoji(emoji, s = null) {
		if (emoji ==~ /\d+/) {
			translateEmoji(emojis.find { it.id == emoji })
		} else if (emoji instanceof Emoji) {
			"$emoji.name:$emoji.id"
		} else if (emoji ==~ /\w+/) {
			def i = ((List<Emoji>) (guild(s)?.emojis ?: []) + (List<Emoji>) (emojis - guild(s)?.emojis)
				).find { it.name == emoji }?.id
			i ? "$emoji:$i" : ":$emoji:"
		} else {
			emoji
		}
	}

	List<Message> requestChannelLogs(c, int max = 100, boundary = null,
		String boundaryType = 'before') {
		final cached = messages[Snowflake.from(c)]
		if (!boundary && cached?.size() > max) return cached.values().sort {
			it.id }[-1..-max]
		def l = directRequestChannelLogs(c, max, boundary, boundaryType)
			.toSorted(new Comparator<Map<String, Object>>() {
			@Override
			int compare(Map<String, Object> o1, Map<String, Object> o2) {
				ConversionUtil.fromJsonDate((String) o2.timestamp).compareTo(
						ConversionUtil.fromJsonDate((String) o1.timestamp))
			}
		}).collect { new Message(this, it) }
		if (boundaryType in ['around', 'after']) l = l.reverse()
		if (cached) for (a in l) messages[Snowflake.from(c)].add(a)
		else messages[Snowflake.from(c)] = new Cache<Message>(l)
		l
	}

	List<Message> forceRequestChannelLogs(c, int m = 100, b = null,
		String bt = 'before') { directRequestChannelLogs(c, m, b, bt).collect { new Message(this, it) } }

	private static final MathContext floormathcontext = new MathContext(0, RoundingMode.FLOOR)
	private static final BigDecimal floor(BigDecimal dec) { dec.round(floormathcontext) }

	// after and around sorted old-new
	// before sorted new-old

	@SuppressWarnings("GroovyAssignabilityCheck")
	List<Map<String, Object>> directRequestChannelLogs(c, int max, boundary = null, String boundaryType = 'before') {
		Map<String, Object> params = [limit: (Object) Math.min(100, max)]
		if (boundary) {
			if (boundaryType && boundaryType != 'before' && boundaryType != 'after' && boundaryType != 'around')
				throw new IllegalArgumentException('Boundary type has to be before, after or around')
			params.put boundaryType ?: 'before', Snowflake.from(boundary)
		}
		List<Map<String, Object>> messages = directRequestChannelLogs(params, c)
		if (max > 100) {
			if (boundaryType == 'after') {
				for (int m = 1; m < floor(max / 100).intValue(); m++) {
					messages.addAll directRequestChannelLogs(c,
							after: messages.last().id, limit: 100)
				}
				if (max % 100 != 0) messages.addAll directRequestChannelLogs(c,
						after: messages.last().id, limit: max % 100)
			} else if (boundaryType == 'around') {
				int age = max - 100
				int af = age.intdiv(2).intValue()
				int bef = age.intdiv(2).intValue() + (age % 2)
				for (int m = 1; m < floor(bef / 100).intValue(); m++) {
					messages.addAll 0, directRequestChannelLogs(c, before: messages.first().id,
							limit: 100).reverse()
				}
				if (bef % 100 != 0) messages.addAll 0, directRequestChannelLogs(c, before: messages.first().id,
						limit: bef % 100).reverse()
				for (int m = 1; m < floor(af / 100).intValue(); m++) {
					messages.addAll directRequestChannelLogs((Map<String, Object>) [after: messages.last().id, limit: 100], c)
				}
				if (af % 100 != 0) messages.addAll directRequestChannelLogs([before: messages.last().id,
						limit: af % 100], c)
			} else {
				for (int m = 1; m < floor(max / 100).intValue(); m++) {
					messages.addAll directRequestChannelLogs(c,
							before: messages.last().id, limit: 100)
				}
				if (max % 100) messages.addAll directRequestChannelLogs(c,
						before: messages.last().id, limit: max % 100)
			}
		}
		messages
	}

	List<Map<String, Object>> directRequestChannelLogs(Map<String, Object> data = [:], c) {
		String parameters = data ? '?' + data.collect { k, v ->
			URLEncoder.encode(k.toString(), 'UTF-8') + '=' + URLEncoder.encode(v.toString(), 'UTF-8')
		}.join('&') : ''
		http.jsonGets("channels/${Snowflake.from(c)}/messages$parameters")
	}

	def bulkDeleteMessages(c, Collection ids) {
		askPool('bulkDeleteMessages') {
			http.discardPost("channels/${Snowflake.from(c)}/messages/bulk-delete",
				[messages: ids.collect { Snowflake.from(it) }])
		}
	}

	Webhook editWebhook(Map data, w) {
		new Webhook(this, http.jsonPatch("webhooks/${Snowflake.from(w)}",
			patchData(data)))
	}

	void deleteWebhook(w) {
		http.discardDelete("webhooks/${Snowflake.from(w)}")
	}

	Channel createPrivateChannel(u) {
		new Channel(this, http.jsonPost('users/@me/channels',
			[recipient_id: Snowflake.from(u)]))
	}
}
