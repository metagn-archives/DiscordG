package hlaaftana.discordg

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import hlaaftana.discordg.conn.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.objects.*

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
	String getToken(){
		tokenPrefix ? "$tokenPrefix ${this.@token}" : this.@token
	}
	void setToken(String newToken){
		this.@token = newToken
	}
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
	String logName = "DiscordG"
	Log log
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]
	ParentListenerSystem listenerSystem = new ParentListenerSystem(this)
	int gatewayVersion = 5 // changing this is not recommended
	boolean cacheTokens = true
	String tokenCachePath = "token.json"
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	int largeThreshold = 250 // if your bot is on tons of big servers, setting this to lower numbers might help your memory
	boolean requestMembersOnReady = true
	boolean copyReady = true // adds fullData to ready to archive the initial ready
	boolean retryOn502 = true // retries your request if you get a 5xx status code, lke the red messages you get in the regular client
	boolean allowMemberRequesting = false // requests new member info on PRESENCE_UPDATE if it sees a new member. Turn this on if you don't have issues with dog
	boolean enableListenerCrashes = true
	def enableListenerCrashes(){ enableListenerCrashes = true }
	def disableListenerCrashes(){ enableListenerCrashes = false }
	boolean enableEventRegisteringCrashes = false
	def enableEventRegisteringCrashes(){ enableEventRegisteringCrashes = true }
	def disableEventRegisteringCrashes(){ enableEventRegisteringCrashes = false }
	long serverTimeout = 30_000
	List includedEvents = []
	List excludedEvents = [Events.TYPING]
	Tuple2 shardTuple // tuple of shard id and shard count.
	Map pools = [
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

	Requester requester
	WSClient wsClient
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
		requester = new Requester(this)
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

		if (requestMembersOnReady) requestMembersOnReady()
	}

	WSClient getWebSocketClient(){ wsClient }

	Client startAnew(){
		Client newApi = new Client()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
		def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
		commonProps.each { newApi[it] = !(it in ["requester", "wsClient", "client", "cache", "voiceData", "loaded"]) ? this[it] : null }
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
						[(email): requestToken(email, password)])
					token = JSONUtil.parse(tokenCache)[(email)]["token"]
				}
			}
			log.info "Got token."
			login(token, false, false)
		}
		if (threaded) Thread.start(a)
		else  a()
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
			try{
				originalToken = original["bots"][customBotName]["token"]
			}catch (ex){
				original["bots"] = [:]
			}
			if (originalToken == null){
				String newToken = requestToken()
				original["bots"][customBotName] = [token: newToken]
				JSONUtil.dump(tokenFile, original)
				token = newToken
			}else{
				try{
					token = originalToken
				}catch (ex){
					String newToken = requestToken()
					original["bots"][customBotName] = [token: newToken]
					JSONUtil.dump(tokenFile, original)
					token = newToken
				}
			}
			connectGateway(true, false)
		}
		if (threaded) Thread.start(a)
		else a()
	}

	void connectGateway(boolean requestGateway = true, boolean threaded = true){
		Closure a = {
			if (requestGateway){
				log.info "Requesting gateway..."
				gateway = requester.jsonGet("gateway")["url"]
				if (!gateway.endsWith("/")){ gateway += "/" }
				gateway += "?encoding=json&v=$gatewayVersion"
			}
			WebSocketClient client = new WebSocketClient(new SslContextFactory())
			if (!wsClient) wsClient = new WSClient(this)
			log.info "Starting websocket connection..."
			gatewayClosed = false
			client.start()
			client.connect(wsClient, new URI(gateway), new ClientUpgradeRequest())
		}
		askPool("connect"){
			if (threaded) Thread.start(a)
			else a()
		}
	}

	void logout(boolean exit = false){
		if (!bot) requester.post("auth/logout", ["token": token])
		closeGateway(false)
		wsClient = null
		token = null
		requester = null
		if (exit) System.exit(0)
	}

	void closeGateway(boolean threaded = true){
		Closure a = {
			gatewayClosed = true
			if (wsClient){
				wsClient.keepAliveThread?.interrupt()
				wsClient.session?.close(1000, "Close")
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

	def addListener(event, boolean temporary = false, Closure closure) {
		listenerSystem.addListener(event, temporary, closure)
	}

	def removeListener(event, Closure closure) {
		listenerSystem.removeListener(event, closure)
	}

	def removeListenersFor(event){
		listenerSystem.removeListenersFor(event)
	}

	def removeAllListeners(){
		listenerSystem.removeAllListeners()
	}

	def dispatchEvent(type, data){
		listenerSystem.dispatchEvent(type, data)
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

	def addField(String key, value){ fields[key] = value }

	def setField(String key, value){ fields[key] = value }

	def field(String key){ fields[key] }

	def getField(String key){ fields[key] }

	boolean isLoaded(){
		this.@token &&
			wsClient &&
			wsClient.loaded &&
			!cache.empty &&
			(!client.bot || !servers.any { it.unavailable })
	}


	String requestToken(String email, String password){
		askPool("login"){
			requester.jsonPost("auth/login",
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
			wsClient.send op: 12, d: it
			Thread.sleep 1000
		}
	}

	def chunkMembersFor(servers, String query = "", int limit = 0){
		List guilds = servers instanceof Collection ?
			servers.collect(DiscordObject.&id).collate(25) :
			[id(servers)]
		guilds.each {
			wsClient.send op: 8, d: [
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

	String getSessionId(){ cache["session_id"] }

	TextChannel createTextChannel(Server server, String name) {
		server.createTextChannel(name)
	}

	VoiceChannel createVoiceChannel(Server server, String name) {
		server.createVoiceChannel(name)
	}

	Channel editChannel(Channel channel, Map<String, Object> data) {
		channel.edit(data)
	}

	void deleteChannel(Channel channel) {
		channel.delete()
	}

	Server createServer(Map data) {
		new Server(this, requester.jsonPost("guilds", ["name": data.name.toString(), "region": (data.region ?: optimalRegion).toString()]))
	}

	Server editServer(Server server, Map data) {
		server.edit(data)
	}

	void leaveServer(Server server) {
		server.leave()
	}

	Message sendMessage(TextChannel channel, String content, boolean tts=false) {
		channel.sendMessage(content, tts)
	}

	Message editMessage(Message message, String newContent) {
		message.edit(newContent)
	}

	void deleteMessage(Message message) {
		message.delete()
	}

	// removed ack method because of discord dev request

	List<Server> getServers() {
		cache["guilds"]?.list ?: []
	}

	Map<String, Server> getServerMap(){
		cache["guilds"]?.map ?: [:]
	}

	List<Server> requestServers(){
		List array = requester.jsonGet("users/@me/guilds")
		List<Server> servers = []
		for (s in array){
			def serverInReady = cache["guilds"][s.id]
			servers.add(new Server(this, s << ["channels": serverInReady["channels"], "members": serverInReady["members"], "presences": serverInReady["presences"], "voice_states": serverInReady["voice_states"], "large": serverInReady["large"]]))
		}
		servers
	}

	List<PrivateChannel> requestPrivateChannels(){
		requester.jsonGet("users/@me/channels").collect { new PrivateChannel(this, it + ["cached_messages": []]) }
	}

	List<TextChannel> getTextChannelsForServer(Server server) {
		server.textChannels
	}

	List<VoiceChannel> getVoiceChannelsForServer(Server server) {
		server.voiceChannels
	}

	List<User> getAllUsers() {
		// This takes a long time to .unique() on so i found this faster method
		(members + privateChannels*.user).groupBy { it.id }
			.values()*.first()
	}

	List<Member> getAllMembers() {
		servers*.members.sum()
	}

	List<Role> getAllRoles(){
		servers*.roles.sum()
	}

	List<User> getUsers(){ allUsers }
	List<Member> getMembers(){ allMembers }
	List<Role> getRoles(){ allRoles }

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
		askPool("changePresence"){
			wsClient.send([
				op: 3,
				d: [
					idle_since: (data["idle"] != null) ? System.currentTimeMillis() : null,
					game: data["game"] != null ?
						data["game"] instanceof String ? [name: data["game"]] :
						data["game"] instanceof DiscordObject ? data["game"].object :
						data["game"] instanceof Map ? data["game"] :
						[name: data["game"].toString()] :
						game.object
				]
			])
		}
		for (s in servers){
			dispatchEvent("PRESENCE_UPDATE", [
				"fullData": [
					"game": (data["game"] != null) ? ["name": data["game"]] : null,
					"status": (data["idle"] != null) ? "online" : "idle",
					"guild_id": s.id, "user": user.object
					],
				"server": s,
				"guild": s,
				"member": s.members.find { it.id == user.id },
				"game": (data["game"] != null) ? data["game"] : null,
				"status": (data["idle"] != null) ? "online" : "idle"
				])
		}
	}

	void play(String game){ changeStatus(game: game) }
	void playGame(String game){ changeStatus(game: game) }
	void stream(String desc = user.game.name, String url = null){
		changeStatus(game: [url: url, type: 1] << (desc ? [name: desc] : [:]))
	}

	Invite acceptInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			new Invite(this, requester.jsonPost("invite/${Invite.parseId(link)}", [:]))
		else
			new Invite(this, requester.jsonPost("invite/$link", [:]))
	}

	Invite getInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			new Invite(this, requester.jsonGet("invite/${Invite.parseId(link)}"))
		else
			new Invite(this, requester.jsonGet("invite/${link}"))
	}

	Invite createInvite(dest, Map data=[:]){
		new Invite(this, requester.jsonPost("channels/${id(dest)}/invites", data))
	}

	List<Invite> getInvitesFor(Server server){
		server.invites
	}

	List<Invite> getInvitesFor(Channel channel){
		channel.invites
	}

	List<Connection> getConnections(){
		requester.jsonGet("users/@me/connections").collect { new Connection(this, it) }
	}

	void moveToChannel(Member member, VoiceChannel channel){
		member.moveTo(channel)
	}

	User editProfile(Map data){
		Map map = ["avatar": user.avatarHash, "email": this?.email, "password": this?.password, "username": user.username]
		if (data["avatar"] != null){
			if (data["avatar"] instanceof String && !(data["avatar"].startsWith("data"))){
				data["avatar"] = ConversionUtil.encodeImage(data["avatar"] as File)
			}else if (ConversionUtil.isImagable(data["avatar"])){
				data["avatar"] = ConversionUtil.encodeImage(data["avatar"])
			}
		}
		Map response = requester.jsonPatch("users/@me", map << data)
		email = response.email
		password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : password
		token = response.token
			.replaceFirst(java.util.regex.Pattern.quote(tokenPrefix), "")
			.trim()
		cache["user"]["email"] = response.email
		cache["user"]["verified"] = response.verified
		new User(this, response)
	}

	Application getApplication(){
		new Application(this, requester.jsonGet("oauth2/applications/@me"))
	}

	List<Application> getApplications(){
		requester.jsonGet("oauth2/applications").collect { new Application(this, it) }
	}
	Map<String, Application> getApplicationMap(){
		new DiscordListCache(applications, this).map
	}

	Application getApplicationFromId(String id){
		new Application(this, requester.jsonGet("oauth2/applications/$id"))
	}

	Application getApplication(String id){
		new Application(this, requester.jsonGet("oauth2/applications/$id"))
	}

	Application editApplication(Application application, Map data){
		application.edit(data)
	}

	void deleteApplication(Application application){
		application.delete()
	}

	Application createApplication(Map data){
		Map map = ["icon": null, "description": "", "redirect_uris": [], "name": ""]
		if (data["icon"] != null){
			if (data["icon"] instanceof String && !(data["icon"].startsWith("data"))){
				data["icon"] = ConversionUtil.encodeImage(data["icon"] as File)
			}else if (ConversionUtil.isImagable(data["icon"])){
				data["icon"] = ConversionUtil.encodeImage(data["icon"])
			}
		}
		new Application(this, requester.jsonPost("oauth2/applications", map << data))
	}

	BotAccount createBot(Application application, String oldAccountToken=null){
		application.createBot(oldAccountToken)
	}

	List<Region> getRegions(){
		requester.jsonGet("voice/regions").collect { new Region(this, it) }
	}

	Region getOptimalRegion(){
		regions.find { it.optimal }
	}

	List<User> queueUsers(String query, int limit=25){ requester.jsonGet("users?q=${URLEncoder.encode(query)}&limit=$limit").collect { new User(this, it) } }
	User requestUser(i){ new User(this, requester.jsonGet("users/${id(i)}")) }
	Server requestServer(i){
		new Server(this, requester.jsonGet("guilds/${id(i)}"))
	}
	Channel requestChannel(i){
		Map response = requester.jsonGet("channels/${id(i)}")
		(response.type == "text") ? new TextChannel(this, response) : new VoiceChannel(this, response)
	}

	List<PrivateChannel> getPrivateChannels(){
		cache["private_channels"]?.list ?: []
	}

	Map<String, PrivateChannel> getPrivateChannelMap(){
		cache["private_channels"]?.map ?: [:]
	}

	List<TextChannel> getTextChannels(){
		privateChannels + servers*.textChannels.flatten() ?: []
	}

	Map<String, TextChannel> getTextChannelMap(){
		servers*.textChannelMap.sum() + privateChannelMap ?: [:]
	}

	List<VoiceChannel> getVoiceChannels(){
		servers*.voiceChannels.sum() ?: []
	}

	Map<String, VoiceChannel> getVoiceChannelMap(){
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

	TextChannel textChannel(id){ find(textChannels, textChannelMap, id) }

	VoiceChannel voiceChannel(id){ find(voiceChannels, voiceChannelMap, id) }

	Channel channel(id){ find(channels, channelMap, id) }

	Role role(id){ find(roles, roleMap, id) }

	List getEverything(){
		servers + roles + members + channels
	}

	// Adding built-in listeners. Look above at "removeListenersFor" to understand why I did it like

	void addGuildMemberAddListener(){
		addListener(Events.MEMBER){ Map d ->
			cache["guilds"][d.server.id]["members"].add(d.member.object)
			cache["guilds"][d.server.id]["member_count"]++
		}
	}

	void addGuildMemberRemoveListener(){
		addListener(Events.MEMBER_REMOVED){ Map d ->
			cache["guilds"][d.server.id]["members"].remove(d.member.id)
			cache["guilds"][d.server.id]["member_count"]--
		}
	}

	void addGuildRoleCreateListener(){
		addListener(Events.ROLE){ Map d ->
			cache["guilds"][d.server.id]["roles"].add(d.role.object + ["guild_id": d.server.id])
		}
	}

	void addGuildRoleDeleteListener(){
		addListener(Events.ROLE_DELETE){ Map d ->
			cache["guilds"][d.server.id]["roles"].remove(d.role.id)
		}
	}

	void addChannelCreateListener(){
		addListener(Events.CHANNEL_CREATE){ Map d ->
			if (d.server){
				cache["guilds"][d.server.id]["channels"].add(d.channel.object)
			}else{
				cache["private_channels"].add(d.channel.object)
			}
		}
	}

	void addChannelDeleteListener(){
		addListener(Events.CHANNEL_DELETE){ Map d ->
			if (d.server){
				cache["private_channels"].remove(d.channel.id)
			}else{
				cache["guilds"][d.server.id]["channels"].remove(d.channel.id)
			}
		}
	}

	void addChannelUpdateListener(){
		addListener(Events.CHANNEL_UPDATE){ Map d ->
			cache["guilds"][d.server.id]["channels"][d.channel.id] << d.channel.object
		}
	}

	void addMessageCreateListener(){
		addListener(Events.MESSAGE){ Map d ->
			TextChannel channel = d.message.channel
			if (messages[channel.id]){
				messages[channel.id].add(d.message)
			}else{
				messages[channel.id] = new DiscordListCache([d.message], this, Message)
				messages[channel.id].root = channel
			}
		}
	}

	void addMessageUpdateListener(){
		addListener(Events.MESSAGE_UPDATE){ Map d ->
			if (!(d.message instanceof String)){
				TextChannel channel = d.message.channel
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
					cache.guilds[server.id].members.add(d.fullData.with {
						Map di = it.clone()
						di.remove("status")
						di.remove("game")
						if (allowMemberRequesting) di << server.memberInfo(d.user).object
						else di.joined_at = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
						di
					})
				}
			}
			cache.guilds[server.id].presences.add(d.fullData.with {
				Map di = it.clone()
				di.remove("roles")
				di
			})
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
			cache["user"] << d.fullData
		}
	}

	void addGuildMemberChunkListener(){
		addListener("guild members chunk"){ Map d ->
			d.fullData.members.each {
				cache["guilds"][d.fullData.guild_id]["members"].add(it)
			}
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
				wsClient.reconnect()
		}
	}
}
