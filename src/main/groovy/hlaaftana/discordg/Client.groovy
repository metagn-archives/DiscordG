package hlaaftana.discordg

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.body.MultipartBody
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.collections.DiscordListCache

import hlaaftana.discordg.exceptions.MessageInvalidException
import static hlaaftana.discordg.logic.ActionPool.create as newPool
import hlaaftana.discordg.logic.*
import hlaaftana.discordg.net.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.objects.*

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
	static List<String> knownEvents = ['INITIAL_GUILD_CREATE', 'UNRECOGINZED', 'ALL'] + knownDiscordEvents

	String customUserAgent = ''
	String getFullUserAgent(){ "$DiscordG.USER_AGENT $customUserAgent" }

	String tokenPrefix
	String rawToken
	String email
	String getEmail(){ userObject.getOrDefault('email', this.@email) }
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
	int gatewayVersion = 6
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
	
	Log log
	Map<String, Object> fields = [:]
	Map<String, Object> extraIdentifyData = [:]
	Set<String> mutedChannels = []
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
	@Delegate(excludes = ['getClass', 'equals']) ListenerSystem listenerSystem = new ParentListenerSystem(this)
	HTTPClient http
	WSClient ws
	Cache<String, Object> readyData = Cache.empty(this)
	Cache<String, DiscordListCache<Message>> messages = Cache.empty(this)
	Cache<String, List<Map<String, Object>>> reactions = Cache.empty(this)
	Cache<String, Map<String, Object>> calls = Cache.empty(this)
	DiscordListCache<Guild> guildCache
	DiscordListCache<Presence> presenceCache
	DiscordListCache<Channel> privateChannelCache
	DiscordListCache<Relationship> relationshipCache
	Cache<String, Map<String, Object>> userGuildSettingCache
	Map<String, Object> userObject
	String gateway
	String sessionId
	boolean gatewayClosed

	Client(Map config = [:]){
		super(null, [:])
		http = new HTTPClient(this)
		client = this

		config.each { k, v ->
			this[k.toString()] = v
		}

		log = new Log(logName)

		addCacher()

		if (requestMembersOnReady) requestMembersOnReady()
	}

	String getToken(){
		tokenPrefix ? "$tokenPrefix $rawToken" : rawToken
	}

	void setToken(String newToken){
		rawToken = newToken
	}
	
	void setConfirmedBot(boolean ass){
		if (ass) tokenPrefix = 'Bot'
		else tokenPrefix = ''
		this.@confirmedBot = ass
	}
	
	boolean isBot(){
		boolean ass = confirmedBot || object.bot
		if (ass) tokenPrefix = 'Bot'
		else tokenPrefix = ''
		ass
	}

	WSClient getWebSocketClient(){ ws }

	def blacklist(event) { client.eventBlacklist.add(parseEvent(event)) }
	def whitelist(event) { client.eventWhitelist.add(parseEvent(event)) }

	void login(String email, String password, boolean threaded=true){
		Closure a = {
			log.info 'Getting token...'
			this.email = email
			this.password = password
			File tokenCache = new File(tokenCachePath)
			if (!cacheTokens){
				token = requestToken(email, password)
			}else{
				try{
					token = ((Map) JSONUtil.parse(tokenCache)[(email)]).token
				}catch (ignored){
					JSONUtil.modify(tokenCache,
						[(email): [token: requestToken(email, password)]])
					token = ((Map) JSONUtil.parse(tokenCache)[(email)]).token
				}
			}
			log.info 'Got token.'
			login(token.toString(), false, false)
		}
		if (threaded) Thread.start(a)
		else a()
	}

	void login(String token, boolean bot = true, boolean threaded=true){
		Closure a = {
			confirmedBot = bot
			this.token = token
			connectGateway(true, false)
		}
		if (threaded) Thread.start(a)
		else a()
	}

	void login(String customBotName, boolean threaded = true, Closure requestToken){
		Closure a = {
			confirmedBot = true
			File tokenFile = new File(tokenCachePath)
			if (!tokenFile.exists()){
				JSONUtil.dump(tokenFile, [bots: [:]])
			}
			Map original = (Map<String, Map<String, Map<String, String>>>) JSONUtil.parse(tokenFile)
			if (!original.bots) original.bots = [:]
			if (!original.bots[customBotName]) original.bots[customBotName] = [:]
			def x = (Map<String, String>) original.bots[customBotName]
			if (x.token){
				try{
					token = x.token
				}catch (ignored){
					String newToken = requestToken()
					JSONUtil.dump(tokenFile, original)
					token = newToken
				}
			}else{
				String newToken = requestToken()
				x.token = newToken
				JSONUtil.dump(tokenFile, original)
				token = newToken
			}
			connectGateway(true, false)
		}
		if (threaded) Thread.start(a)
		else a()
	}

	void login(boolean threaded = true){
		if (token) login(token, threaded)
		else if (email && password) login(email, password, threaded)
		else throw new IllegalArgumentException('Can\'t login without credentials')
	}

	void connectGateway(boolean requestGateway = true, boolean threaded = true){
		Closure a = {
			if (requestGateway){
				log.info 'Requesting gateway...'
				gateway = http.jsonGet('gateway').url
				if (!gateway.endsWith('/')) gateway += '/'
				gateway += "?encoding=json&v=$gatewayVersion"
			}
			WebSocketClient cl = new WebSocketClient(new SslContextFactory())
			if (!ws) ws = new WSClient(this)
			log.info 'Starting websocket connection...'
			gatewayClosed = false
			cl.start()
			cl.connect(ws, new URI(gateway), new ClientUpgradeRequest())
		}
		askPool('connect'){
			if (threaded) Thread.start(a)
			else a()
		}
	}

	void logout(boolean exit = false){
		if (!bot) http.post('auth/logout', ['token': token])
		closeGateway(false)
		ws = null
		if (exit) System.exit(0)
	}

	void closeGateway(boolean threaded = true){
		Closure a = {
			gatewayClosed = true
			if (ws){
				ws.keepAliveThread?.interrupt()
				ws.session?.close(1000, 'Close')
			}
		}
		if (threaded){ Thread.start(a) }
		else { a() }
	}

	static String parseEvent(str){
		def r = str.toString().toUpperCase().replaceAll(/\s+/, '_')
			.replace('CHANGE', 'UPDATE').replaceAll(/^(?!VOICE_)GUILD/, 'GUILD')
		knownEvents.contains(r) ? r : (eventAliases[r] ?: 'UNRECOGNIZED')
	}

	def listenerError(String event, Throwable ex, Closure closure, data){
		ex.printStackTrace()
		log.info "Ignoring exception from event $event"
	}

	def propertyMissing(String name){
		if (fields.containsKey(name)) fields[name]
		else throw new MissingPropertyException(name, Client)
	}

	def propertyMissing(String name, value){
		if (fields.containsKey(name)) fields.put name, value
		else throw new MissingPropertyException(name, Client)
	}

	boolean isLoaded(){
		rawToken && ws && ws.loaded && (null != guildCache && !guildCache.isEmpty()) &&
			(!bot || !anyUnavailableGuilds())
	}

	boolean anyUnavailableGuilds() {
		for (g in guildCache) if (g.value.unavailable) return true
		false
	}

	String requestToken(String email, String password){
		askPool('login'){
			http.jsonPost('auth/login',
				['email': email, 'password': password]).token
		}
	}

	User getUser(){ new User(this, userObject) }

	Region getOptimalRegion(){
		requestRegions().find { it.optimal }
	}

	boolean isPrivateChannel(c){
		privateChannelCache.containsKey(id(c))
	}

	List<Channel> getPrivateChannels(){
		(List<Channel>) (privateChannelCache?.list() ?: [])
	}

	Map<String, Channel> getPrivateChannelMap(){
		(Map<String, Channel>) (privateChannelCache?.map() ?: [:])
	}

	Channel privateChannel(c){ find(privateChannelCache, c) }
	List<Channel> privateChannels(c){ findAll(privateChannelCache, c) }

	DiscordListCache<Channel> getUserDmChannelCache(){
		DiscordListCache dlc = new DiscordListCache([], this, Channel)
		for (e in privateChannelCache) if (e.value.type == 1)
			dlc.put(((DiscordListCache<User>) e.value.recipients).keySet()[0], e.value)
		dlc
	}

	Map<String, Channel> getUserDmChannelMap(){
		userDmChannelCache.map()
	}

	DiscordListCache<Channel> getDmChannelCache(){
		DiscordListCache dlc = new DiscordListCache([], this, Channel)
		for (e in privateChannelCache) if (e.value.type == 1) dlc.add(e.value)
		dlc
	}

	List<Channel> getDmChannels(){
		privateChannels.findAll { it.dm }
	}

	Map<String, Channel> getDmChannelMap(){
		privateChannelMap.findAll { k, v -> v.dm }
	}

	Channel dmChannel(c){ channels(c).find { it.dm } }
	List<Channel> dmChannels(c){ channels(c).findAll { it.dm } }
	Channel userDmChannel(u){ find(userDmChannelCache, u) }

	List<Channel> getGroups(){
		privateChannels.findAll { it.group }
	}

	Map<String, Channel> getGroupMap(){
		privateChannelMap.findAll { k, v -> v.group }
	}

	Channel group(c){ channels(c).find { it.group } }
	List<Channel> groups(c){ channels(c).findAll { it.group } }

	List<Channel> getChannels(){
		(List<Channel>) (guildChannels + privateChannels ?: [])
	}

	Map<String, Channel> getChannelMap(){
		(Map<String, Channel>) (guildChannelMap + privateChannelMap ?: [:])
	}

	List<Channel> getGuildChannels(){
		def result = new ArrayList<Channel>()
		for (e in guildCache) {
			def c = (DiscordListCache<Channel>) e.value.channels
			result.addAll(c.list())
		}
		result
	}

	Map<String, Channel> getGuildChannelMap(){
		def result = new HashMap<String, Channel>()
		for (e in guildCache) {
			def c = (DiscordListCache<Channel>) e.value.channels
			result.putAll(c.map())
		}
		result
	}

	Map<String, String> getChannelGuildIdMap(){
		def result = new HashMap<String, String>()
		for (e in guildCache) for (ce in (DiscordListCache<Channel>) e.value.channels) {
			result.put ce.key, e.key
		}
		result
	}

	Member member(t){ (Member) findNested(guildCache, "members", t) }
	List<Member> members(t){ findAllNested(guildCache, "members", t) }
	Member member(s, u){ find((DiscordListCache<Member>) guildCache[id(s)].members, u) }
	List<Member> members(s, u){ findAll((DiscordListCache<Member>) guildCache[id(s)].members, u) }

	User user(u){ (User) (member(u)?.user ?:
			findNested(privateChannelCache, 'recipients', u) ?:
					relation(u)?.user) }
	Relationship relationship(u){ find(relationshipCache, u) }
	Relationship relation(u){ relationship(u) }

	Guild guild(s){ find(guildCache, s) }

	Channel textChannel(...args){ ((List<Channel>) invokeMethod('channels', args)).find { it.text } }
	List<Channel> textChannels(...args){ ((List<Channel>) invokeMethod('channels', args)).findAll { it.text } }
	Channel voiceChannel(...args){ ((List<Channel>) invokeMethod('channels', args)).find { it.voice } }
	List<Channel> voiceChannels(...args){ ((List<Channel>) invokeMethod('channels', args)).findAll { it.voice } }

	Channel channel(c){ guildChannel(c) ?: privateChannel(c) }
	List<Channel> channels(c){ guildChannels(c) ?: privateChannels(c) }
	Channel guildChannel(c){ (Channel) findNested(guildCache, "channels", c) }
	List<Channel> guildChannels(c){ findAllNested(guildCache, "channels", c) }
	Channel channel(s, c){ find((DiscordListCache<Channel>) guildCache[id(s)].channels, c) }
	List<Channel> channels(s, c){ findAll((DiscordListCache<Channel>) guildCache[id(s)].channels, c) }

	Role role(r){ (Role) findNested(guildCache, "roles", r) }
	List<Role> roles(r){ findAllNested(guildCache, "roles", r) }
	Role role(s, r){ find((DiscordListCache<Role>) guildCache[id(s)].roles, r) }
	List<Role> roles(s, r){ findAll((DiscordListCache<Role>) guildCache[id(s)].roles, r) }

	Call ongoingCall(c){ calls[id(c)] ? new Call(this, calls[id(c)]) : null }

	List<Relationship> getRelationships(){ relationshipCache.list() }
	Map<String, Relationship> getRelationshipMap(){ relationshipCache.map() }

	List<Guild> getGuilds() { (List<Guild>) (guildCache?.list() ?: []) }
	Map<String, Guild> getGuildMap(){ (Map<String, Guild>) (guildCache?.map() ?: [:]) }

	List<Presence> getGlobalPresences(){ (List<Presence>) (presenceCache?.list() ?: []) }
	Map<String, Presence> getGlobalPresenceMap(){ (Map<String, Presence>) (presenceCache?.map() ?: [:]) }

	List<User> getAllUsers() { userMap.values().toList() }
	List<Member> getAllMembers(){ (List<Member>) memberMap.values()*.values().flatten().toList() }
	List<Role> getAllRoles(){ (List<Role>) roleMap.values().flatten().toList() }
	List<Emoji> getAllEmojis(){ (List<Emoji>) emojiIdMap.values().flatten().toList() }

	List<User> getUsers(){ allUsers }
	List<Member> getMembers(){ allMembers }
	List<Role> getRoles(){ allRoles }
	List<Emoji> getEmojis(){ allEmojis }

	Map<String, User> getUserMap(){
		def result = new HashMap<String, User>()
		for (e in guildCache) {
			def m = (DiscordListCache<Member>) e.value.members
			for (me in m) result.put(me.key, m.at(me.key).user)
		}
		for (e in privateChannelCache) {
			def m = (DiscordListCache<User>) e.value.recipients
			for (me in m) result.put(me.key, m.at(me.key))
		}
		if (null != relationshipCache) for (e in relationshipCache) {
			def r = relationshipCache.at(e.key).user
			result.put(r.id, r)
		}
		result
	}

	Map<String, Map<String, Member>> getMemberMap(){
		Map doo = [:]
		guilds.each { doo[it.id] = it.memberMap }
		doo
	}

	Map<String, Role> getRoleMap(){
		def result = new HashMap<String, Role>()
		for (e in guilds) result.putAll(e.roleMap)
		result
	}

	Map<String, Emoji> getEmojiIdMap(){
		def result = new HashMap<String, Emoji>()
		for (e in guilds) result.putAll(e.emojiIdMap)
		result
	}

	Map<String, Emoji> getEmojiNameMap(){
		def result = new HashMap<String, Emoji>()
		for (e in guilds) result.putAll(e.emojiNameMap)
		result
	}

	boolean isVerified(){ userObject.verified as boolean }

	def <T> T askPool(String name, String bucket = '$', Closure<T> action){
		pools[name].ask bucket, action
	}

	def syncGuilds(g){
		g = g instanceof Collection ?
			g.collect(DiscordObject.&id).collate(25) :
			[[id(g)]]
		for (c in g) {
			ws.send op: 12, d: c
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(guilds, String query = '', int limit = 0){
		guilds = guilds instanceof Collection ?
			guilds.collect(DiscordObject.&id).collate(25) :
			[id(guilds)]
		guilds.each {
			ws.send op: 8, d: [
				guild_id: it,
				query: query,
				limit: limit
			]
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(guilds, int limit, String query = ''){
		chunkMembersFor(guilds, query, limit)
	}

	String filterMessage(cnt){
		String a = cnt.toString()
		messageFilters.each { k, v ->
			if (k instanceof Pattern) a = a.invokeMethod('replaceAll', [((Pattern) k).pattern(), v]).toString()
			else a = a.invokeMethod('replace', [k.toString(), v]).toString()
		}
		a
	}

	static String getApplicationLink(app, Permissions perms = null){
		"https://discordapp.com/oauth2/authorize?client_id=${id(app)}&scope=bot" +
			(perms ? "&permissions=$perms.value" : '')
	}

	static String getAppLink(app, Permissions perms = null){ getApplicationLink(app, perms) }
	static String applicationLink(app, Permissions perms = null){ getApplicationLink(app, perms) }
	static String appLink(app, Permissions perms = null){ getApplicationLink(app, perms) }

	void mute(t){
		if (t.class.array || t instanceof Collection) mutedChannels.addAll(t.collect(this.&id))
		else mutedChannels.add(id(t))
	}

	void changePresence(Map<String, Object> data) {
		def oldPresence = presences.find { it.id == id }
		def dg = data.game
		def payload = [
			status: data.status ?: oldPresence?.status ?: 'online',
			game: null != dg ?
				dg instanceof String ? [name: dg, type: 0] :
				dg instanceof DiscordObject ? ((DiscordObject) dg).object :
				dg instanceof Map ? dg :
				[name: dg.toString()] :
				oldPresence?.game?.object ?: null,
			since: data.since ?: 0,
			afk: data.afk ?: false,
		]
		askPool('changePresence'){
			ws.send(3, payload)
		}
		for (s in guildCache){
			((DiscordListCache) s.value.presences).add(
				user: userObject,
				game: payload.game,
				status: payload.status,
				guild_id: s.key
			)
		}
		presenceCache.add(
			user: userObject,
			game: payload.game,
			status: payload.status,
			last_modified: System.currentTimeMillis()
		)
	}

	void status(status){ changePresence(status: status) }
	void play(game){ changePresence(game: game) }
	void playGame(game){ changePresence(game: game) }

	// WEBSOCKET LISTENERS

	void addListener(DiscordRawWSListener listener) { rawListeners.add(listener) }

	void addCacher() {
		addListener(new DiscordRawWSListener() {
			@Override
			@CompileStatic
			void fire(String type, Map d) {
				def g = (String) d.guild_id
				if (type == 'GUILD_MEMBER_ADD') {
					((DiscordListCache) guildCache[g].members).add(d)
					((int) guildCache[g].member_count)++
				} else if (type == 'GUILD_MEMBER_REMOVE') {
					if (!guildCache[g]) return
					((DiscordListCache<Member>) guildCache[g].members).remove((String) ((Map) d.user).id)
					((int) guildCache[g].member_count)--
				} else if (type == 'GUILD_ROLE_CREATE') {
					((DiscordListCache<Role>) guildCache[g].roles).add((Map) d.role + [guild_id: g])
				} else if (type == 'GUILD_ROLE_DELETE') {
					((DiscordListCache<Role>) guildCache[g].roles).remove((String) d.role_id)
				} else if (type == 'CHANNEL_CREATE') {
					if (null != g) ((DiscordListCache<Channel>) guildCache[g].channels).add(d)
					else privateChannelCache.add(d)
				} else if (type == 'CHANNEL_DELETE') {
					if (null != g) ((DiscordListCache<Channel>) guildCache[g].channels).remove((String) d.id)
					else privateChannelCache.remove((String) d.id)
				} else if (type == 'CHANNEL_UPDATE') {
					if (null != g) ((DiscordListCache<Channel>) guildCache[g].channels)[(String) d.id].putAll(d)
					else privateChannelCache[(String) d.id].putAll(d)
				} else if (type == 'MESSAGE_CREATE') {
					if (messages[(String) d.channel_id]) {
						messages[(String) d.channel_id].add(d)
					} else {
						def a = new DiscordListCache([d], Client.this, Message)
						a.setRoot channel((String) d.channel_id)
						messages[(String) d.channel_id] = a
					}
				} else if (type == 'MESSAGE_UPDATE') {
					if (messages[(String) d.channel_id]) {
						if (messages[(String) d.channel_id][(String) d.id]) {
							messages[(String) d.channel_id][(String) d.id] << d
						} else {
							messages[(String) d.channel_id].add(d)
						}
					} else {
						def a = new DiscordListCache([d], Client.this, Message)
						a.setRoot channel((String) d.channel_id)
						messages[(String) d.channel_id] = a
					}
				} else if (type == 'MESSAGE_DELETE') {
					messages[(String) d.channel_id]?.remove((String) d.id)
				} else if (type == 'GUILD_CREATE') {
					if (guildCache[(String) d.id]) guildCache[(String) d.id].putAll(d)
					else guildCache.add(d)
				} else if (type == 'GUILD_DELETE') {
					guildCache.remove((String) d.id)
				} else if (type == 'GUILD_MEMBER_UPDATE') {
					((DiscordListCache<Member>) guildCache[g].members)[(String) ((Map) d.user).id]?.putAll(d)
				} else if (type == 'ROLE_UPDATE') {
					def r = (String) ((Map) d.role).id
					def rc = (DiscordListCache<Role>) guildCache[g].roles
					if (rc.containsKey(r)) rc[r].putAll((Map) d.role)
					else {
						def x = new HashMap<String, Object>((Map) d.role)
						x.put('guild_id', g)
						rc.put r, x
					}
				} else if (type == 'GUILD_UPDATE') {
					guildCache[(String) d.id] = d
				} else if (type == 'PRESENCE_UPDATE') {
					if (null != g) {
						if (guildCache[g].unavailable) return
						if (((Map) d.user).avatar) {
							def m = (DiscordListCache<Member>) guildCache[g].members
							if (m.containsKey((String) ((Map) d.user).id)) {
								((Map) m[(String) ((Map) d.user).id].user).putAll((Map) d.user)
							} else {
								Map di = new HashMap(d)
								di.remove 'status'
								di.remove 'game'
								if (allowMemberRequesting) di.putAll(requestMember(g, (String) ((Map) d.user).id).object)
								m.add(di)
							}
						}
						def pc = (DiscordListCache<Presence>) guildCache[g].presences
						if (d.status == 'offline') pc?.remove((String) ((Map) d.user).id)
						else {
							Map di = new HashMap(d)
							di.remove 'roles'
							di.remove 'nick'
							di.put 'id', ((Map) d.user).id
							pc?.add(di)
						}
					} else {
						def m = (Map) d.user
						def i = (String) m.id
						if (null == user(i)) for (e in privateChannelCache) {
							def v = (DiscordListCache<User>) e.value.recipients
							if (v.containsKey(i)) v[i] = m
						}
						if (d.status == 'offline') presenceCache.remove(i)
						else {
							def x = new HashMap<String, Object>(d)
							x.put('id', i)
							presenceCache[i] = x
						}
					}
				} else if (type == 'CHANNEL_RECIPIENT_ADD') {
					((DiscordListCache<User>) privateChannelCache[(String) d.channel_id].recipients)
							.add((Map) d.user)
				} else if (type == 'CHANNEL_RECIPIENT_REMOVE') {
					((DiscordListCache<User>) privateChannelCache[(String) d.channel_id].recipients)
							.remove((String) ((Map) d.user).id)
				} else if (type == 'VOICE_STATE_UPDATE') {
					def gi = g
					if (null == g) {
						for (e in guildCache) if (((DiscordListCache<Channel>) e.value.channels)
												  .containsKey((String) d.channel_id)) gi = e.key
					}
					def x = (DiscordListCache<VoiceState>) guildCache[gi].voice_states
					if ((String) d.channel_id) {
						if (x.containsKey((String) d.id)) x[(String) d.id].putAll d
						else x[(String) d.id] = d
					} else x.remove((String) d.id)
				} else if (type == 'GUILD_EMOJIS_UPDATE') {
					guildCache[g].emojis = d.emojis
				} else if (type == 'USER_UPDATE') {
					userObject.putAll d
				} else if (type == 'GUILD_MEMBERS_CHUNK') {
					for (x in (List<Map>) d.members)
						((DiscordListCache<Member>) guildCache[g].members).add(x + [guild_id: g])
				} else if (type == 'GUILD_SYNC') {
					for (x in (List<Map>) d.members)
						((DiscordListCache<Member>) guildCache[g].members).add(x + [guild_id: g])
					for (x in (List<Map>) d.presences)
						((DiscordListCache<Presence>) guildCache[g].presences).add(x + [guild_id: g])
					guildCache[(String) d.id].large = d.large
				} else if (type == 'USER_NOTE_UPDATE') {
					def n = ((Map<String, Object>) readyData.notes)
					d.note ? n.put((String) d.id, d.note) : n.remove((String) d.id)
				} else if (type == 'MESSAGE_REACTION_ADD') {
					def fabricated = [user_id: (String) d.user_id,
					                  emoji  : d.emoji]
					reactions.containsKey((String) d.message_id) ?
							reactions[(String) d.message_id].add(fabricated) :
							reactions.put((String) d.message_id, [(Map) fabricated])
				} else if (type == 'MESSAGE_REACTION_REMOVE' && reactions.containsKey((String) d.message_id)) {
					def x = reactions[(String) d.message_id]
					int i = 0
					for (a in x) {
						if (a.user_id == d.user_id && a.emoji == d.emoji) {
							x.remove(i)
							break
						}
						i++
					}
				} else if (type == 'MESSAGE_REACTION_REMOVE_ALL' && reactions.containsKey((String) d.message_id)) {
					reactions.remove((String) d.message_id)
				} else if (type == 'RELATIONSHIP_ADD') {
					relationshipCache.add(d)
				} else if (type == 'RELATIONSHIP_REMOVE') {
					relationshipCache.remove((String) d.id)
				} else if (type == 'CALL_CREATE') {
					calls[(String) d.channel_id] = d
				} else if (type == 'CALL_UPDATE') {
					calls[(String) d.channel_id] << d
				} else if (type == 'CALL_DELETE') {
					calls.remove((String) d.channel_id)
				} else if (type == 'USER_GUILD_SETTINGS_UPDATE') {
					def x = userGuildSettingCache[g]
					if (x) x.putAll(d)
					else userGuildSettingCache[g] = d
				} else if (type == 'USER_SETTINGS_UPDATE') {
					((Map) readyData.user_settings) << d
				}
			}
		})
	}

	def requestMembersOnReady(){
		addListener(new DiscordRawWSListener() {
			@CompileStatic
			void fire(String type, Map data) {
				chunkMembersFor(guilds.findAll { it.large })
			}
		})
	}

	// add this yourself
	void addReconnector(){
		addListener('close') { Map it ->
			if (!gatewayClosed && ((int) it.code) < 4000) ws.reconnect()
		}
	}

	// HTTP REQUESTS

	List<Guild> requestGuilds(boolean checkCache = true){
		http.jsonGets('users/@me/guilds').collect {
			Map object = it
			if (checkCache && !it.unavailable){
				def cached = guildCache[(String) it.id]
				object.members = cached.members
				object.channels = cached.channels
				object.presences = cached.presences
				object.voice_states = cached.voice_states
				object.roles = cached.roles
				object.emojis = cached.emojis
				object.large = cached.large
			}
			new Guild(this, object)
		}
	}

	List<Channel> requestPrivateChannels(){
		http.jsonGets('users/@me/channels').collect { new Channel(this, it) }
	}

	void moveMemberVoiceChannel(s, u, vc){
		editMember(channel_id: id(vc), s, u)
	}

	void editRoles(s, u, Collection r) {
		editMember(roles: r.collect(this.&id), s, u)
	}

	void addRoles(s, u, Collection r) {
		editRoles(s, u, (List) ((DiscordListCache<Member>) guildCache[id(s)].members)[id(u)].roles + r)
	}

	void addRole(s, m, r){
		http.put("guilds/${id(s)}/members/${id(m)}/roles/${id(r)}")
	}

	void removeRole(s, m, r){
		http.delete("guilds/${id(s)}/members/${id(m)}/roles/${id(r)}")
	}

	String changeOwnGuildNick(s, String nick){
		askPool('changeNick'){
			http.jsonPatch("guilds/${id(s)}/members/@me/nick",
				[nick: nick]).nick
		}
	}

	void changeGuildNick(s, m, String nick){
		editMember(nick: nick, s, m)
	}

	void kick(s, m){
		http.delete("guilds/${id(s)}/members/${id(m)}")
	}

	List<Guild.Ban> requestBans(s) {
		http.jsonGets("guilds/${id(s)}/bans").collect { new Guild.Ban(this, it) }
	}

	List<Invite> requestGuildInvites(s){
		http.jsonGets("guilds/${id(s)}/invites").collect { new Invite(this, it) }
	}

	List<Region> requestRegions(s){
		http.jsonGets("guilds/${id(s)}/regions").collect { new Region(this, it) }
	}

	List<Integration> requestIntegrations(s){
		http.jsonGets("guilds/${id(s)}/integrations").collect { new Integration(this, it) }
	}

	Integration createIntegration(s, type, id){
		new Integration(this, http.jsonPost("guilds/${this.id(s)}/integrations",
			[type: type.toString(), id: id.toString()]))
	}

	void ban(s, u, int d = 0) {
		http.put("guilds/${id(s)}/bans/${id(u)}?delete-message-days=$d", [:])
	}

	void unban(s, u) {
		http.delete("guilds/${id(s)}/bans/${id(u)}")
	}

	int checkPrune(s, int d){
		(int) http.jsonGet("guilds/${id(s)}/prune?days=$d").pruned
	}

	int prune(s, int d){
		(int) http.jsonPost("guilds/${id(s)}/prune?days=$d").pruned
	}

	Role createRole(Map data, s) {
		if (data.color instanceof Color) data.color = ((Color) data.color).RGB
		if (data.permissions instanceof Permissions) data.permissions = ((Permissions) data.permissions).value
		Role createdRole = new Role(this, http.jsonPost("guilds/${id(s)}/roles", [:]))
		editRole(data, s, createdRole)
	}

	Role editRole(Map data, s, r) {
		if (data.color instanceof Color) data.color = ((Color) data.color).RGB
		if (data.permissions instanceof Permissions) data.permissions = ((Permissions) data.permissions).value
		new Role(this, http.jsonPatch("guilds/${id(s)}/roles/${id(r)}", data))
	}

	void deleteRole(s, r) {
		http.delete("guilds/${id(s)}/roles/${id(r)}")
	}
	
	List<Role> editRolePositions(Map mods, s){
		http.jsonPatches("guilds/${id(s)}/roles", mods.collect { k, v ->
			[id: id(k), position: v]}).collect { new Role(this, it + [guild_id: (Object) id(s)]) }
	}
	
	List<Channel> editChannelPositions(Map mods, s){
		http.jsonPatches("guilds/${id(s)}/channels", mods.collect { k, v ->
			[id: id(k), position: v]}).collect { new Channel(this, Channel.construct(this, it, id(s))) }
	}

	List<Webhook> requestGuildWebhooks(s){
		http.jsonGets("guilds/${s}/webhooks").collect { new Webhook(this, it) }
	}

	List<Member> requestMembers(s, int max=1000, boolean updateCache=true){
		def i = id(s)
		List<Map> members = http.jsonGets("guilds/$i/members?limit=${max}")
		if (max > 1000){
			for (int m = 1; m < (int) Math.ceil(max / 1000) - 1; m++){
				members += http.jsonGets("guilds/$i/members?after=${(m * 1000) + 1}&limit=1000")
			}
			members += http.jsonGets("guilds/$i/members?after=${(int)((Math.ceil(max / 1000) - 1) * 1000)+1}&limit=1000")
		}
		if (updateCache){
			guildCache[i].members = new DiscordListCache(members.collect {
				def r = new HashMap<String, Object>(it)
				r.put('guild_id', i)
				r.putAll((Map<String, Object>) it.user)
				new Member(this, r)
			}, this, Member)
			guildCache[i].member_count = members.size()
		}
		members.collect {
			def r = new HashMap(it)
			r.put('guild_id', i)
			new Member(this, r)
		}
	}

	Member requestMember(s, m){ new Member(this,
		http.jsonGet("guilds/${id(s)}/members/${id(m)}")) }

	List<Emoji> requestEmojis(s){ http.jsonGets("guilds/${id(s)}/emojis")
		.collect { new Emoji(this, it) } }

	Emoji createEmoji(Map data, s){
		new Emoji(this, http.jsonPost("guilds/${id(s)}/emojis",
			patchData(data, 'image')))
	}

	Emoji editEmoji(Map data, s, emoji){
		new Emoji(this, http.jsonPatch("guilds/${id(s)}/emojis/${id(emoji)}", data))
	}

	Webhook requestGuildWebhook(s, w){
		new Webhook(this, http.jsonGet("guilds/${id(s)}/webhooks/${id(w)}"))
	}

	Invite acceptInvite(i){
		new Invite(this, http.jsonPost("invite/${Invite.parseId(i)}", [:]))
	}

	Invite requestInvite(i){
		new Invite(this, http.jsonGet("invite/${Invite.parseId(i)}"))
	}

	Invite createInvite(Map data = [:], c){
		new Invite(this, http.jsonPost("channels/${id(c)}/invites", data))
	}
	
	void deleteInvite(i){
		http.delete("invite/${Invite.parseId(i)}")
	}

	Guild.Embed requestEmbed(s){ new Guild.Embed(this, http.jsonGet("guilds/${id(s)}/embed")) }

	Guild.Embed editEmbed(Map data, s, e){ new Guild.Embed(this,
		http.jsonPatch("guilds/${id(s)}/embed", data)) }

	List<Connection> requestConnections(){
		http.jsonGets('users/@me/connections').collect { new Connection(this, it) }
	}

	User edit(Map data){
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

	Profile requestProfile(a){
		new Profile(this, http.jsonGet("users/${id(a)}/profile"))
	}

	Application requestApplication(){
		new Application(this, http.jsonGet('oauth2/applications/@me'))
	}

	List<Application> requestApplications(){
		http.jsonGets('oauth2/applications').collect { new Application(this, it) }
	}

	Application requestApplication(a){
		new Application(this, http.jsonGet("oauth2/applications/${id(a)}"))
	}

	Application editApplication(Map data, a){
		new Application(this, http.jsonPut("oauth2/applications/${id(a)}", patchData(data)))
	}

	void deleteApplication(a){
		http.delete("oauth2/applications/${id(a)}")
	}

	Application createApplication(Map data){
		Map map = [icon: null, description: '', redirect_uris: [], name: '']
		new Application(this, http.jsonPost('oauth2/applications', map <<
			patchData(data)))
	}

	BotAccount createApplicationBotAccount(a, String oldAccountToken = null){
		new BotAccount(this, http.jsonPost("oauth2/applications/${id(a)}/bot",
			(oldAccountToken == null) ? [:] : [token: oldAccountToken]))
	}

	List<Region> requestRegions(){
		http.jsonGets('voice/regions').collect { new Region(this, it) }
	}

	List<User> queueUsers(String query, int limit=25){
		http.jsonGets("users?q=${URLEncoder.encode(query, 'UTF-8')}&limit=$limit")
			.collect { new User(this, it) }
	}

	User requestUser(i){ new User(this, http.jsonGet("users/${id(i)}")) }
	Guild requestGuild(i){ new Guild(this, http.jsonGet("guilds/${id(i)}")) }
	Channel requestChannel(i){ new Channel(this, http.jsonGet("channels/${id(i)}")) }

	Guild editGuild(Map data, s) {
		new Guild(this, object << http.jsonPatch("guilds/${id(s)}",
			patchData(data)))
	}

	void leaveGuild(s) {
		http.delete("users/@me/guilds/${id(s)}")
	}

	void deleteGuild(s) {
		http.delete("guilds/${id(s)}")
	}

	Channel createTextChannel(s, String name) {
		new Channel(this, http.jsonPost("guilds/${id(s)}/channels",
			[name: name, type: 0]))
	}

	Channel createVoiceChannel(s, String name) {
		new Channel(this, http.jsonPost("guilds/${id(s)}/channels",
			[name: name, type: 2]))
	}
	
	Channel createChannel(Map data = [:], s){
		new Channel(this, http.jsonPost("guilds/${id(s)}/channels", data))
	}

	Channel requestGuildChannel(s, c){
		new Channel(this, http.jsonGet("guilds/${id(s)}/channels/${id(c)}"))
	}

	List<Channel> requestGuildChannels(s){
		http.jsonGets("guilds/${id(s)}/channels").collect { new Channel(this, it) }
	}

	Integration editIntegration(Map data, s, i){
		new Integration(this,
			http.jsonPatch("guilds/${id(s)}/integrations/${id(i)}", data))
	}

	void deleteIntegration(s, i){
		http.delete("guilds/${id(s)}/integrations/${id(i)}")
	}

	void syncIntegration(s, i){
		http.post("guilds/${id(s)}/integrations/${id(i)}/sync")
	}

	void editMember(Map data, s, m){
		askPool('editMembers', id(s)){
			http.patch("guilds/${id(s)}/members/${id(m)}", data)
		}
	}

	void addChannelRecipient(c, u){
		http.put("channels/${id(c)}/recipients/${id(u)}")
	}

	void removeChannelRecipient(c, u){
		http.delete("channels/${id(c)}/recipients/${id(u)}")
	}

	void addRelationship(u, type){
		http.put("users/@me/relationships/${id(u)}", [type: type.invokeMethod('toInteger', null)])
	}

	void removeRelationship(u){
		http.delete("users/@me/relationships/${id(u)}")
	}

	List<Invite> requestChannelInvites(c){
		http.jsonGets("channels/${id(c)}/invites").collect { new Invite(this, it) }
	}

	void startTyping(c){
		http.post("channels/${id(c)}/typing")
	}

	void deleteChannel(c){
		http.delete("channels/${id(c)}")
	}

	Channel editChannel(Map data, c) {
		new Channel(this, http.jsonPatch("channels/${id(c)}",
			patchableObject << patchData(data)))
	}

	void editChannelOverwrite(Map data, c, o){
		String i = id(o)
		String type = data.type ?: (role(i) ? 'role' : 'member')
		int allowBytes = null == data.allow ? 0 : (int) data.allow.invokeMethod('toInteger', null)
		int denyBytes = null == data.deny ? 0 : (int) data.deny.invokeMethod('toInteger', null)
		http.put("channels/${id(c)}/permissions/${i}",
			[allow: allowBytes, deny: denyBytes, id: i, type: type])
	}

	void deleteChannelOverwrite(c, o){
		http.delete("channels/${id(c)}/permissions/${id(o)}")
	}

	Webhook createWebhook(Map data = [:], c){
		new Webhook(this, http.jsonPost("channels/${id(c)}/webhooks",
			patchData(data)))
	}

	List<Webhook> requestChannelWebhooks(c){
		http.jsonGets("channels/${id(c)}/webhooks").collect { new Webhook(this, it) }
	}

	Webhook requestWebhook(w){
		new Webhook(this, http.jsonGet("webhooks/${id(w)}"))
	}

	Webhook requestWebhook(id, token) {
		new Webhook(this, http.jsonGet("webhooks/${this.id(id)}/$token"))
	}

	Message sendMessage(Map data, c){
		if (data.containsKey('content')){
			def s = data.content.toString()
			s = filterMessage(s)
			if (!s || s.size() > 2000) throw new MessageInvalidException(s)
			data.content = s
		}
		boolean isWebhook = MiscUtil.defaultValueOnException new Closure(this) {
			@CompileDynamic
			def call() {
				data.webhook && c?.id && c?.token
			}
		}
		Closure<Message> clos = {
			new Message(this, http.jsonPost(isWebhook ?
				"webhooks/${id(c)}/${c instanceof Map ? c.token : ((Webhook) c).token}" :
				"channels/${id(c)}/messages", [channel_id: id] << data))
		}
		isWebhook ? clos() : askPool('sendMessages', getChannelQueueName(c), clos)
	}

	Message sendMessage(c, content, tts = false){ sendMessage(c, content: content, tts: tts) }

	Message editMessage(Map data, c, m){
		if (data.containsKey('content')){
			def s = data.content.toString()
			s = filterMessage(s)
			if (!s || s.size() > 2000) throw new MessageInvalidException(s)
			data.content = s
		}
		askPool('sendMessages', getChannelQueueName(c)){ // that's right, they're in the same bucket
			new Message(this, http.jsonPatch("channels/${id(c)}/messages/${id(m)}", data))
		}
	}

	void deleteMessage(c, m){
		askPool('deleteMessages',
			getChannelQueueName(c)){ http.delete("channels/${id(c)}/messages/${id(m)}") }
	}

	Map sendFileRaw(Map data = [:], c, file){
		boolean isWebhook = MiscUtil.defaultValueOnException new Closure(this) {
			@CompileDynamic
			def call() {
				data.webhook && c?.id && c?.token
			}
		}
		List fileArgs = []
		if (file instanceof File){
			if (data.filename){
				fileArgs += file.bytes
				fileArgs += data.filename
			}else fileArgs += file
		}else{
			fileArgs += ConversionUtil.getBytes(file)
			if (!data.filename) throw new IllegalArgumentException("Tried to send non-file class ${file.class} and gave no filename")
			fileArgs += data.filename
		}
		def url = http.baseUrl + (isWebhook ?
			"webhooks/${id(c)}/${((Webhook) c).token}" :
			"channels/${id(c)}/messages")
		def aa = Unirest.post(url)
			.header('Authorization', token)
			.header('User-Agent', fullUserAgent)
			.field('content', data.content == null ? '' : data.content.toString())
			.field('tts', data.tts as boolean)
		if (fileArgs.size() == 1){
			aa = aa.field('file', fileArgs[0])
		}else if (fileArgs.size() == 2){
			aa = (MultipartBody) aa.invokeMethod('field', ['file', fileArgs[0], fileArgs[1]])
		}
		Closure<Map> clos = {
			(Map) JSONUtil.parse(aa.asString().body)
		}
		isWebhook ? clos() : askPool('sendMessages', getChannelQueueName(c), clos)
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

	Message sendFile(c, implicatedFile, filename){
		sendFile([:], c, implicatedFile, filename)
	}

	Message sendFile(c, implicatedFile){
		sendFile([:], c, implicatedFile, null)
	}

	Message requestMessage(c, ms, boolean addToCache = true){
		def ch = id(c)
		def m = new Message(this,
			http.jsonGet("channels/$ch/messages/${id(ms)}"))
		if (addToCache){
			if (messages[ch]) messages[ch].add(m)
			else {
				def r = new DiscordListCache([m], this, Message)
				r.setRoot channel(ch)
				messages[ch] = r
			}
		}
		m
	}

	Message message(c, m, boolean request = true){
		boolean inCache = messages[id(c)].containsKey(id(m))
		if (inCache) new Message(this, messages[id(c)][id(m)])
		else if (request) requestMessage(c, m)
		else null
	}

	String getChannelQueueName(c){
		guildChannel(id(c)) ?: 'dm'
	}

	def pinMessage(c, m){
		http.put("channels/${id(c)}/pins/${id(m)}")
	}

	def unpinMessage(c, m){
		http.delete("channels/${id(c)}/pins/${id(m)}")
	}

	Collection<Message> requestPinnedMessages(c){
		http.jsonGets("channels/${id(c)}/pins").collect { new Message(this, it) }
	}

	void reactToMessage(c, m, e){
		http.put("channels/${id(c)}/messages/${id(m)}/" +
			"reactions/${translateEmoji(e)}/@me")
	}

	void unreactToMessage(c, m, e, u = '@me'){
		http.delete("channels/{id(c)}/messages/${id(m)}/" +
			"reactions/${translateEmoji(e)}/${id(u)}")
	}

	List<User> requestReactors(c, m, e, int limit = 100){
		http.jsonGets("channels/${id(c)}/messages/${id(m)}/reactions/" +
			translateEmoji(e) + "?limit=$limit").collect { new User(this, it) }
	}

	String translateEmoji(emoji, s = null){
		if (emoji ==~ /\d+/){
			translateEmoji(emojis.find { it.id == emoji })
		}else if (emoji instanceof Emoji){
			"$emoji.name:$emoji.id"
		}else if (emoji ==~ /\w+/){
			def i = ((List<Emoji>) (guild(s)?.emojis ?: []) + (List<Emoji>) (emojis - guild(s)?.emojis)
				).find { it.name == emoji }?.id
			i ? "$emoji:$i" : ":$emoji:"
		}else{
			emoji
		}
	}

	List<Message> requestChannelLogs(c, int max = 100, boundary = null,
		String boundaryType = 'before'){
		Map<String, Map> cached = messages[id(c)]
		if (!boundary && cached?.size() > max) return cached.values().sort {
			it.id }[-1..-max].collect { new Message(this, it) }
		def l = directRequestChannelLogs(c, max, boundary, boundaryType)
		if (boundaryType in ['around', 'after']) l = l.reverse()
		if (cached) for (a in l) messages[id(c)].add(a)
		else messages[id(c)] = new DiscordListCache(l, this, Message)
		l.collect { new Message(this, it) }
	}

	List<Message> forceRequestChannelLogs(c, int m = 100, b = null,
		String bt = 'before'){ directRequestChannelLogs(c, m, b, bt).collect { new Message(this, it) } }

	// after and around sorted old-new
	// before sorted new-old

	@SuppressWarnings("GroovyAssignabilityCheck")
	List<Map<String, Object>> directRequestChannelLogs(c, int max, boundary = null, String boundaryType = 'before'){
		Map<String, Object> params = [limit: (Object) (max > 100 ? 100 : max)]
		if (boundary){
			if (boundaryType && boundaryType != 'before' && boundaryType != 'after' && boundaryType != 'around')
				throw new IllegalArgumentException('Boundary type has to be before, after or around')
			params.put boundaryType ?: 'before', id(boundary)
		}
		List<Map<String, Object>> messages = directRequestChannelLogs(params, c)
		if (max > 100){
			if (boundaryType == 'after'){
				for (int m = 1; m < Math.floor(max / 100); m++) {
					messages.addAll directRequestChannelLogs(c,
							after: messages.last().id, limit: 100)
				}
				if (max % 100 != 0) messages.addAll directRequestChannelLogs(c,
						after: messages.last().id, limit: max % 100)
			}else if (boundaryType == 'around'){
				int age = max - 100
				int af = age.intdiv(2).intValue()
				int bef = age.intdiv(2).intValue() + (age % 2)
				for (int m = 1; m < Math.floor(bef / 100); m++) {
					messages.addAll 0, directRequestChannelLogs(c, before: messages.first().id,
							limit: 100).reverse()
				}
				if (bef % 100 != 0) messages.addAll 0, directRequestChannelLogs(c, before: messages.first().id,
						limit: bef % 100).reverse()
				for (int m = 1; m < Math.floor(af / 100); m++) {
					messages.addAll directRequestChannelLogs((Map<String, Object>) [after: messages.last().id, limit: 100], c)
				}
				if (af % 100 != 0) messages.addAll directRequestChannelLogs([before: messages.last().id,
						limit: af % 100], c)
			}else{
				for (int m = 1; m < Math.floor(max / 100); m++) {
					messages.addAll directRequestChannelLogs(c,
							before: messages.last().id, limit: 100)
				}
				if (max % 100) messages.addAll directRequestChannelLogs(c,
						before: messages.last().id, limit: max % 100)
			}
		}
		messages
	}

	List<Map<String, Object>> directRequestChannelLogs(Map<String, Object> data = [:], c){
		String parameters = data ? '?' + data.collect { k, v ->
			URLEncoder.encode(k.toString(), 'UTF-8') + '=' + URLEncoder.encode(v.toString(), 'UTF-8')
		}.join('&') : ''
		http.jsonGets("channels/${id(c)}/messages$parameters")
	}

	def bulkDeleteMessages(c, Collection ids){
		askPool('bulkDeleteMessages'){
			http.post("channels/${id(c)}/messages/bulk-delete",
				[messages: ids.collect { id(it) }])
		}
	}

	Webhook editWebhook(Map data, w){
		new Webhook(this, http.jsonPatch("webhooks/${id(w)}",
			patchData(data)))
	}

	void deleteWebhook(w){
		http.delete("webhooks/${id(w)}")
	}

	Channel createPrivateChannel(u){
		new Channel(this, http.jsonPost('users/@me/channels',
			[recipient_id: id(u)]))
	}
}
