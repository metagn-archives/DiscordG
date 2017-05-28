package hlaaftana.discordg

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.collections.DynamicMap;
import hlaaftana.discordg.exceptions.MessageInvalidException
import hlaaftana.discordg.logic.ActionPool
import hlaaftana.discordg.logic.ParentListenerSystem;
import hlaaftana.discordg.net.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.objects.*

import java.awt.Color
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * The Discord client.
 * @author Hlaaftana
 */
class Client extends User {
	static Closure newPool = ActionPool.&new
	static Map eventAliases = [MESSAGE: 'MESSAGE_CREATE',
		NEW_MESSAGE: 'MESSAGE_CREATE', MESSAGE_DELETED: 'MESSAGE_DELETE',
		MESSAGE_BULK_DELETE: 'MESSAGE_DELETE_BULK',
		MESSAGE_BULK_DELETED: 'MESSAGE_DELETE_BULK',
		MESSAGE_UPDATED: 'MESSAGE_UPDATE', CHANNEL: 'CHANNEL_CREATE',
		NEW_CHANNEL: 'CHANNEL_CREATE', CHANNEL_UPDATED: 'CHANNEL_UPDATE',
		CHANNEL_DELETED: 'CHANNEL_DELETE', BAN: 'GUILD_BAN_ADD',
		UNBAN: 'GUILD_BAN_REMOVE', GUILD: 'GUILD_CREATE', SERVER: 'GUILD_CREATE',
		SERVER_CREATE: 'GUILD_CREATE', GUILD_CREATED: 'GUILD_CREATE',
		SERVER_CREATED: 'GUILD_CREATE', GUILD_UPDATED: 'GUILD_UPDATE',
		SERVER_UPDATED: 'GUILD_UPDATE', SERVER_UPDATE: 'GUILD_UPDATE',
		SERVER_DELETE: 'GUILD_DELETE', SERVER_DELETED: 'GUILD_DELETE',
		GUILD_DELETED: 'GUILD_DELETE', MEMBER: 'GUILD_MEMBER_ADD',
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
	static List knownDiscordEvents = ['READY', 'MESSAGE_ACK', 'GUILD_INTEGRATIONS_UPDATE',
		'GUILD_EMOJIS_UPDATE', 'VOICE_STATE_UPDATE', 'VOICE_SERVER_UPDATE', 'USER_UPDATE',
		'USER_GUILD_SETTINGS_UPDATE', 'USER_SETTINGS_UPDATE', 'GUILD_MEMBERS_CHUNK',
		'GUILD_SYNC', 'CHANNEL_PINS_UPDATE', 'CHANNEL_PINS_ACK',
		'MESSAGE_REACTION_REMOVE_ALL', 'WEBHOOKS_UPDATE', 'RESUMED'] +
			(eventAliases.values() as ArrayList).unique()
	static List knownEvents = ['INITIAL_GUILD_CREATE', 'UNRECOGINZED', 'ALL'] + knownDiscordEvents
	static Map defaultEventDataCalls = [
		CHANNEL_CREATE: { client, data ->
			Map c = data
			c = Channel.construct(client, c)
			Channel ass = new Channel(client, c)
			server { ass.server }
			channel { ass }
			constructed { c }
		},
		CHANNEL_RECIPIENT_ADD: { client, data ->
			channel { client.privateChannel(data.channel_id) }
			recipient { new User(client, data.user) }
			alias 'recipient', 'user'
		},
		GUILD_BAN_ADD: { client, data ->
			server { client.server(data.guild_id) }
			user { new User(client, data.user) }
		},
		GUILD_CREATE: { client, data ->
			Map s = data
			s = Server.construct(client, s)
			server { new Server(client, s) }
			constructed { s }
		},
		GUILD_DELETE: { client, data ->
			server client.server(data.id)
		},
		GUILD_INTEGRATIONS_UPDATE: { client, data ->
			server { client.server(data.guild_id) }
		},
		GUILD_EMOJIS_UPDATE: { client, data ->
			server { client.server(data.guild_id) }
			emojis {
				new DiscordListCache(data.emojis
					.collect { it + [guild_id: data.guild_id] }
					, client, Emoji)
			}
		},
		GUILD_MEMBER_ADD: { client, data ->
			server { client.server(data.guild_id) }
			member { new Member(client, data) }
		},
		GUILD_MEMBER_REMOVE: { client, data ->
			server { client.server(data.guild_id) }
			user(client.server(data.guild_id)?.member(data.user) ?:
					new User(client, data.user))
			alias 'user', 'member'
		},
		GUILD_ROLE_CREATE: { client, data ->
			server { client.server(data.guild_id) }
			role { new Role(client, data.role << [guild_id: data.guild_id]) }
		},
		GUILD_ROLE_DELETE: { client, data ->
			server { client.server(data.guild_id) }
			role client.serverMap[data.guild_id].roleMap[data.role_id]
		},
		GUILD_UPDATE: { client, data ->
			Map s = data
			Map oldObject = client.server(s).object.clone()
			s = oldObject << s.clone().with {
				remove 'roles'
				remove 'members'
				remove 'presences'
				remove 'emojis'
				remove 'voice_states'
				it
			}
			server { new Server(client, s) }
			constructed { s }
		},
		MESSAGE_CREATE: { client, data ->
			def msg = new Message(client, data)
			message { msg }
			delegate.content { msg.content }
			sendMessage { msg.channel.&sendMessage }
			sendFile { msg.channel.&sendFile }
			respond { msg.channel.&sendMessage }
			author { msg.author }
			member { msg.author(true) }
			channel { msg.channel }
			server { msg.server }
			serverId { msg.serverId }
		},
		MESSAGE_DELETE: { client, data ->
			bulk { data.bulk as boolean }
			channel { client.channelMap[data.channel_id] }
			message(client.cache.messages[data.channel_id] ?
				client.channel(data.channel_id).cachedLogMap[data.id]
				?: data.id : data.id)
		},
		MESSAGE_UPDATE: { client, data ->
			if (data.containsKey("content")){
				message { new Message(client, data) }
			}else{
				channel { client.channel(data.channel_id) }
				message { client.cache.messages[data.channel_id] ? client.channel(data.channel_id).cachedLogMap[data.id] ?: data.id : data.id }
				embeds { data.embeds }
			}
		},
		MESSAGE_REACTION_ADD: { client, data ->
			channel { client.channel(data.channel_id) }
			user { client.user(data.user_id) }
			message { client.cache.messages[data.channel_id]?.containsKey(data.message_id) ?
				client.cache.messages[data.channel_id][data.message_id] :
				data.message_id }
			emoji { data.emoji }
		},
		MESSAGE_REACTION_REMOVE_ALL: { client, data ->
			channel { client.channel(data.channel_id) }
			message { client.cache.messages[data.channel_id].containsKey(data.message_id) ?
				client.cache.messages[data.channel_id][data.message_id] :
				data.message_id }
		},
		PRESENCE_UPDATE: { client, data ->
			server { client.server(data.guild_id) }
			game { data.game ? new Game(client, data.game) : null }
			status { data.status }
			lastModified { data.last_modified }
			if (data.guild_id)
				member { client.server(data.guild_id).member(data.user) }
			else
				user { client.user(data.user.id) ?: new User(client, data.user) }
			isNew { data.user != client.user(data.user.id) }
		},
		TYPING_START: { client, data ->
			channel { client.channel(data.channel_id) }
			user { def c = client.channel(data.channel_id)
				c.private ? c.user : c.server.member(data.user_id) }
		},
		VOICE_STATE_UPDATE: { client, data ->
			Map v = data
			v << [id: data.user_id]
			VoiceState ase = new VoiceState(client, v)
			voiceState { ase }
			server { ase.server }
			channel { ase.channel }
			member { ase.member }
			constructed { v }
		},
		VOICE_SERVER_UPDATE: { client, data ->
			token { data.token }
			server { client.server(data.guild_id) }
			endpoint { data.endpoint }
		},
		USER_UPDATE: { client, data ->
			user { new User(client, data) }
		}
	]
	
	static {
		defaultEventDataCalls.CHANNEL_DELETE = defaultEventDataCalls.CHANNEL_UPDATE = defaultEventDataCalls.CHANNEL_CREATE
		defaultEventDataCalls.CHANNEL_RECIPIENT_REMOVE = defaultEventDataCalls.CHANNEL_RECIPIENT_ADD
		defaultEventDataCalls.GUILD_BAN_REMOVE = defaultEventDataCalls.GUILD_BAN_ADD
		defaultEventDataCalls.GUILD_MEMBER_UPDATE = defaultEventDataCalls.GUILD_MEMBER_ADD
		defaultEventDataCalls.GUILD_ROLE_UPDATE = defaultEventDataCalls.GUILD_ROLE_CREATE
		defaultEventDataCalls.MESSAGE_REACTION_REMOVE = defaultEventDataCalls.MESSAGE_REACTION_ADD
	}
	
	String customUserAgent = ""
	String getFullUserAgent(){ "$DiscordG.USER_AGENT $customUserAgent" }

	String tokenPrefix
	String rawToken
	String email
	String getEmail(){ cache["user"]?.getOrDefault("email", this.@email) }
	String password
	boolean confirmedBot

	Map eventDataCalls = defaultEventDataCalls
	// if the key is a string, it calls .replace
	// if the key is a pattern, it calls .replaceAll
	Map messageFilters = [
		"@everyone": "@\u200beveryone",
		"@here": "@\u200bhere"
	]
	// name in log
	String logName = "DiscordG"
	// discord gateway version, dont change unless you know what it is and want to
	int gatewayVersion = 6
	// cache tokens from logins. dont turn this off.
	boolean cacheTokens = true
	// path to the token cache file
	String tokenCachePath = "token.json"
	// maximum amount of events that can be handled at the same time
	// increasing might help with lag but takes up more CPU
	int eventThreadCount = 3
	// number of maximum members in a server until discord doesnt send offline members
	// set lower for a possible RAM decrease
	int largeThreshold = 250 
	// request offline members after discord gives us the online ones for large servers
	// set this to false if youre changing large threshold or if it uses too much RAM
	boolean requestMembersOnReady = true
	// adds the READY raw event data to the cache
	// set to false for possible RAM decrease
	boolean copyReady = true
	// retry a request on a 5xx status code
	// generally harmless, 5xx status codes are usually nothing
	boolean retryOn502 = true
	// requests a member on PRESENCE_UPDATE for the joined_at data for a newly discovered member
	// dont turn it on unless you need join dates and cant request offline members
	boolean allowMemberRequesting = false
	// cache reaction data from reaction events
	// not something you can depend on but its left as an option
	boolean cacheReactions = false
	// spread message bulk delete events to separate message delete events
	// true by default for easier handling by bots
	boolean spreadBulkDelete = true
	// timeout for waiting for guild_create events after the READY
	// only for bots and accounts at or over 100 servers
	long serverTimeout = 30_000
	// whitelisted events
	List includedEvents = []
	// blacklisted events
	List excludedEvents = ["TYPING"]
	// for shards, [shardId, shardCount]
	Tuple2 shardTuple
	
	Log log
	Map<String, Object> fields = [:]
	Map<String, Object> extraIdentifyData = [:]
	List<String> mutedChannels = []
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
	// if you want to use global variables through the API object. mostly for utility
	
	@Delegate(excludes = ["parseEvent", "listenerError", "toString"])
	ParentListenerSystem listenerSystem = new ParentListenerSystem(this)
	HTTPClient http
	WSClient ws
	DynamicMap voiceClients = new DynamicMap(keyConverter: DiscordObject.&id)
	Cache cache = new Cache(this, messages: Cache.empty(this),
		reactions: Cache.empty(this), calls: Cache.empty(this))
	String gateway
	boolean gatewayClosed

	Client(Map config = [:]){
		super(null, [:])
		http = new HTTPClient(this)
		client = this

		config.each { k, v ->
			this[k] = v
		}

		log = new Log(logName)

		// oh boy am i gonna get hate for this
		// check reason below where i define these
		addChannelCreateListener()
		addChannelDeleteListener()
		addChannelUpdateListener()
		addChannelRecipientAddListener()
		addChannelRecipientRemoveListener()

		addMessageCreateListener()
		addMessageUpdateListener()
		addMessageDeleteListener()

		addGuildCreateListener()
		addGuildDeleteListener()
		addGuildUpdateListener()

		addGuildMemberAddListener()
		addGuildMemberRemoveListener()
		addGuildMemberUpdateListener()

		addGuildRoleCreateListener()
		addGuildRoleDeleteListener()
		addGuildRoleUpdateListener()

		addPresenceUpdateListener()
		addVoiceStateUpdateListener()
		addVoiceServerUpdateListener()
		addGuildEmojisUpdateListener()
		addUserUpdateListener()
		addGuildMemberChunkListener()
		addGuildSyncListener()
		addRelationshipAddListener()
		addRelationshipRemoveListener()
		addCallCreateListener()
		addCallUpdateListener()
		addCallDeleteListener()
		addUserGuildSettingsUpdateListener()
		addUserSettingsUpdateListener()

		if (requestMembersOnReady) requestMembersOnReady()
	}

	String getToken(){
		tokenPrefix ? "$tokenPrefix $rawToken" : rawToken
	}

	void setToken(String newToken){
		rawToken = newToken
	}
	
	void setCacheReactions(boolean x){
		if (x){
			addMessageReactionAddListener()
			addMessageReactionRemoveListener()
			addMessageReactionRemoveAllListener()
		}
		this.@cacheReactions = x
	}
	
	void setConfirmedBot(boolean ass){
		if (ass) tokenPrefix = "Bot"
		else tokenPrefix = ""
		this.@confirmedBot = ass
	}
	
	boolean isBot(){
		boolean ass = confirmedBot || object["bot"]
		if (ass) tokenPrefix = "Bot"
		else tokenPrefix = ""
		ass
	}

	WSClient getWebSocketClient(){ ws }

	Client startAnew(){
		Client newApi = new Client()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
		def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
		commonProps.each { newApi[it] = !(it in ["http", "ws", "client", "cache", "loaded"]) ? this[it] : null }
		newApi
	}

	void login(String email, String password, boolean threaded=true){
		Closure a = {
			log.info "Getting token..."
			email = email
			password = password
			File tokenCache = new File(tokenCachePath)
			if (!cacheTokens){
				token = requestToken(email, password)
			}else{
				try{
					token = JSONUtil.parse(tokenCache)[(email)]["token"]
				}catch (ex){
					JSONUtil.modify(tokenCache,
						[(email): [token: requestToken(email, password)]])
					token = JSONUtil.parse(tokenCache)[(email)]["token"]
				}
			}
			log.info "Got token."
			login(token, false, false)
		}
		if (threaded) Thread.start(a)
		else a()
	}

	void login(String token, boolean bot = true, boolean threaded=true){
		Closure a = {
			confirmedBot = bot
			token = token
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
			Map original = JSONUtil.parse(tokenFile)
			String originalToken
			if (!original.bots) original.bots = [:]
			if (!original.bots[customBotName]) original.bots[customBotName] = [:]
			if (original.bots[customBotName].token){
				try{
					token = original.bots[customBotName].token
				}catch (ex){
					String newToken = requestToken()
					JSONUtil.dump(tokenFile, original)
					token = newToken
				}
			}else{
				String newToken = requestToken()
				original.bots[customBotName].token = newToken
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
				log.info "Requesting gateway..."
				gateway = http.jsonGet("gateway")["url"]
				if (!gateway.endsWith("/")){ gateway += "/" }
				gateway += "?encoding=json&v=$gatewayVersion"
			}
			WebSocketClient client = new WebSocketClient(new SslContextFactory())
			if (!ws) ws = new WSClient(this)
			log.info "Starting websocket connection..."
			gatewayClosed = false
			client.start()
			client.connect(ws, new URI(gateway), new ClientUpgradeRequest())
		}
		askPool("connect"){
			if (threaded) Thread.start(a)
			else a()
		}
	}

	void logout(boolean exit = false){
		if (!bot) http.post("auth/logout", ["token": token])
		closeGateway(false)
		ws = null
		if (exit) System.exit(0)
	}

	void closeGateway(boolean threaded = true){
		Closure a = {
			gatewayClosed = true
			if (ws){
				ws.keepAliveThread?.interrupt()
				ws.session?.close(1000, "Close")
			}
		}
		if (threaded){ Thread.start(a) }
		else { a() }
	}

	static String parseEvent(str){
		def r = str.toString().toUpperCase().replaceAll(/\s+/, '_')
			.replace("CHANGE", "UPDATE").replaceAll(/^(?!VOICE_)SERVER/, "GUILD")
		knownEvents.contains(r) ? r : (eventAliases[r] ?: "UNRECOGNIZED")
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
		if (fields.containsKey(name)) fields[name] = value
		else throw new MissingPropertyException(name, Client)
	}

	boolean isLoaded(){
		rawToken && ws && ws.loaded && !cache.empty &&
			(!client.bot || !servers.any { it.unavailable })
	}


	String requestToken(String email, String password){
		askPool("login"){
			http.jsonPost("auth/login",
				["email": email, "password": password])["token"]
		}
	}

	User getUser(){ new User(this, cache["user"]) }

	boolean isVerified(){ cache["user"]["verified"] }

	def askPool(String name, String bucket = '$', Closure action){
		pools[name].ask bucket, action
	}

	def sync(servers){
		List guilds = servers instanceof Collection ?
			servers.collect(DiscordObject.&id).collate(25) :
			[[id(servers)]]
		guilds.each {
			ws.send op: 12, d: it
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(servers, String query = "", int limit = 0){
		List guilds = servers instanceof Collection ?
			servers.collect(DiscordObject.&id).collate(25) :
			[id(servers)]
		guilds.each {
			ws.send op: 8, d: [
				guild_id: it,
				query: query,
				limit: limit
			]
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(servers, int limit, String query = ""){
		chunkMembersFor(servers, query, limit)
	}

	String filterMessage(cnt){
		String a = cnt.toString()
		messageFilters.each { k, v ->
			if (k instanceof Pattern)
				a = a.replaceAll(k.pattern(), v)
			// above is for groovy closure support
			else
				a = a.replace(k.toString(), v)
		}
		a
	}

	static String getApplicationLink(app, perms = null){
		"https://discordapp.com/oauth2/authorize?client_id=${id(app)}&scope=bot" +
			(perms ? "&permissions=$perms.value" : "")
	}

	static String getAppLink(app, perms = null){ getApplicationLink(app, perms) }
	static String applicationLink(app, perms = null){ getApplicationLink(app, perms) }
	static String appLink(app, perms = null){ getApplicationLink(app, perms) }

	String mute(t){
		def d = t.class.array || t instanceof Collection ? t.collect { id(it) } : id(t)
		mutedChannels += d
	}

	String mute(...t){ mute(t as List) }

	String getSessionId(){ cache["session_id"] }

	List<Server> getServers() { cache["guilds"]?.list ?: [] }
	Map<String, Server> getServerMap(){ cache["guilds"]?.map ?: [:] }

	List<Presence> getGlobalPresences(){ cache.presences?.list ?: [] }
	Map<String, Presence> getGlobalPresenceMap(){ cache.presences?.map ?: [:] }

	List<User> getAllUsers() {
		// This takes a long time to .unique() on so i found this faster method
		(members + privateChannels*.user - null).groupBy { it.id }
			.values()*.first()
	}
	List<Member> getAllMembers(){ servers*.members.flatten() }
	List<Role> getAllRoles(){ servers*.roles.flatten() }
	List<Emoji> getAllEmojis(){ servers*.emojis.flatten() }

	List<User> getUsers(){ allUsers }
	List<Member> getMembers(){ allMembers }
	List<Role> getRoles(){ allRoles }
	List<Emoji> getEmojis(){ allEmojis }

	Map<String, User> getUserMap(){
		servers*.memberMap.sum()
	}

	Map<String, Map<String, Member>> getMemberMap(){
		Map doo = [:]
		servers.each { doo[it.id] = it.memberMap }
		doo
	}

	Map<String, Role> getRoleMap(){
		servers*.roleMap.sum()
	}

	void changePresence(Map<String, Object> data) {
		def oldPresence = presences.find { it.id == id }
		def payload = [
			status: data.status ?: oldPresence?.status ?: "online",
			game: data.game != null ?
				data.game instanceof String ? [name: data.game, type: 0] :
				data.game instanceof DiscordObject ? data.game.object :
				data.game instanceof Map ? data.game :
				[name: data.game.toString()] :
				oldPresence?.game?.object ?: null,
			since: data.since ?: 0,
			afk: data.afk ?: false,
		]
		askPool("changePresence"){
			ws.send(3, payload)
		}
		for (s in servers){
			cache.guilds[s.id].presences.add(
				user: cache.user,
				game: payload.game,
				status: payload.status,
				guild_id: s.id
			)
		}
		cache.presences.add(
			user: cache.user,
			game: payload.game,
			status: payload.status,
			last_modified: System.currentTimeMillis()
		)
	}

	void status(status){ changePresence(status: status) }
	void play(game){ changePresence(game: game) }
	void playGame(game){ changePresence(game: game) }

	Region getOptimalRegion(){
		requestRegions().find { it.optimal }
	}

	boolean isPrivateChannel(c){
		cache["private_channels"].containsKey(id(c))
	}
	
	List<Channel> getPrivateChannels(){
		cache["private_channels"]?.list ?: []
	}

	Map<String, Channel> getPrivateChannelMap(){
		cache["private_channels"]?.map ?: [:]
	}

	Channel privateChannel(c){ find(cache.private_channels, c) }
	List<Channel> privateChannels(c){ findAll(cache.private_channels, c) }

	DiscordListCache getUserDmChannelCache(){
		DiscordListCache dlc = new DiscordListCache([], this, Channel)
		dlc.store = (cache["private_channels"].values().findAll { it.type == 1 }
			.collectEntries { [(it.recipients.keySet()[0]): it] })
		dlc
	}

	Map<String, Channel> getUserDmChannelMap(){
		userDmChannelCache.map
	}

	DiscordListCache getDmChannelCache(){
		new DiscordListCache(cache.private_channels.values().findAll { it.type == 1 },
			this, Channel)
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

	List<Channel> getTextChannels(){
		privateChannels + servers*.textChannels.flatten() ?: []
	}

	Map<String, Channel> getTextChannelMap(){
		servers*.textChannelMap.sum() + privateChannelMap ?: [:]
	}

	List<Channel> getVoiceChannels(){
		servers*.voiceChannels.sum() ?: []
	}

	Map<String, Channel> getVoiceChannelMap(){
		servers*.voiceChannelMap.sum() ?: [:]
	}

	List<Channel> getChannels(){
		serverChannels + privateChannels ?: []
	}

	Map<String, Channel> getChannelMap(){
		serverChannelMap + privateChannelMap ?: [:]
	}
	
	List<Channel> getServerChannels(){
		cache.guilds.mapList*.channels*.list.flatten()
	}
	
	Map<String, Channel> getServerChannelMap(){
		cache.guilds.mapList*.channels*.map.sum()
	}
	
	Map<String, String> getChannelServerIdMap(){
		Map a = [:]
		cache.guilds.each { x, y ->
			y.channels.each { m, n ->
				a[m] = x
			}
		}
		a
	}

	Member member(t){ findNested(cache.guilds, "members", t) }
	List<Member> members(t){ findAllNested(cache.guilds, "members", t) }
	Member member(s, u){ find(cache.guilds[s].members, u) }
	List<Member> members(s, u){ findAll(cache.guilds[s].members, u) }

	User user(u){ member(u)?.user ?:
		findNested(cache.private_channels, 'recipients', u) ?:
		relation(u)?.user }
	Relationship relationship(u){ find(cache.relationships, u) }
	Relationship relation(u){ relationship(u) }

	Server server(s){ find(cache.guilds, s) }

	Channel textChannel(...args){ channels(*args).find { it.text } }
	List<Channel> textChannels(...args){ channels(*args).findAll { it.text } }
	Channel voiceChannel(...args){ channels(*args).find { it.voice } }
	List<Channel> voiceChannels(...args){ channels(*args).findAll { it.voice } }

	Channel channel(c){ serverChannel(c) ?: privateChannel(c) }
	List<Channel> channels(c){ serverChannels(c) ?: privateChannels(c) }
	Channel serverChannel(c){ findNested(cache.guilds, "channels", c) }
	List<Channel> serverChannels(c){ findAllNested(cache.guilds, "channels", c) }
	Channel channel(s, c){ find(cache.guilds[s].channels, c) }
	List<Channel> channels(s, c){ findAll(cache.guilds[s].channels, c) }

	Role role(r){ findNested(cache.guilds, "roles", r) }
	List<Role> roles(r){ findAllNested(cache.guilds, "roles", r) }
	Role role(s, r){ find(cache.guilds[id(s)], r) }
	List<Role> roles(s, r){ findAll(cache.guilds[id(s)], r) }

	Call ongoingCall(c){ cache.calls[id(c)] ? new Call(this, cache.calls[id(c)]) : null }

	List<Relationship> getRelationships(){ cache.relationships.list }
	Map<String, Relationship> getRelationshipMap(){ cache.relationships.map }

	List getEverything(){
		servers + roles + members + channels
	}

	// WEBSOCKET LISTENERS

	void addGuildMemberAddListener(){
		addListener("MEMBER"){ Map d ->
			cache["guilds"][d.json.guild_id]["members"].add(d.json)
			cache["guilds"][d.json.guild_id]["member_count"]++
		}
	}

	void addGuildMemberRemoveListener(){
		addListener("MEMBER_REMOVED"){ Map d ->
			if (!cache.guilds[d.json.guild_id]) return
			cache["guilds"][d.json.guild_id]["members"].remove(d.json.user.id)
			cache["guilds"][d.json.guild_id]["member_count"]--
		}
	}

	void addGuildRoleCreateListener(){
		addListener("ROLE"){ Map d ->
			cache["guilds"][d.json.guild_id]["roles"].add(d.json.role + [guild_id: d.json.guild_id])
		}
	}

	void addGuildRoleDeleteListener(){
		addListener("ROLE_DELETE"){ Map d ->
			cache["guilds"][d.json.guild_id]["roles"].remove(d.json.role_id)
		}
	}

	void addChannelCreateListener(){
		addListener("CHANNEL_CREATE"){ Map d ->
			if (d.constructed.guild_id){
				cache["guilds"][d.constructed.guild_id]["channels"].add(d.constructed)
			}else{
				cache["private_channels"].add(d.constructed)
			}
		}
	}

	void addChannelDeleteListener(){
		addListener("CHANNEL_DELETE"){ Map d ->
			if (d.constructed.guild_id){
				cache["guilds"][d.constructed.guild_id]["channels"].remove(d.constructed.id)
			}else{
				cache["private_channels"].remove(d.constructed.id)
			}
		}
	}

	void addChannelUpdateListener(){
		addListener("CHANNEL_UPDATE"){ Map d ->
			if (d.constructed.guild_id) {
				cache["guilds"][d.constructed.guild_id]["channels"][d.constructed.id] <<
						d.constructed
			}else{
				cache["private_channels"][d.constructed.id] << d.constructed
			}
		}
	}

	void addMessageCreateListener(){
		addListener("MESSAGE"){ Map d ->
			if (cache.messages[d.json.channel_id]){
				cache.messages[d.json.channel_id].add(d.json)
			}else{
				def a = new DiscordListCache([d.json], this, Message)
				a.root = channel(d.json.channel_id)
				cache.messages[d.json.channel_id] = a
			}
		}
	}

	void addMessageUpdateListener(){
		addListener("MESSAGE_UPDATE"){ Map d ->
			if (cache.messages[d.json.channel_id]){
				if (cache.messages[d.json.channel_id][d.json.id]){
					cache.messages[d.json.channel_id][d.json.id] << d.json
				}else{
					cache.messages[d.json.channel_id].add(d.json)
				}
			}else{
				def a = new DiscordListCache([d.json], this, Message)
				a.root = channel(d.json.channel_id)
				cache.messages[d.json.channel_id] = a
			}
		}
	}

	void addMessageDeleteListener(){
		addListener("message delete") { Map d ->
			cache.messages[d.json.channel_id]?.remove(d.json.id)
		}
	}

	void addGuildCreateListener(){
		addListener("SERVER"){ Map d ->
			Map server = d.constructed
			if (cache["guilds"][server["id"]]){
				cache["guilds"][server["id"]] << server
			}else{
				cache["guilds"].add(server)
			}
		}
	}

	void addGuildDeleteListener(){
		addListener("SERVER_DELETED"){ Map d ->
			cache["guilds"].remove(d.server.id)
		}
	}

	void addGuildMemberUpdateListener(){
		addListener("MEMBER_UPDATED"){ Map d ->
			cache["guilds"][d.json.guild_id]["members"][d.json.user.id]?.leftShift(d.json)
		}
	}

	void addGuildRoleUpdateListener(){
		addListener("ROLE_UPDATE"){ Map d ->
			if (cache["guilds"][d.json.guild_id]["roles"][d.json.role.id])
				cache["guilds"][d.json.guild_id]["roles"][d.json.role.id] << d.json.role
			else cache["guilds"][d.json.guild_id]["roles"][d.json.role.id] = d.json.role + [guild_id: d.json.guild_id]
		}
	}

	void addGuildUpdateListener(){
		addListener("SERVER_UPDATED"){ Map d ->
			cache["guilds"][d.json.id] = d.constructed
		}
	}

	void addPresenceUpdateListener(){
		addListener("presence change"){ Map d ->
			if (d.json.guild_id){
				if (cache.guilds[d.json.guild_id].unavailable) return
				if (d.json.user.avatar){
					if (cache.guilds[d.json.guild_id].members[d.json.user.id]){
						cache.guilds[d.json.guild_id].members[d.json.user.id].user <<
							d.json.user
					}else{
						cache.guilds[d.json.guild_id].members.add(d.json.with {
							Map di = it.clone()
							di.remove("status")
							di.remove("game")
							if (allowMemberRequesting)
								di << requestMember(d.json.guild_id, d.json.user.id).object
							di
						})
					}
				}
				if (d.json.status == "offline")
					cache.guilds[d.json.guild_id].presences?.remove(d.json.user.id)
				else {
					cache.guilds[d.json.guild_id].presences?.add(d.json.with {
						Map di = it.clone()
						di.remove("roles")
						di.remove("nick")
						di
					} + [id: d.json.user.id])
				}
			}else{
				if (d.isNew){
					cache.private_channels.each { k, v ->
						if (v.recipients.containsKey(d.json.user.id))
							v.recipients[d.json.user.id] = d.json.user
					}
				}
				if (d.json.status == "offline")
					cache.presences.remove(d.json.user.id)
				else
					cache.presences[d.json.user.id] = d.json + [id: d.json.user.id]
			}
		}
	}

	void addChannelRecipientAddListener(){
		addListener("channel recipient add"){ Map d ->
			cache.private_channels[d.json.channel_id].recipients.add(d.json.user)
		}
	}

	void addChannelRecipientRemoveListener(){
		addListener("channel recipient remove"){ Map d ->
			cache.private_channels[d.json.channel_id].recipients.remove(d.json.user.id)
		}
	}

	void addVoiceStateUpdateListener(){
		addListener("voice state change"){ Map d ->
			def gi = d.constructed.guild_id ?:
				cache["guilds"].find { it.channels[d.constructed.channel_id] }.id
			if (d.constructed.channel_id){
				if (cache["guilds"][gi]["voice_states"][d.constructed.id])
					cache["guilds"][gi]["voice_states"][d.constructed.id] << d.constructed
				else cache["guilds"][gi]["voice_states"][d.constructed.id] = d.constructed
				if (d.constructed.id == this){
					VoiceClient vc = voiceClients[d.constructed.id]
					if (vc) vc.handleVoiceStateUpdate(d)
				}
			}else{
				cache["guilds"][gi]["voice_states"].remove(d.constructed.id)
			}
		}
	}

	void addVoiceServerUpdateListener(){
		addListener("voice server change"){ Map d ->
			VoiceClient vc = voiceClients[d.json.guild_id]
			if (vc) vc.handleVoiceServerUpdate(d)
		}
	}

	void addGuildEmojisUpdateListener(){
		addListener("guild emojis change"){ Map d ->
			cache["guilds"][d.json.guild_id]["emojis"] = d.emojis
		}
	}

	void addUserUpdateListener(){
		addListener("user change"){ Map d ->
			cache["user"] << d.json
		}
	}

	void addGuildMemberChunkListener(){
		addListener("guild members chunk"){ Map d ->
			d.members.each {
				cache["guilds"][d.guild_id]["members"].add(it + [guild_id: d.guild_id])
			}
		}
	}

	void addGuildSyncListener(){
		addListener("guild sync"){ Map d ->
			d.members.each {
				cache["guilds"][d.id]["members"].add(it + [guild_id: d.guild_id])
			}
			d.presences.each {
				cache["guilds"][d.id]["presences"].add(it + [guild_id: d.guild_id])
			}
			cache["guilds"][d.id]["large"] = d.large
		}
	}

	void addUserNoteUpdateListener(){
		addListener("user note update"){ Map d ->
			d.note ? (cache.notes[d.id] = d.note) :
				cache.notes.remove(d.id)
		}
	}

	void addMessageReactionAddListener(){
		addListener("message reaction add"){ Map d ->
			def fabricated = [user_id: d.json.user_id,
					emoji: d.json.emoji]
			cache.reactions.containsKey(d.json.message_id) ?
				cache.reactions[d.json.message_id].add(fabricated) :
				cache.reactions.put(d.json.message_id, [fabricated])
		}
	}

	void addMessageReactionRemoveListener(){
		addListener("message reaction remove"){ Map d ->
			if (cache.reactions.containsKey(d.json.message_id))
				cache.reactions[d.json.message_id].with {
					remove(find { it.user_id == d.json.user_id && it.emoji == d.json.emoji })
				}
		}
	}

	void addMessageReactionRemoveAllListener(){
		addListener("message reaction remove all"){ Map d ->
			if (cache.reactions.containsKey(d.json.message_id))
				cache.reactions.remove(d.json.message_id)
		}
	}

	void addRelationshipAddListener(){
		addListener("relationship"){ Map d ->
			cache.relationships.add(d.json)
		}
	}

	void addRelationshipRemoveListener(){
		addListener("relationship remove"){ Map d ->
			cache.relationships.remove(d.json.id)
		}
	}

	void addCallCreateListener(){
		addListener("call"){ Map d ->
			cache.calls[d.json.channel_id] = d.json
		}
	}

	void addCallUpdateListener(){
		addListener("call update"){ Map d ->
			cache.calls[d.json.channel_id] << d.json
		}
	}

	void addCallDeleteListener(){
		addListener("call delete"){ Map d ->
			cache.calls.remove(d.json.channel_id)
		}
	}

	void addUserGuildSettingsUpdateListener(){
		addListener("user guild settings update"){ Map d ->
			cache["user_guild_settings"][d.json.guild_id] =
				(cache["user_guild_settings"] ?: [:]) + d.json
		}
	}

	void addUserSettingsUpdateListener(){
		addListener("user settings update"){ Map d ->
			cache["user_settings"] << d.json
		}
	}

	def requestMembersOnReady(){
		addListener("ready"){
			chunkMembersFor(servers.findAll { it.large })
		}
	}

	// add this yourself
	void addReconnector(){
		addListener("close"){
			if (!gatewayClosed && it.code < 4000)
				ws.reconnect()
		}
	}

	// HTTP REQUESTS

	List<Server> requestServers(boolean checkCache = true){
		http.jsonGet("users/@me/guilds").collect {
			Map object = it
			if (checkCache && !it.unavailable){
				def cached = cache["guilds"][it.id]
				object.members = cached.members
				object.channels = cached.channels
				object.presences = cached.presences
				object.voice_states = cached.voice_states
				object.roles = cached.roles
				object.emojis = cached.emojis
				object.large = cached.large
			}
			new Server(this, object)
		}
	}

	List<Channel> requestPrivateChannels(){
		http.jsonGet("users/@me/channels").collect { new Channel(this, it) }
	}

	void moveMemberVoiceChannel(s, u, vc){
		editMember(channel_id: id(vc), s, u)
	}

	void editRoles(s, u, Collection r) {
		editMember(roles: r.collect(this.&id), s, u)
	}

	void addRoles(s, u, Collection r) {
		editRoles(s, u, cache.guilds[s].members[u].roles*.id + r)
	}

	void addRole(s, m, r){
		http.put("guilds/${id(s)}/members/${id(m)}/roles/${id(r)}")
	}

	void removeRole(s, m, r){
		http.delete("guilds/${id(s)}/members/${id(m)}/roles/${id(r)}")
	}

	String changeOwnServerNick(s, String nick){
		askPool("changeNick"){
			http.jsonPatch("guilds/${id(s)}/members/@me/nick",
				[nick: nick])["nick"]
		}
	}

	String changeServerNick(s, m, String nick){
		editMember(nick: nick, s, m)
	}

	void kick(s, m){
		http.delete("guilds/${id(s)}/members/${id(m)}")
	}

	List<Server.Ban> requestBans(s) {
		http.jsonGet("guilds/${id(s)}/bans").collect { new Server.Ban(this, it) }
	}

	List<Invite> requestServerInvites(s){
		http.jsonGet("guilds/${id(s)}/invites").collect { new Invite(this, it) }
	}

	List<Region> requestRegions(s){
		http.jsonGet("guilds/${id(s)}/regions").collect { new Region(this, it) }
	}

	List<Integration> requestIntegrations(s){
		http.jsonGet("guilds/${id(s)}/integrations").collect { new Integration(this, it) }
	}

	Integration createIntegration(s, type, id){
		new Integration(this, http.jsonPost("guilds/${id(s)}/integrations",
			[type: type.toString(), id: id.toString()]))
	}

	void ban(s, u, int d = 0) {
		http.put("guilds/${id(s)}/bans/${id(u)}?delete-message-days=$d", [:])
	}

	void unban(s, u) {
		http.delete("guilds/${id(s)}/bans/${id(u)}")
	}

	int checkPrune(s, int d){
		http.jsonGet("guilds/${id(s)}/prune?days=$d")["pruned"]
	}

	int prune(s, int d){
		http.jsonPost("guilds/${id(s)}/prune?days=$d")["pruned"]
	}

	Role createRole(Map data, s) {
		if (data["color"] instanceof Color) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions) data["permissions"] = data["permissions"].value
		Role createdRole = new Role(this, http.jsonPost("guilds/${id(s)}/roles", [:]))
		editRole(data, s, createdRole)
	}

	Role editRole(Map data, s, r) {
		if (data["color"] instanceof Color) data["color"] = data["color"].value
		if (data["permissions"] instanceof Permissions) data["permissions"] = data["permissions"].value
		new Role(this, http.jsonPatch("guilds/${id(s)}/roles/${id(r)}", data))
	}

	void deleteRole(s, r) {
		http.delete("guilds/${id(s)}/roles/${id(r)}")
	}
	
	List<Role> editRolePositions(Map mods, s){
		http.jsonPatch("guilds/${id(s)}/roles", mods.collect { k, v ->
			[id: id(k), position: v]}).collect { new Role(this, it + [guild_id: id(s)]) }
	}
	
	List<Channel> editChannelPositions(Map mods, s){
		http.jsonPatch("guilds/${id(s)}/channels", mods.collect { k, v ->
			[id: id(k), position: v]}).collect { Channel.construct(this, it, id(s)) }
	}

	List<Webhook> requestServerWebhooks(s){
		http.jsonGet("guilds/${s}/webhooks").collect { new Webhook(this, it) }
	}

	List<Member> requestMembers(s, int max=1000, boolean updateCache=true){
		def i = id(s)
		List members = http.jsonGet("guilds/$i/members?limit=${max}")
		if (max > 1000){
			for (int m = 1; m < (int) Math.ceil(max / 1000) - 1; m++){
				members += http.jsonGet("guilds/$i/members?after=${(m * 1000) + 1}&limit=1000")
			}
			members += http.jsonGet("guilds/$i/members?after=${(int)((Math.ceil(max / 1000) - 1) * 1000)+1}&limit=1000")
		}
		if (updateCache){
			client.cache["guilds"][i]["members"] = new DiscordListCache(members.collect { it + ["guild_id": i] + it["user"] }, this, Member)
			client.cache["guilds"][i]["member_count"] = members.size()
		}
		members.collect { new Member(this, it + ["guild_id": i]) }
	}

	Member requestMember(s, m){ new Member(this,
		http.jsonGet("guilds/${id(s)}/members/${id(m)}")) }

	List<Emoji> requestEmojis(s){ http.jsonGet("guilds/${id(s)}/emojis")
		.collect { new Emoji(this, it) } }

	Emoji createEmoji(Map data, s){
		new Emoji(this, http.jsonPost("guilds/${id(s)}/emojis",
			patchData(data, ["image"])))
	}

	Emoji editEmoji(Map data, s, emoji){
		new Emoji(this, http.jsonPatch("guilds/${id(s)}/emojis/${id(emoji)}", data))
	}

	Webhook requestServerWebhook(s, w){
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

	Server.Embed requestEmbed(s){ new Server.Embed(this, http.jsonGet("guilds/${id(s)}/embed")) }

	Server.Embed editEmbed(Map data, s, e){ new Server.Embed(this,
		http.jsonPatch("guilds/${id(s)}/embed", data)) }

	List<Connection> requestConnections(){
		http.jsonGet("users/@me/connections").collect { new Connection(this, it) }
	}

	User edit(Map data){
		Map map = [avatar: user.avatarHash, email: this?.email,
			password: this?.password, username: user.username]
		Map response = http.jsonPatch("users/@me", map <<
			patchData(data))
		email = response.email
		password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : password
		token = response.token
			.replaceFirst(java.util.regex.Pattern.quote(tokenPrefix), "")
			.trim()
		cache["user"]["email"] = response.email
		cache["user"]["verified"] = response.verified
		new User(this, response)
	}

	Profile requestProfile(a){
		new Profile(this, http.jsonGet("users/${id(a)}/profile"))
	}

	Application requestApplication(){
		new Application(this, http.jsonGet("oauth2/applications/@me"))
	}

	List<Application> requestApplications(){
		http.jsonGet("oauth2/applications").collect { new Application(this, it) }
	}

	Application requestApplication(a){
		new Application(this, http.jsonGet("oauth2/applications/${id(a)}"))
	}

	Application editApplication(Map data, a){
		new Application(this, http.jsonPut("oauth2/applications/${id(a)}", map <<
			patchData(data)))
	}

	void deleteApplication(a){
		http.delete("oauth2/applications/${id(a)}")
	}

	Application createApplication(Map data){
		Map map = [icon: null, description: "", redirect_uris: [], name: ""]
		new Application(this, http.jsonPost("oauth2/applications", map <<
			patchData(data)))
	}

	BotAccount createApplicationBotAccount(a, String oldAccountToken = null){
		new BotAccount(this, http.jsonPost("oauth2/applications/${id(a)}/bot",
			(oldAccountToken == null) ? [:] : [token: oldAccountToken]))
	}

	List<Region> requestRegions(){
		http.jsonGet("voice/regions").collect { new Region(this, it) }
	}

	List<User> queueUsers(String query, int limit=25){
		http.jsonGet("users?q=${URLEncoder.encode(query)}&limit=$limit")
			.collect { new User(this, it) }
	}

	User requestUser(i){ new User(this, http.jsonGet("users/${id(i)}")) }
	Server requestServer(i){ new Server(this, http.jsonGet("guilds/${id(i)}")) }
	Channel requestChannel(i){ new Channel(this, http.jsonGet("channels/${id(i)}")) }

	Server editServer(Map data, s) {
		new Server(this, object << http.jsonPatch("guilds/${id(s)}",
			patchData(data)))
	}

	void leaveServer(s) {
		http.delete("users/@me/guilds/${id(s)}")
	}

	void deleteServer(s) {
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

	Channel requestServerChannel(s, c){
		new Channel(this, http.jsonGet("guilds/${id(s)}/channels/${id(c)}"))
	}

	List<Channel> requestServerChannels(s){
		http.jsonGet("guilds/${id(s)}/channels").collect { new Channel(this, it) }
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
		askPool("editMembers", id(s)){
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
		http.put("users/@me/relationships/${id(u)}", [type: type.toInteger()])
	}

	void removeRelationship(u){
		http.delete("users/@me/relationships/${id(u)}")
	}

	List<Invite> requestChannelInvites(c){
		http.jsonGet("channels/${id(c)}/invites").collect { new Invite(this, it) }
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
		String type = data.type ?: (role(i) ? "role" : "member")
		int allowBytes = data.allow?.toInteger() ?: 0
		int denyBytes = data.deny?.toInteger() ?: 0
		http.put("channels/${id(c)}/permissions/${i}",
			[allow: allowBytes, deny: denyBytes, id: i, type: type])
	}

	void deleteChannelOverwrite(c, o){
		http.delete("channels/${id(c)}/permissions/${id(o)}")
	}

	Webhook createWebhook(Map data = [:], c){
		new Webhook(client, http.jsonPost("channels/${id(c)}/webhooks",
			patchData(data)))
	}

	List<Webhook> requestChannelWebhooks(c){
		http.jsonGet("channels/${id(c)}/webhooks").collect { new Webhook(this, it) }
	}

	Webhook requestWebhook(w){
		new Webhook(this, http.jsonGet("webhooks/${id(w)}"))
	}

	Message sendMessage(Map data, c){
		if (data.containsKey("content")){
			data.content = client.filterMessage(data.content)
			if (!data.content || data.content.size() > 2000)
				throw new MessageInvalidException(data.content)
		}
		boolean isWebhook = MiscUtil.defaultValueOnException { data.webhook && c?.id && c?.token }
		Closure clos = {
			new Message(this, http.jsonPost(isWebhook ?
				"webhooks/${id(c)}/$c.token" :
				"channels/${id(c)}/messages", [channel_id: id] << data))
		}
		isWebhook ? clos() : askPool("sendMessages", getChannelQueueName(c), clos)
	}

	Message sendMessage(c, content, tts = false){ sendMessage(c, content: content, tts: tts) }

	Message editMessage(Map data, c, m){
		if (data.content != null){
			data.content = client.filterMessage(data.content)
			if (!data.content || data.content.size() > 2000)
				throw new MessageInvalidException(data.content)
		}
		askPool("sendMessages", getChannelQueueName(c)){ // that's right, they're in the same bucket
			new Message(this, http.jsonPatch("channels/${id(c)}/messages/${id(m)}", data))
		}
	}

	void deleteMessage(c, m){
		askPool("deleteMessages",
			getChannelQueueName(c)){ http.delete("channels/${id(c)}/messages/${id(m)}") }
	}

	def sendFileRaw(Map data = [:], c, file){
		boolean isWebhook = MiscUtil.defaultValueOnException { data.webhook && c?.id && c?.token }
		List fileArgs = []
		if (file instanceof File){
			if (data["filename"]){
				fileArgs += file.bytes
				fileArgs += data["filename"]
			}else fileArgs += file
		}else{
			fileArgs += ConversionUtil.getBytes(file)
			if (!data["filename"]) throw new IllegalArgumentException("Tried to send non-file class ${file.class} and gave no filename")
			fileArgs += data["filename"]
		}
		def url = http.baseUrl + (isWebhook ?
			"webhooks/${id(c)}/${c.token}" :
			"channels/${id(c)}/messages")
		def aa = Unirest.post(url)
			.header("Authorization", token)
			.header("User-Agent", fullUserAgent)
			.field("content", data["content"] == null ? "" : data["content"].toString())
			.field("tts", data["tts"] as boolean)
		if (fileArgs.size() == 1){
			aa = aa.field("file", fileArgs[0])
		}else if (fileArgs.size() == 2){
			aa = aa.field("file", fileArgs[0], fileArgs[1])
		}
		def clos = {
			JSONUtil.parse(aa.asString().body)
		}
		isWebhook ? clos() : askPool("sendMessages", getChannelQueueName(c), clos)
	}

	Message sendFile(Map data, c, implicatedFile, filename = null) {
		def file
		if (implicatedFile.class in [File, String]) file = implicatedFile as File
		else file = ConversionUtil.getBytes(implicatedFile)
		new Message(this, sendFileRaw((filename ? [filename: filename] : [:]) << data, c, file))
	}

	Message sendFile(c, implicatedFile, filename = null){
		sendFile([:], c, implicatedFile, filename)
	}

	Message requestMessage(c, ms, boolean addToCache = true){
		def ch = id(c)
		def m = new Message(this,
			http.jsonGet("channels/$ch/messages/${id(ms)}"))
		if (addToCache){
			if (cache.messages[ch]) cache.messages[ch].add(m)
			else {
				cache.messages[ch] = new DiscordListCache([m], this, Message)
				cache.messages[ch].root = channel(ch)
			}
		}
		m
	}

	Message message(c, m, boolean request = true){
		boolean inCache = cache.messages[id(c)].containsKey(id(m))
		if (inCache) new Message(this, cache.messages[id(c)][id(m)])
		else if (request) requestMessage(c, m)
	}

	String getChannelQueueName(c){
		serverChannel(id(c)) ?: 'dm'
	}

	def pinMessage(c, m){
		http.put("channels/${id(c)}/pins/${id(m)}")
	}

	def unpinMessage(c, m){
		http.delete("channels/${id(c)}/pins/${id(m)}")
	}

	Collection<Message> requestPinnedMessages(c){
		http.jsonGet("channels/${id(c)}/pins").collect { new Message(this, it) }
	}

	void reactToMessage(c, m, e){
		http.put("channels/${id(c)}/messages/${id(m)}/" +
			"reactions/${translateEmoji(e)}/@me")
	}

	void unreactToMessage(c, m, e, u = "@me"){
		http.delete("channels/{id(c)}/messages/${id(m)}/" +
			"reactions/${translateEmoji(e)}/${id(u)}")
	}

	List<User> requestReactors(c, m, e, int limit = 100){
		http.jsonGet("channels/${id(c)}/messages/${id(m)}/reactions/" +
			translateEmoji(e) + "?limit=$limit").collect { new User(this, it) }
	}

	String translateEmoji(emoji, s = null){
		if (emoji ==~ /\d+/){
			translateEmoji(emojis.find { it.id == emoji })
		}else if (emoji instanceof Emoji){
			"$emoji.name:$emoji.id"
		}else if (emoji ==~ /\w+/){
			def i = ((server(s)?.emojis ?: []) + (emojis - server(s)?.emojis)
				).find { it.name == emoji }?.id
			i ? "$emoji:$i" : ":$emoji:"
		}else{
			emoji
		}
	}

	List<Message> requestChannelLogs(c, int max = 100, boundary = null,
		boundaryType = 'before'){
		def cached = cache.messages[id(c)]
		if (!boundary && cached?.size() > max) return cached.values().sort {
			it.id }[-1..-max].collect { new Message(this, it) }
		def l = rawRequestChannelLogs(c, max, boundary, boundaryType)
		if (boundaryType in ['around', 'after']) l = l.reverse()
		if (cached) l.each(cache.messages[id(c)].&add)
		else cache.messages[id(c)] = new DiscordListCache(l, this, Message)
		l.collect { new Message(this, it) }
	}

	List<Message> forceRequestChannelLogs(c, int m = 100, b = null,
		bt = 'before'){ rawRequestChannelLogs(c, m, b, bt).collect { new Message(this, it) } }

	// after and around sorted old-new
	// before sorted new-old
	List rawRequestChannelLogs(c, int max, boundary = null, boundaryType = 'before'){
		Map params = [limit: max > 100 ? 100 : max]
		if (boundary){
			if (boundaryType && !(boundaryType in ['before', 'after', 'around']))
				throw new IllegalArgumentException('Boundary type has to be before, after or around')
			params[boundaryType ?: 'before'] = id(boundary)
		}
		List messages = rawRequestChannelLogs(params, c)
		if (max > 100){
			if (boundaryType == 'after'){
				for (int m = 1; m < Math.floor(max / 100); m++) {
					messages += rawRequestChannelLogs(c,
							after: messages.last().id, limit: 100)
				}
				if (max % 100) messages += rawRequestChannelLogs(c,
						after: messages.last().id, limit: max % 100)
			}else if (boundaryType == 'around'){
				def age = max - 100
				def af = age.intdiv(2)
				def bef = age.intdiv(2) + (age % 2)
				for (int m = 1; m < Math.floor(bef / 100); m++) {
					messages = rawRequestChannelLogs(c, before: messages.first().id,
							limit: 100).reverse() + messages
				}
				if (bef % 100) messages = rawRequestChannelLogs(c, before: messages.first().id,
						limit: bef % 100).reverse() + messages
				for (int m = 1; m < Math.floor(af / 100); m++) {
					messages += rawRequestChannelLogs(c, after: messages.last().id,
							limit: 100)
				}
				if (af % 100) messages += rawRequestChannelLogs(c, before: messages.last().id,
						limit: af % 100)
			}else{
				for (int m = 1; m < Math.floor(max / 100); m++) {
					messages += rawRequestChannelLogs(c,
							before: messages.last().id, limit: 100)
				}
				if (max % 100) messages += rawRequestChannelLogs(c,
						before: messages.last().id, limit: max % 100)
			}
		}
		messages
	}

	List rawRequestChannelLogs(Map data = [:], c){
		String parameters = data ? "?" + data.collect { k, v ->
			URLEncoder.encode(k.toString()) + "=" + URLEncoder.encode(v.toString())
		}.join("&") : ""
		http.jsonGet("channels/${id(c)}/messages$parameters")
	}

	def bulkDeleteMessages(c, Collection ids){
		askPool("bulkDeleteMessages"){
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
		new Channel(this, http.jsonPost("users/@me/channels",
			[recipient_id: id(u)]))
	}
}
