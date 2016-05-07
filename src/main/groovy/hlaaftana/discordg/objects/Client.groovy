package hlaaftana.discordg.objects

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.conn.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.oauth.Application
import hlaaftana.discordg.oauth.BotAccount
import hlaaftana.discordg.objects.VoiceClient

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * The Discord client.
 * @author Hlaaftana
 */
class Client extends User {
	String customUserAgent = ""
	String getFullUserAgent(){ "$DiscordG.USER_AGENT $customUserAgent" }

	Requester requester
	String token
	String email
	String getEmail(){
		return this.cache["user"]?.getAt("email") ?: this.@email
	}
	String password
	WSClient wsClient
	VoiceWSClient voiceWsClient
	VoiceClient voiceClient
	Map<String, List<Closure>> listeners = new HashMap<String, List<Closure>>()
	Cache cache = Cache.empty(this)
	Cache voiceData = Cache.empty(this)
	String gateway
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]
	int v = 4 // changing this is not recommended
	boolean cacheTokens = true
	String tokenCachePath = "token.json"
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	int largeThreshold = 250 // if your bot is on tons of big servers, setting this to lower numbers might help your startup time
	boolean copyReady = true // adds fullData to ready to archive the initial ready
	boolean retryOn502 = true // retries your request if you get a 5xx status code, lke the red messages you get in the regular client
	boolean allowMemberRequesting = false // requests new member info on PRESENCE_UPDATE if it sees a new member. Turn this on if you don't have issues with dog
	boolean countEvents = false // counts events
	Map eventCounts = [:]
	List includedEvents
	List excludedEvents = [Events.TYPING]
	Tuple2 shardTuple // tuple of shard id and shard count.

	/**
	 * Builds a new client. This is safe to do.
	 */
	Client(Map config=[:]){
		super(null, [:])
		this.client = this

		config.each { k, v ->
			this[k] = v
		}
		requester = new Requester(this)

		// oh boy am i gonna get hate for this
		// check reason below where i define these
		this.addChannelCreateListener()
		this.addChannelDeleteListener()
		this.addChannelUpdateListener()

		// todo: add better caching
		//this.addMessageCreateListener()
		//this.addMessageUpdateListener()
		//this.addMessageDeleteListener()


		this.addGuildCreateListener()
		this.addGuildDeleteListener()
		this.addGuildUpdateListener()
		this.addGuildMemberAddListener()
		this.addGuildMemberRemoveListener()
		this.addGuildMemberUpdateListener()
		this.addGuildRoleCreateListener()
		this.addGuildRoleDeleteListener()
		this.addGuildRoleUpdateListener()
		this.addPresenceUpdateListener()
		this.addVoiceStateUpdateListener()
		this.addGuildEmojisUpdateListener()
		this.addUserUpdateListener()
		this.addReadyListener()
	}

	WSClient getWebSocketClient(){ return wsClient }

	Client startAnew(){
		Client newApi = new Client()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
		def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
		commonProps.each { newApi[it] = !(it in ["requester", "wsClient", "client", "cache", "voiceData", "loaded"]) ? this[it] : null }
		return newApi
	}

	/**
	 * Logs onto Discord.
	 * @param email - the email to log in with.
	 * @param password - the password to use.
	 */
	void login(String email, String password, boolean threaded=true){
		Closure a = {
			Log.info "Getting token..."
			this.email = email
			this.password = password
			File tokenCache = new File(tokenCachePath)
			if (tokenCache.exists()){
				try{
					if (cacheTokens){
						token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
					}else{
						token = JSONUtil.parse(this.requester.post("auth/login", ["email": email, "password": password]))["token"]
					}
				}catch (ex){
					if (cacheTokens){
						tokenCache.write(JsonOutput.prettyPrint(JSONUtil.json(new JsonSlurper().parse(tokenCache) + [(email): JSONUtil.parse(this.requester.post("auth/login", ["email": email, "password": password]))])))

						token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
					}else{
						token = JSONUtil.parse(this.requester.post("auth/login", ["email": email, "password": password]))["token"]
					}
				}
			}else{
				if (cacheTokens){
					tokenCache.createNewFile()
					tokenCache.write(JsonOutput.prettyPrint(JSONUtil.json(new JsonSlurper().parse(tokenCache) + [(email): JSONUtil.parse(this.requester.post("auth/login", ["email": email, "password": password]))])))

					token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
				}else{
					token = JSONUtil.parse(this.requester.post("auth/login", ["email": email, "password": password]))["token"]
				}
			}
			Log.info "Got token."
			this.login(this.token, false)
		}
		if (threaded){ Thread.start(a) }
		else { a() }
	}

	void login(String token, boolean threaded=true){
		Closure a = {
			this.token = token
			connectGateway(true, false)
		}
		if (threaded){ Thread.start(a) }
		else { a() }
	}

	void connectGateway(boolean requestGateway = true, boolean threaded = true){
		Closure a = {
			if (requestGateway){
				Log.info "Requesting gateway..."
				this.gateway = JSONUtil.parse(this.requester.get("gateway"))["url"]
				if (!this.gateway.endsWith("/")){ this.gateway += "/" }
				this.gateway += "?encoding=json&v=4"
			}
			SslContextFactory sslFactory = new SslContextFactory()
			WebSocketClient client = new WebSocketClient(sslFactory)
			this.wsClient = new WSClient(this)
			Log.info "Starting websocket connection..."
			client.start()
			ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
			client.connect(this.wsClient, new URI(this.gateway), upgreq)
		}
		if (threaded){ Thread.start(a) }
		else { a() }
	}

	void logout(boolean exit = false){
		try{
			this.requester.post("auth/logout", ["token": this.token])
			this.wsClient.keepAliveThread.interrupt()
			this.wsClient.session.close(0, "Logout")
			this.wsClient = null
			this.token = null
			this.requester = null
			if (exit) System.exit(0)
		}catch (ex){

		}
	}

	/**
	 * Registers a closure to listen to an event.
	 * @param event - the event string. This is manipulated by API#parseEventType.
	 * @param closure - the closure to listen to the event. This will be provided with 1 parameter which will be an Event object.
	 */
	void addListener(event, boolean temporary=false, Closure closure) {
		for (e in listeners.entrySet()){
			if (e.key == Events.get(event).type){
				if (!temporary){
					e.value.add(closure)
				}else{
					e.value.add { Map d, Closure c ->
						closure(d)
						e.value.remove(c)
					}
				}
				return
			}
		}
		listeners.put(Events.get(event).type, [closure])
	}

	/**
	 * Removes a listener from an event.
	 * @param event - the event name. This is manipulated by API#parseEventType.
	 * @param closure - the closure to remove.
	 */
	void removeListener(event, Closure closure) {
		for (e in listeners.entrySet()){
			if (e.key == Events.get(event).type){
				e.value.remove(closure)
			}
		}
	}

	/**
	 * Remove all listeners for an event (containing built-in listeners. you can add them back using the "addBlankEventExampleListener()" methods in this class. yay low-levelism)
	 * @param event - the event name. This is manipulated by API#parseEventType.
	 */
	void removeListenersFor(event){
		for (e in listeners.entrySet()){
			if (e.key == Events.get(event).type){
				listeners[e.key] = []
				return
			}
		}
	}

	void removeAllListeners(){
		this.listeners = [:]
	}

	String requestToken(String email, String password){
		return JSONUtil.parse(requester.post("auth/login", ["email": email, "password": password]))["token"]
	}

	/**
	 * Assigns a
	 * @param key - to a
	 * @param value - and adds it to API#fields.
	 */
	void addField(String key, value){
		fields[key] = value
	}

	/**
	 * Gets a value from a key from API#fields.
	 * @param key - the key.
	 * @return the value.
	 */
	def field(String key){
		return this.getField(key)
	}

	/**
	 * Gets a value from a key from API#fields.
	 * @param key - the key.
	 * @return the value.
	 */
	def getField(String key){
		return fields[key]
	}

	/**
	 * Triggers all listeners for an event.
	 * @param event - the event object to provide.
	 */
	void dispatchEvent(type, Map data){
		this.listeners.each { Map.Entry<String, List<Closure>> entry ->
			if (Events.get(type).type == entry.key){
				for (c in entry.value){
					try{
						if (c.maximumNumberOfParameters > 1){
							c.call(data, c)
						}else{
							c.call(data)
						}
					}catch (ex){
						if (Log.enableListenerCrashes) ex.printStackTrace()
						Log.info "Ignoring exception from event " + entry.key
					}
				}
			}
		}
	}

	/**
	 * @return whether or not the client is loaded.
	 */
	boolean isLoaded(){
		return this.requester != null && this.token != null && this.wsClient != null && this.wsClient.loaded && !this.cache.empty
	}

	/**
	 * @return the user which the client is logged in to.
	 */
	User getUser(){ return new User(this, this.cache["user"]) }

	boolean isVerified(){ return this.cache["user"]["verified"] }

	/**
	 * @return the session ID for the session.
	 */
	String getSessionId(){ return this.cache["session_id"] }

	/**
	 * See Server#createTextChannel.
	 */
	TextChannel createTextChannel(Server server, String name) {
		return server.createTextChannel(name)
	}

	/**
	 * See Server#createVoiceChannel.
	 */
	VoiceChannel createVoiceChannel(Server server, String name) {
		return server.createVoiceChannel(name)
	}

	/**
	 * See Channel#edit.
	 */
	Channel editChannel(Channel channel, Map<String, Object> data) {
		channel.edit(data)
	}

	/**
	 * See Channel#delete.
	 */
	void deleteChannel(Channel channel) {
		channel.delete()
	}

	/**
	 * Creates a new server.
	 * @param name - the name of the server.
	 * @return the created server.
	 */
	Server createServer(Map data) {
		return new Server(this, JSONUtil.parse(this.requester.post("guilds", ["name": data.name.toString(), "region": (data.region ?: "london").toString()])))
	}

	/**
	 * See Server#edit.
	 */
	Server editServer(Server server, Map data) {
		return server.edit(data)
	}

	/**
	 * See Server#leave.
	 */
	void leaveServer(Server server) {
		server.leave()
	}

	/**
	 * See TextChannel#sendMessage.
	 */
	Message sendMessage(TextChannel channel, String content, boolean tts=false) {
		return channel.sendMessage(content, tts)
	}

	/**
	 * See Message#edit.
	 */
	Message editMessage(Message message, String newContent) {
		return message.edit(newContent)
	}

	/**
	 * See Message#delete.
	 */
	void deleteMessage(Message message) {
		message.delete()
	}

	// removed ack method because of discord dev request

	/**
	 * @return a List of Servers the client is connected to.
	 */
	List<Server> getServers() {
		return this.cache["guilds"]?.list ?: []
	}

	Map<String, Server> getServerMap(){
		return this.cache["guilds"]?.map ?: [:]
	}

	List<Server> requestServers(){
		List array = JSONUtil.parse(this.requester.get("users/@me/guilds"))
		List<Server> servers = []
		for (s in array){
			def serverInReady = this.cache["guilds"][s.id]
			servers.add(new Server(this, s << ["channels": serverInReady["channels"], "members": serverInReady["members"], "presences": serverInReady["presences"], "voice_states": serverInReady["voice_states"], "large": serverInReady["large"]]))
		}
		return servers
	}

	List<PrivateChannel> requestPrivateChannels(){
		return JSONUtil.parse(this.requester.get("users/@me/channels")).collect { new PrivateChannel(this, it + ["cached_messages": []]) }
	}

	/**
	 * See Server#getTextChannels.
	 */
	List<TextChannel> getTextChannelsForServer(Server server) {
		return server.textChannels
	}

	/**
	 * See Server#getVoiceChannels.
	 */
	List<VoiceChannel> getVoiceChannelsForServer(Server server) {
		return server.voiceChannels
	}

	/**
	 * @return a List of Users the client can see.
	 */
	List<User> getAllUsers() {
		List<User> ass = []
		for (m in this.allMembers){
			if (!(m.id in ass*.id)){ ass += m.user }
		}
		return ass
	}

	/**
	 * @return a List of Members the client can see. Same users can be different member objects.
	 */
	List<Member> getAllMembers() {
		return this.servers*.members.sum()
	}

	List<Role> getAllRoles(){
		return this.servers*.roles.sum()
	}

	List<User> getUsers(){ return this.allUsers }
	List<Member> getMembers(){ return this.allMembers }
	List<Role> getRoles(){ return this.allRoles }

	Map<String, User> getUserMap(){
		return this.servers*.memberMap.sum()
	}

	Map<String, Map<String, Member>> getMemberMap(){
		Map doo
		this.servers.each { doo[it.id] = it.memberMap }
		return doo
	}

	Map<String, Role> getRoleMap(){
		return this.servers*.roleMap.sum()
	}

	/**
	 * See Member#editRoles.
	 */
	void editRoles(Member member, List<Role> roles) {
		member.server.editRoles(member, roles)
	}

	/**
	 * See Member#delete.
	 */
	void addRoles(Member member, List<Role> roles) {
		member.server.addRoles(member, roles)
	}

	/**
	 * See Member#kick.
	 */
	void kick(Member member) {
		member.kick()
	}

	void kick(Server server, User user){
		server.kick(user)
	}

	/**
	 * See Server#ban.
	 */
	void ban(Server server, User user, int days=0) {
		server.ban(user, days)
	}

	/**
	 * See Server#unban.
	 */
	void unban(Server server, User user) {
		server.unban(user)
	}

	/**
	 * See Server#createRole.
	 */
	Role createRole(Server server, Map<String, Object> data) {
		return server.createRole(data)
	}

	/**
	 * See Server#editRole.
	 */
	Role editRole(Server server, Role role, Map<String, Object> data) {
		return server.editRole(role, data)
	}

	/**
	 * See Server#deleteRole.
	 */
	void deleteRole(Server server, Role role) {
		server.deleteRole(role)
	}

	/**
	 * Updates the client's presence.
	 * @param data - The data to send. Can be: <br>
	 * [game: "FL Studio 12", idle: "anything. as long as it's defined in the map, the user will become idle"]
	 */
	void changeStatus(Map<String, Object> data) {
		this.wsClient.send([
			op: 3,
			d: [
				idle_since: (data["idle"] != null) ? System.currentTimeMillis() : null,
				game: data["game"] instanceof String ? [name: data["game"]] :
					data["game"] instanceof DiscordObject ? data["game"].object :
					data["game"] instanceof Map ? data["game"] :
					[name: data["game"].toString()]
			]
		])
		for (s in this.servers){
			this.dispatchEvent("PRESENCE_UPDATE", [
				"fullData": [
					"game": (data["game"] != null) ? ["name": data["game"]] : null,
					"status": (data["idle"] != null) ? "online" : "idle",
					"guild_id": s.id, "user": this.user.object
					],
				"server": s,
				"guild": s,
				"member": s.members.find { it.id == this.user.id },
				"game": (data["game"] != null) ? data["game"] : null,
				"status": (data["idle"] != null) ? "online" : "idle"
				])
		}
	}

	void play(String game){ this.changeStatus(game: game) }
	void playGame(String game){ this.changeStatus(game: game) }
	void stream(String desc = this.user.game, String url = null){
		this.changeStatus(game: [url: url, type: 1] << (desc ? [name: desc] : [:]))
	}

	/**
	 * Accepts an invite and joins a new server.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return a new Invite object of the accepted invite.
	 */
	Invite acceptInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(this, JSONUtil.parse(this.requester.post("invite/${Invite.parseId(link)}", [:])))
		else
			return new Invite(this, JSONUtil.parse(this.requester.post("invite/${link}", [:])))
	}

	/**
	 * Gets an Invite object from a link/ID.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return the gotten invite.
	 */
	Invite getInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(this, JSONUtil.parse(this.requester.get("invite/${Invite.parseId(link)}")))
		else
			return new Invite(this, JSONUtil.parse(this.requester.get("invite/${link}")))
	}

	/**
	 * Creates an invite.
	 * @param dest - The destination for the invite. Can be a Server, a Channel, or the ID of a channel.
	 * @return the created invite.
	 */
	Invite createInvite(def dest, Map data=[:]){
		String id = (dest instanceof Channel) ? dest.id : (dest instanceof Server) ? dest.defaultChannel.id : dest
		return new Invite(this, JSONUtil.parse(this.requester.post("channels/${id}/invites", data)))
	}

	List<Invite> getInvitesFor(Server server){
		return server.invites
	}

	List<Invite> getInvitesFor(Channel channel){
		return channel.invites
	}

	List<Connection> getConnections(){
		return JSONUtil.parse(this.requester.get("users/@me/connections")).collect { new Connection(this, it) }
	}

	void moveToChannel(Member member, VoiceChannel channel){
		this.requester.patch("guilds/${member.server.id}/members/{member.id}", ["channel_id": channel.id])
	}

	/**
	 * Edits the user's profile.
	 * @param data - the data to edit with. Be really careful with this. Can be: <br>
	 * [email: "newemail@dock.org", new_password: "oopsaccidentallygavesomeonemypass", username: "New name new me"] <br>
	 * Note that you can also have an avatar property in the map above, but I'm not encouraging it until I provide a utility function for that.
	 * @return a User object for the edited profile.
	 */
	User editProfile(Map data){
		Map map = ["avatar": this.user.avatarHash, "email": this?.email, "password": this?.password, "username": this.user.username]
		if (data["avatar"] != null){
			if (data["avatar"] instanceof String && !(data["avatar"].startsWith("data"))){
				data["avatar"] = ConversionUtil.encodeImage(data["avatar"] as File)
			}else if (data["avatar"].class in ConversionUtil.imagable){
				data["avatar"] = ConversionUtil.encodeImage(data["avatar"])
			}
		}
		Map response = JSONUtil.parse this.requester.patch("users/@me", map << data)
		this.email = response.email
		this.password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : this.password
		this.token = response.token
		this.cache["user"]["email"] = response.email
		this.cache["user"]["verified"] = response.verified
		return new User(this, response)
	}

	List<Application> getApplications(){
		return JSONUtil.parse(this.requester.get("oauth2/applications")).collect { new Application(this, it) }
	}
	Map<String, Application> getApplicationMap(){
		return new DiscordListCache(this.applications, this).map
	}

	Application getApplicationFromId(String id){
		return new Application(this, JSONUtil.parse(this.requester.get("oauth2/applications/${id}")))
	}

	Application getApplication(String id){
		return new Application(this, JSONUtil.parse(this.requester.get("oauth2/applications/${id}")))
	}

	Application editApplication(Application application, Map data){
		return application.edit(data)
	}

	void deleteApplication(Application application){
		application.delete()
	}

	Application createApplication(Map data){
		Map map = ["icon": null, "description": "", "redirect_uris": [], "name": ""]
		if (data["icon"] != null){
			if (data["icon"] instanceof String && !(data["icon"].startsWith("data"))){
				data["icon"] = ConversionUtil.encodeToBase64(data["icon"] as File)
			}else if (data["icon"] instanceof File){
				data["icon"] = ConversionUtil.encodeToBase64(data["icon"])
			}
		}
		return new Application(this, JSONUtil.parse(this.requester.post("oauth2/applications", map << data)))
	}

	BotAccount createBot(Application application, String oldAccountToken=null){
		return application.createBot(oldAccountToken)
	}

	List<Region> getRegions(){
		return JSONUtil.parse(this.requester.get("voice/regions")).collect { new Region(this, it) }
	}

	List<User> queueUsers(String query, int limit=25){ return JSONUtil.parse(this.requester.get("users?q=${URLEncoder.encode(query)}&limit=${limit}")).collect { new User(this, it) } }
	User getUserInfo(String id){ return new User(this, JSONUtil.parse(this.requester.get("users/${id}"))) }
	User userInfo(String id){ return this.getUserInfo(id) }
	Server getServerInfo(String id){ return new Server(this, JSONUtil.parse(this.requester.get("guilds/${id}"))) }
	Server serverInfo(String id){ return this.getServerInfo(id) }
	Channel getChannelInfo(String id){
		Map response = JSONUtil.parse(this.requester.get("channels/${id}"))
		return (response.type == "text") ? new TextChannel(this, response) : new VoiceChannel(this, response)
	}
	Channel channelInfo(String id){ return this.getChannelInfo(id) }

	/**
	 * Gets a user by its ID.
	 * @param id - the ID.
	 * @return the user. null if not found.
	 */
	User getUserById(String id){
		for (u in this.getAllUsers()){
			if ({ try{ u.id == id }catch (ex){ false } }() as boolean) return u
		}
		return null
	}

	/**
	 * Gets a server by its ID.
	 * @param id - the ID.
	 * @return the server. null if not found.
	 */
	Server getServerById(String id){
		this.servers.find { it.id == id }
	}

	/**
	 * @return all private channels.
	 */
	List<PrivateChannel> getPrivateChannels(){
		return this.cache["private_channels"]?.list ?: []
	}

	Map<String, PrivateChannel> getPrivateChannelMap(){
		return this.cache["private_channels"]?.map ?: []
	}

	List<TextChannel> getTextChannels(){
		return this.privateChannels + this.servers*.textChannels.sum() ?: []
	}

	Map<String, TextChannel> getTextChannelMap(){
		return this.servers*.textChannelMap.sum() ?: [:]
	}

	List<VoiceChannel> getVoiceChannels(){
		return this.servers*.voiceChannels.sum() ?: []
	}

	Map<String, VoiceChannel> getVoiceChannelMap(){
		return this.servers*.voiceChannelMap.sum() ?: [:]
	}

	List<Channel> getChannels(){
		return this.textChannels + this.voiceChannels ?: []
	}

	Map<String, Channel> getChannelMap(){
		return this.textChannelMap + this.voiceChannelMap ?: [:]
	}

	/**
	 * Gets a user by its ID.
	 * @param id - the ID.
	 * @return the user. null if not found.
	 */
	List<Member> members(def id){
		return this.members.findAll { it.id == id }
	}

	/**
	 * Gets a user by its ID.
	 * @param id - the ID.
	 * @return the user. null if not found.
	 */
	User user(def id){
		return this.members.find { it.id == resolveId(id) }
	}

	/**
	 * Gets a server by its ID.
	 * @param id - the ID.
	 * @return the server. null if not found.
	 */
	Server server(def id){
		this.servers.find { it.id == resolveId(id) }
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID.
	 * @return the text channel. null if not found.
	 */
	TextChannel textChannel(def id){
		return this.textChannels.find { it.id == resolveId(id) }
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID.
	 * @return the voice channel. null if not found.
	 */
	VoiceChannel voiceChannel(def id){
		return this.voiceChannels.find { it.id == resolveId(id) }
	}

	Channel channel(def id){
		return this.channels.find { it.id == resolveId(id) }
	}

	Role role(def id){ return this.roles.find { it.id == resolveId(id) } }

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID.
	 * @return the text channel. null if not found.
	 */
	TextChannel getTextChannelById(String id){
		return this.textChannelMap[id]
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID.
	 * @return the voice channel. null if not found.
	 */
	VoiceChannel getVoiceChannelById(String id){
		return this.voiceChannelMap[id]
	}

	Channel getChannelById(String id){
		return this.channelMap[id]
	}

	// Adding built-in listeners. Look above at "removeListenersFor" to understand why I did it like this.

	void addGuildMemberAddListener(){
		this.addListener(Events.MEMBER){ Map d ->
			this.cache["guilds"][d.server.id]["members"].add(d.member.object)
			this.cache["guilds"][d.server.id]["member_count"]++
		}
	}

	void addGuildMemberRemoveListener(){
		this.addListener(Events.MEMBER_REMOVED){ Map d ->
			this.cache["guilds"][d.server.id]["members"].remove(d.member.id)
			this.cache["guilds"][d.server.id]["member_count"]--
		}
	}

	void addGuildRoleCreateListener(){
		this.addListener(Events.ROLE){ Map d ->
			this.cache["guilds"][d.server.id]["roles"].add(d.role.object + ["guild_id": d.server.id])
		}
	}

	void addGuildRoleDeleteListener(){
		this.addListener(Events.ROLE_DELETE){ Map d ->
			this.cache["guilds"][d.server.id]["roles"].remove(d.role.id)
		}
	}

	void addChannelCreateListener(){
		this.addListener(Events.CHANNEL_CREATE){ Map d ->
			if (d.server){
				this.cache["guilds"][d.server.id]["channels"].add(d.fullData + ((d.fullData.type == "text") ? ["cached_messages": []] : [:]))
			}else{
				this.cache["private_channels"].add(d.fullData + ["cached_messages": []])
			}
		}
	}

	void addChannelDeleteListener(){
		this.addListener(Events.CHANNEL_DELETE){ Map d ->
			if (d.server){
				this.cache["private_channels"].remove(d.channel.id)
			}else{
				this.cache["guilds"][d.server.id]["channels"].remove(d.channel.id)
			}
		}
	}

	void addChannelUpdateListener(){
		this.addListener(Events.CHANNEL_UPDATE){ Map d ->
			this.cache["guilds"][d.server.id]["channels"][d.channel.id] << d.channel.object
		}
	}

	void addMessageCreateListener(){
		this.addListener(Events.MESSAGE){ Map d ->
			TextChannel channel = d.message.channel
			if (channel.server){
				this.cache["guilds"][channel.server.id]["channels"][channel.id]["cached_messages"].add(d.message.object)
			}else{
				this.cache["private_channels"][channel.id]["cached_messages"].add(d.message.object)
			}
		}
	}

	void addMessageUpdateListener(){
		this.addListener(Events.MESSAGE_UPDATE){ Map d ->
			if (d.message instanceof Message){
				Channel channel = d.message.channel
				if (channel.server == null){
					this.cache["private_channels"][channel.id]["cached_messages"].find { it.id == d.message.id } << d.message.object
				}else{
					this.cache["guilds"][channel.server.id]["channels"][channel.id]["cached_messages"].find { it.id == d.message.id } << d.message.object
				}
			}else{
				TextChannel channel = d.channel
				if (channel.server == null){
					this.cache["private_channels"][channel.id]["cached_messages"].find { it.id == d.message } << [embeds: d.embeds]
				}else{
					this.cache["guilds"][channel.server.id]["channels"][channel.id]["cached_messages"].find { it.id == d.message}?.leftShift(embeds: d.embeds)
				}
			}
		}
	}

	void addMessageDeleteListener(){
		this.addListener("message delete") { Map d ->
			if (d.message instanceof Message){
				Channel channel = d.message.channel
				if (channel.server == null){
					Map desiredMessage = this.cache["private_channels"][channel.id]["cached_messages"].find { it.id == d.message.id }
					this.cache["private_channels"][channel.id]["cached_messages"].remove(desiredMessage)
				}else{
					this.cache["guilds"][channel.server.id]["channels"][channel.id]["cached_messages"].remove(this.cache["guilds"][channel.server.id]["channels"][channel.id]["cached_messages"].find { it.id == d.message.id })
				}
			}else{}
		}
	}

	void addGuildCreateListener(){
		this.addListener(Events.SERVER){ Map d ->
			Map server = d.server.object
			server.members.each { Map m ->
				m["guild_id"] = server["id"]
				m << m["user"]
			}
			server["members"] = new DiscordListCache(server["members"], client, Member)
			server.presences.each { Map p ->
				p["guild_id"] = server["id"]
				p << p["user"]
			}
			server["presences"] = new DiscordListCache(server["presences"], client, Presence)
			server.channels.each { Map c ->
				c["guild_id"] = server["id"]
				if (c["type"] == "text"){
					c["cached_messages"] = []
				}
				c["permission_overwrites"].each { Map po ->
					po["channel_id"] = c["id"]
				}
				c["permission_overwrites"] = new DiscordListCache(c["permission_overwrites"], client, Channel.PermissionOverwrite)
			}
			server["channels"] = new ChannelListCache(server["channels"], client)
			server.emojis.each { Map e ->
				e["guild_id"] = server["id"]
			}
			server["emojis"] = new DiscordListCache(server["emojis"], client, Emoji)
			server.roles.each { Map e ->
				e["guild_id"] = server["id"]
			}
			server["roles"] = new DiscordListCache(server["roles"], client, Role)
			if (this.cache["guilds"].any { k, v -> k == server["id"] }){
				this.cache["guilds"][server["id"]] << server
			}else{
				this.cache["guilds"].add(server)
			}
		}
	}

	void addGuildDeleteListener(){
		this.addListener(Events.SERVER_DELETED){ Map d ->
			this.cache["guilds"].remove(d.server.id)
		}
	}

	void addGuildMemberUpdateListener(){
		this.addListener(Events.MEMBER_UPDATED){ Map d ->
			this.cache["guilds"][d.server.id]["members"][d.member?.id]?.leftShift(d.member?.object) ?: null
		}
	}

	void addGuildRoleUpdateListener(){
		this.addListener(Events.ROLE_UPDATE){ Map d ->
			this.cache["guilds"][d.server.id]["roles"][d.role.id] << d.role.object
		}
	}

	void addGuildUpdateListener(){
		this.addListener(Events.SERVER_UPDATED){ Map d ->
			this.cache["guilds"][d.server.id] << d.server.object
		}
	}

	void addPresenceUpdateListener(){
		this.addListener("presence change", { Map d ->
			Server server = d.server
			if (server != null){
				if (d.member == null){
					if (!allowMemberRequesting){
						d.fullData["joined_at"] = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
						this.cache["guilds"][server.id]["members"].add(d.fullData)
					}else{
						this.cache["guilds"][server.id]["members"].add(server.memberInfo(d.fullData.user.id).object)
					}
					return
				}
				if (this.cache["guilds"][server.id]["members"][d.member.id] == null){
					this.cache["guilds"][server.id]["members"].add(d.member.object)
				}else{
					this.cache["guilds"][server.id]["members"][d.member.id]["user"] << d.fullData.user
				}
				if (this.cache["guilds"][server.id]["presences"][d.member.id] == null){
					this.cache["guilds"][server.id]["presences"].add([status: d.status, game: (d.game == "") ? null : [name: d.game], user: [id: d.fullData["user"]["id"]], guild_id: server.id])
				}else{
					this.cache["guilds"][server.id]["presences"][d.member.id] << [status: d.status, game: d.fullData.game]
				}
			}else{
				if (this.cache["relationships"][d.member.id] == null){
					this.cache["relationships"].add(d.member.object)
				}else{
					this.cache["relationships"][d.member.id]["user"] << d.fullData.user
				}
				if (this.cache["presences"][d.member.id] == null){
					this.cache["presences"].add([status: d.status, game: (d.game == "") ? null : [name: d.game], user: [id: d.fullData["user"]["id"]], guild_id: server.id])
				}else{
					this.cache["presences"][d.member.id] << [status: d.status, game: d.fullData.game]
				}
			}
		})
	}

	void addVoiceStateUpdateListener(){
		this.addListener "voice state change", { Map d ->
			/*Server server = d.voiceState.server
			Map serverToRemove = this.cache["guilds"].find { it["id"] == server.id }
			Map copyOfServerToRemove = serverToRemove
			if (d.fullData["channel_id"] != null){
				def existingVoiceState = copyOfServerToRemove.voice_states.find { it.user_id == d.fullData["user_id"] }
				if (existingVoiceState != null){ copyOfServerToRemove.voice_states.remove(existingVoiceState) }
				copyOfServerToRemove.voice_states.add(d.voiceState.object)
			}else{
				def existingVoiceState = copyOfServerToRemove.voice_states.find { it.user_id == d.fullData["user_id"] }
				if (existingVoiceState != null){ copyOfServerToRemove.voice_states.remove(existingVoiceState) }
			}
			this.cache["guilds"].remove(serverToRemove)
			this.cache["guilds"].add(copyOfServerToRemove)*/
		}
	}

	void addGuildEmojisUpdateListener(){
		this.addListener "guild emojis change", { Map d ->
			this.cache["guilds"][d.server.id]["emojis"] = new DiscordListCache(this, d.emojis.mapList, Emoji)
		}
	}

	void addUserUpdateListener(){
		this.addListener "user change", { Map d ->
			this.cache["user"] << d.fullData
		}
	}

	void addReadyListener(){
		this.addListener("ready"){ Map d ->
			this.object = this.user.object
		}
	}
}
