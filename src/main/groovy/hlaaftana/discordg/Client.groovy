package hlaaftana.discordg

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.collections.DynamicMap;
import hlaaftana.discordg.logic.ActionPool
import hlaaftana.discordg.logic.ParentListenerSystem;
import hlaaftana.discordg.net.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.objects.*
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

	String customUserAgent = ""
	String getFullUserAgent(){ "$DiscordG.USER_AGENT $customUserAgent" }

	String tokenPrefix
	public String token
	String email
	String getEmail(){
		cache["user"]?.getOrDefault("email", this.@email)
	}
	String password
	boolean confirmedBot
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

	Map messageFilters = [
		"@everyone": "@\u200beveryone",
		"@here": "@\u200bhere"
	] // if the key is a string, it calls .replace. if the key is a pattern, it calls .replaceAll
	List<String> mutedChannels = []
	String logName = "DiscordG"
	Log log
	int gatewayVersion = 6 // changing this is not recommended
	boolean cacheTokens = true
	String tokenCachePath = "token.json"
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	int largeThreshold = 250 // if your bot is on tons of big servers, setting this to lower numbers might help your memory
	boolean requestMembersOnReady = true
	boolean copyReady = true // adds json to ready to archive the initial ready
	boolean retryOn502 = true // retries your request if you get a 5xx status code, lke the red messages you get in the regular client
	boolean allowMemberRequesting = false // requests new member info on PRESENCE_UPDATE if it sees a new member. Turn this on if you don't have issues with dog
	boolean enableListenerCrashes = true
	def enableListenerCrashes(){ enableListenerCrashes = true }
	def disableListenerCrashes(){ enableListenerCrashes = false }
	boolean enableEventRegisteringCrashes = true
	def enableEventRegisteringCrashes(){ enableEventRegisteringCrashes = true }
	def disableEventRegisteringCrashes(){ enableEventRegisteringCrashes = false }
	long serverTimeout = 30_000
	List includedEvents = []
	List excludedEvents = [Events.TYPING]
	Tuple2 shardTuple // tuple of shard id and shard count.

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
	Map<String, Object> fields = [:]
	@Delegate(excludes = ["parseEvent", "listenerError", "toString"])
	ParentListenerSystem listenerSystem = new ParentListenerSystem(this)
	HTTPClient http
	WSClient ws
	DynamicMap voiceClients = {
		DynamicMap map = new DynamicMap()
		map.keyConverter = DiscordObject.&id
		map
	}()
	Cache cache = Cache.empty(this)
	Cache voiceData = Cache.empty(this)
	String gateway
	Cache messages = Cache.empty(this)
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
		addGuildEmojisUpdateListener()
		addUserUpdateListener()
		addGuildMemberChunkListener()
		addGuildSyncListener()

		if (requestMembersOnReady) requestMembersOnReady()
	}


	String getToken(){
		tokenPrefix ? "$tokenPrefix ${this.@token}" : this.@token
	}

	void setToken(String newToken){
		this.@token = newToken
	}

	WSClient getWebSocketClient(){ ws }

	Client startAnew(){
		Client newApi = new Client()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
		def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
		commonProps.each { newApi[it] = !(it in ["http", "ws", "client", "cache", "voiceData", "loaded"]) ? this[it] : null }
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

	String parseEvent(param){ Events.get(param).type }

	def listenerError(String event, Throwable ex, Closure closure, data){
		if (enableListenerCrashes) ex.printStackTrace()
		log.info "Ignoring exception from event $event"
	}

	Closure listener(event, boolean temporary = false, Closure closure){
		Closure ass
		ass = { Map d, Closure internal ->
			//d["rawClosure"] = closure
			//d["closure"] = ass
			Closure copy = closure.clone()
			copy.delegate = d
			copy.parameterTypes.size() == 2 ? copy(copy.delegate, internal) : copy(copy.delegate)
		}
		addListener(event, temporary, ass)
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
		this.@token &&
			ws &&
			ws.loaded &&
			!cache.empty &&
			(!client.bot || !servers.any { it.unavailable })
	}


	String requestToken(String email, String password){
		askPool("login"){
			http.jsonPost("auth/login",
				["email": email, "password": password])["token"]
		}
	}

	User getUser(){ new User(this, cache["user"]) }
	Map getObject(){ cache["user"] ?: [:] }
	void setObject(Map lol){ cache["user"] = lol }

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

	String mute(t){
		def d = t.class.array || t instanceof Collection ? t.collect { id(it) } : id(t)
		mutedChannels += d
	}

	String mute(...t){ mute(t as List) }

	String getSessionId(){ cache["session_id"] }

	Server createServer(Map data) {
		new Server(this, http.jsonPost("guilds", [name: data.name.toString(),
			region: (data.region ?: optimalRegion).toString()]))
	}

	List<Server> getServers() { cache["guilds"]?.list ?: [] }
	Map<String, Server> getServerMap(){ cache["guilds"]?.map ?: [:] }

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

	List<User> getAllUsers() {
		// This takes a long time to .unique() on so i found this faster method
		(members + privateChannels*.user).groupBy { it.id }
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

	void editRoles(Member member, List<Role> roles) {
		member.server.editRoles(member, roles)
	}

	void addRoles(Member member, List<Role> roles) {
		member.server.addRoles(member, roles)
	}

	void kick(Member member) {
		member.kick()
	}

	void kick(Server server, User user){
		server.kick(user)
	}

	void ban(Server server, User user, int days=0) {
		server.ban(user, days)
	}

	void unban(Server server, User user) {
		server.unban(user)
	}

	Role createRole(Server server, Map<String, Object> data) {
		server.createRole(data)
	}

	Role editRole(Server server, Role role, Map<String, Object> data) {
		server.editRole(role, data)
	}

	void deleteRole(Server server, Role role) {
		server.deleteRole(role)
	}

	void changeStatus(Map<String, Object> data) {
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
				game: payload.game,
				status: payload.status,
				guild_id: s.id
			)
		}
	}

	void play(String game){ changeStatus(game: game) }
	void playGame(String game){ changeStatus(game: game) }
	void stream(String desc = user.game.name, String url = null){
		changeStatus(game: [url: url, type: 1] << (desc ? [name: desc] : [:]))
	}

	Invite acceptInvite(id){
		new Invite(this, http.jsonPost("invite/${Invite.parseId(id)}", [:]))
	}

	Invite requestInvite(id){
		new Invite(this, http.jsonGet("invite/${Invite.parseId(id)}"))
	}

	Invite createInvite(Map data = [:], dest){
		new Invite(this, http.jsonPost("channels/${id(dest)}/invites", data))
	}

	List<Connection> requestConnections(){
		http.jsonGet("users/@me/connections").collect { new Connection(this, it) }
	}

	User editProfile(Map data){
		Map map = [avatar: user.avatarHash, email: this?.email,
			password: this?.password, username: user.username]
		Map response = http.jsonPatch("users/@me", map <<
			ConversionUtil.fixImages(data))
		email = response.email
		password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : password
		token = response.token
			.replaceFirst(java.util.regex.Pattern.quote(tokenPrefix), "")
			.trim()
		cache["user"]["email"] = response.email
		cache["user"]["verified"] = response.verified
		new User(this, response)
	}

	Application requestApplication(){
		new Application(this, http.jsonGet("oauth2/applications/@me"))
	}

	List<Application> requestApplications(){
		http.jsonGet("oauth2/applications").collect { new Application(this, it) }
	}

	Map<String, Application> getApplicationMap(){
		new DiscordListCache(requestApplications(), this).map
	}

	Application requestApplication(id){
		new Application(this, http.jsonGet("oauth2/applications/${this.id(id)}"))
	}

	Application editApplication(Application application, Map data){
		application.edit(data)
	}

	void deleteApplication(Application application){
		application.delete()
	}

	Application createApplication(Map data){
		Map map = [icon: null, description: "", redirect_uris: [], name: ""]
		new Application(this, http.jsonPost("oauth2/applications", map <<
			ConversionUtil.fixImages(data)))
	}

	BotAccount createBot(Application application, String oldAccountToken=null){
		application.createBot(oldAccountToken)
	}

	List<Region> getRegions(){
		http.jsonGet("voice/regions").collect { new Region(this, it) }
	}

	Region getOptimalRegion(){
		regions.find { it.optimal }
	}

	List<User> queueUsers(String query, int limit=25){ http.jsonGet("users?q=${URLEncoder.encode(query)}&limit=$limit").collect { new User(this, it) } }
	User requestUser(i){ new User(this, http.jsonGet("users/${id(i)}")) }
	Server requestServer(i){
		new Server(this, http.jsonGet("guilds/${id(i)}"))
	}
	Channel requestChannel(i){
		new Channel(this, http.jsonGet("channels/${id(i)}"))
	}

	List<Channel> getPrivateChannels(){
		cache["private_channels"]?.list ?: []
	}

	Map<String, Channel> getPrivateChannelMap(){
		cache["private_channels"]?.map ?: [:]
	}

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
		textChannels + voiceChannels ?: []
	}

	Map<String, Channel> getChannelMap(){
		textChannelMap + voiceChannelMap ?: [:]
	}

	List<Member> members(id){
		boolean name = !(id ==~ /\d+/)
		String aa = name ? id : resolveId(id)
		members.findAll { name ? it.name == aa : it.id == aa }
	}

	User user(id){ find(members, servers*.memberMap.sum(), id).toUser() }

	Server server(id){ find(servers, serverMap, id) }

	Channel textChannel(id){ find(textChannels, textChannelMap, id) }

	Channel voiceChannel(id){ find(voiceChannels, voiceChannelMap, id) }

	Channel channel(id){ find(channels, channelMap, id) }

	Role role(id){ find(roles, roleMap, id) }

	List getEverything(){
		servers + roles + members + channels
	}

	// Adding built-in listeners. Look above at "removeListenersFor" to understand why I did it like

	void addGuildMemberAddListener(){
		addListener(Events.MEMBER){ Map d ->
			cache["guilds"][d.json.guild_id]["members"].add(d.json)
			cache["guilds"][d.json.guild_id]["member_count"]++
		}
	}

	void addGuildMemberRemoveListener(){
		addListener(Events.MEMBER_REMOVED){ Map d ->
			cache["guilds"][d.json.guild_id]["members"].remove(d.json.user.id)
			cache["guilds"][d.json.guild_id]["member_count"]--
		}
	}

	void addGuildRoleCreateListener(){
		addListener(Events.ROLE){ Map d ->
			cache["guilds"][d.json.guild_id]["roles"].add(d.json.role << [guild_id: d.json.guild_id])
		}
	}

	void addGuildRoleDeleteListener(){
		addListener(Events.ROLE_DELETE){ Map d ->
			cache["guilds"][d.json.guild_id]["roles"].remove(d.json.role_id)
		}
	}

	void addChannelCreateListener(){
		addListener(Events.CHANNEL_CREATE){ Map d ->
			if (d.json.guild_id){
				cache["guilds"][d.json.guild_id]["channels"].add(d.json)
			}else{
				cache["private_channels"].add(d.json)
			}
		}
	}

	void addChannelDeleteListener(){
		addListener(Events.CHANNEL_DELETE){ Map d ->
			if (d.json.guild_id){
				cache["guilds"][d.json.guild_id]["channels"].remove(d.json.id)
			}else{
				cache["private_channels"].remove(d.json.id)
			}
		}
	}

	void addChannelUpdateListener(){
		addListener(Events.CHANNEL_UPDATE){ Map d ->
			cache["guilds"][d.json.guild_id]["channels"][d.json.id] <<
				d.json
		}
	}

	void addMessageCreateListener(){
		addListener(Events.MESSAGE){ Map d ->
			if (messages[d.json.channel_id]){
				messages[d.json.channel_id].add(d.json)
			}else{
				messages[d.json.channel_id] = new DiscordListCache([d.json], this, Message)
				messages[d.json.channel_id].root = d.message.channel
			}
		}
	}

	void addMessageUpdateListener(){
		addListener(Events.MESSAGE_UPDATE){ Map d ->
			if (!(d.message instanceof String)){
				Channel channel = d.message.channel
				if (messages[channel.id]){
					if (messages[channel.id][d.message.id]){
						messages[channel.id][d.message.id] << d.message.object
					}else{
						messages[channel.id].add(d.message)
					}
				}else{
					messages[channel.id] = new DiscordListCache([d.message], this, Message)
					messages[channel.id].root = channel
				}
			}
		}
	}

	void addMessageDeleteListener(){
		addListener("message delete") { Map d ->
			if (!(d.message instanceof String)){
				messages[d.channel.id].remove(d.message.id)
			}
		}
	}

	void addGuildCreateListener(){
		addListener(Events.SERVER){ Map d ->
			Map server = d.server.object
			if (cache["guilds"].any { k, v -> k == server["id"] }){
				cache["guilds"][server["id"]] << server
			}else{
				cache["guilds"].add(server)
			}
		}
	}

	void addGuildDeleteListener(){
		addListener(Events.SERVER_DELETED){ Map d ->
			cache["guilds"].remove(d.server.id)
		}
	}

	void addGuildMemberUpdateListener(){
		addListener(Events.MEMBER_UPDATED){ Map d ->
			cache["guilds"][d.server.id]["members"][d.member?.id]?.leftShift(d.member?.object) ?: null
		}
	}

	void addGuildRoleUpdateListener(){
		addListener(Events.ROLE_UPDATE){ Map d ->
			cache["guilds"][d.server.id]["roles"][d.role.id] << d.role.object
		}
	}

	void addGuildUpdateListener(){
		addListener(Events.SERVER_UPDATED){ Map d ->
			cache["guilds"][d.server.id] = d.server.object
		}
	}

	void addPresenceUpdateListener(){
		addListener("presence change"){ Map d ->
			Server server = d.server
			if (!server) return
			if (d.newUser){
				if (d.member && cache.guilds[server.id].members[d.member?.id]){
					cache.guilds[server.id].members[d.member.id].user <<
						d.newUser.object
				}else{
					cache.guilds[server.id].members.add(d.json.with {
						Map di = it.clone()
						di.remove("status")
						di.remove("game")
						if (allowMemberRequesting) di << server.memberInfo(d.user).object
						else di.joined_at = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
						di
					})
				}
			}
			if (d.json.status == "offline")
				cache.guilds[server.id].presences.remove(d.json.user.id)
			else {
				cache.guilds[server.id].presences?.add(d.json.with {
					Map di = it.clone()
					di.remove("roles")
					di.remove("nick")
					di
				} + [id: d.json.user.id])
			}
		}
	}

	void addVoiceStateUpdateListener(){
		addListener("voice state change"){ Map d ->
			if (d.voiceState.channel){
				def ass = cache["guilds"][d.server]["voice_states"][d.voiceState]
				if (ass) ass << d.voiceState.object
				else cache["guilds"][d.server]["voice_states"][d.voiceState] = d.voiceState.object
				if (d.voiceState == this){
					VoiceClient vc = voiceClients[d.server]
					if (vc) vc.handleVoiceStateUpdate(d)
				}
			}else{
				cache["guilds"][d.server]["voice_states"].remove(d.voiceState.id)
			}
		}
	}

	void addVoiceServerUpdateListener(){
		addListener("voice server change"){ Map d ->
			VoiceClient vc = voiceClients[d.server]
			if (vc) vc.handleVoiceServerUpdate(d)
		}
	}

	void addGuildEmojisUpdateListener(){
		addListener("guild emojis change"){ Map d ->
			cache["guilds"][d.server.id]["emojis"] = new DiscordListCache(this, d.emojis.mapList, Emoji)
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
}
