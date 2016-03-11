package ml.hlaaftana.discordg.objects

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ml.hlaaftana.discordg.request.*
import ml.hlaaftana.discordg.util.*
import ml.hlaaftana.discordg.objects.VoiceClient

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * The Discord client.
 * @author Hlaaftana
 */
class Client {
	static final String version = this.class.getResourceAsStream("/version").text
	static final String github = "https://github.com/hlaaftana/DiscordG"
	static final String userAgent = "DiscordBot ($github, $version)"
	String customUserAgent = ""
	String getFullUserAgent(){ "$userAgent $customUserAgent" }

	Requester requester
	String token
	String email
	String password
	WSClient wsClient
	VoiceWSClient voiceWsClient
	VoiceClient voiceClient
	Map<String, List<Closure>> listeners = new HashMap<String, List<Closure>>()
	Map readyData = [:]
	Map voiceData = [:]
	String gateway
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]
	boolean cacheTokens = true
	String tokenCachePath = "token.json"
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	boolean ignorePresenceUpdate = false // if your bot is on tons of big servers, this might help you lose some CPU
	int largeThreshold = 250 // if your bot is on tons of big servers, setting this to lower numbers might help your startup time
	boolean copyReady = true // adds fullData to ready to archive the initial ready

	/**
	 * Builds a new client. This is safe to do.
	 */
	Client(){
		requester = new Requester(this)
		Unirest.setObjectMapper(new ObjectMapper() {
			public <T> T readValue(String value, Class<T> valueType) {
				return JSONUtil.parse(value);
			}

			String writeValue(Object value) {
				return JSONUtil.json(value);
			}
		})

		// oh boy am i gonna get hate for this
		// check reason below where i define these
		this.addChannelCreateListener()
		this.addChannelDeleteListener()
		this.addChannelUpdateListener()
		this.addMessageCreateListener()
		this.addMessageUpdateListener()
		this.addMessageDeleteListener()
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
	}

	WSClient getWebSocketClient(){ return wsClient }

	Client startAnew(){
		Client newApi = new Client()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
		def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
		commonProps.each { newApi[it] = !(it in ["requester", "wsClient", "client", "readyData", "voiceData", "loaded"]) ? this[it] : null }
		return newApi
	}

	/**
	 * Logs onto Discord.
	 * @param email - the email to log in with.
	 * @param password - the password to use.
	 */
	void login(String email, String password){
		this.email = email
		this.password = password
		File tokenCache = new File(tokenCachePath)
		if (tokenCache.exists()){
			try{
				if (cacheTokens){
					token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
				}else{
					token = JSONUtil.parse(this.requester.post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
				}
			}catch (ex){
				if (cacheTokens){
					tokenCache.write(JsonOutput.prettyPrint(JSONUtil.json(new JsonSlurper().parse(tokenCache) + [(email): JSONUtil.parse(this.requester.post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))])))

					token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
				}else{
					token = JSONUtil.parse(this.requester.post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
				}
			}
		}else{
			if (cacheTokens){
				tokenCache.createNewFile()
				tokenCache.write(JsonOutput.prettyPrint(JSONUtil.json(new JsonSlurper().parse(tokenCache) + [(email): JSONUtil.parse(this.requester.post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))])))

				token = new JsonSlurper().parse(tokenCache)[(email)]["token"]
			}else{
				token = JSONUtil.parse(this.requester.post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
			}
		}
		this.login(this.token)
	}

	void login(String token){
		Thread thread = new Thread({
			try{
				Log.info "Logging in..."
				this.token = token
				SslContextFactory sslFactory = new SslContextFactory()
				WebSocketClient client = new WebSocketClient(sslFactory)
				WSClient socket = new WSClient(this)
				gateway = JSONUtil.parse(this.requester.get("https://discordapp.com/api/gateway"))["url"]
				client.start()
				ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
				client.connect(socket, new URI(gateway), upgreq)
				this.wsClient = socket
				Log.info "Successfully logged in!"
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
		})
		thread.start()
	}

	void logout(boolean exit = false){
		try{
			this.requester.post("https://discordapp.com/api/auth/logout", ["token": this.token])
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
	void addListener(String event, Closure closure) {
		for (e in listeners.entrySet()){
			if (e.key == parseEventType(event)){
				e.value.add(closure)
				return
			}
		}
		listeners.put(parseEventType(event), [closure])
	}

	/**
	 * Removes a listener from an event.
	 * @param event - the event name. This is manipulated by API#parseEventType.
	 * @param closure - the closure to remove.
	 */
	void removeListener(String event, Closure closure) {
		for (e in listeners.entrySet()){
			if (e.key == parseEventType(event)){
				e.value.remove(closure)
			}
		}
	}

	/**
	 * Remove all listeners for an event (containing built-in listeners. you can add them back using the "addBlankEventExampleListener()" methods in this class. yay low-levelism)
	 * @param event - the event name. This is manipulated by API#parseEventType.
	 */
	void removeListenersFor(String event){
		for (e in listeners.entrySet()){
			if (e.key == parseEventType(event)){
				listeners[e.key] = []
				return
			}
		}
	}

	void removeAllListeners(){
		this.listeners = [:]
	}

	/**
	 * Returns an event name from a string by; <br>
	 * 1. Replacing "change" with "update" and "server" with "guild" (case insensitive), <br>
	 * 2. Making it uppercase, and <br>
	 * 3. Replacing spaces with underscores.
	 * @param str - the string.
	 * @return the event name.
	 */
	static String parseEventType(String str){
		return str.toUpperCase().replace("CHANGE", "UPDATE").replace("SERVER", "GUILD").replace(' ', '_')
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
	void dispatchEvent(String type, Map data){
		this.listeners.each { Map.Entry<String, List<Closure>> entry ->
			if (type == entry.key){
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
		return requester != null && token != null && wsClient != null && readyData != null && readyData["guilds"]?.size() == this.servers.size()
	}

	/**
	 * @return the user which the client is logged in to.
	 */
	User getUser(){ return new User(this, this.readyData["user"]) }

	boolean isVerified(){ return this.readyData["user"]["verified"] }

	/**
	 * @return the session ID for the session.
	 */
	String getSessionId(){ return this.readyData["session_id"] }

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
		return new Server(this, JSONUtil.parse(this.requester.post("https://discordapp.com/api/guilds", ["name": data.name.toString(), "region": (data.region ?: "london").toString()])))
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
		return this.readyData["guilds"].collect { new Server(this, it) }
	}

	List<Server> requestServers(){
		List array = JSONUtil.parse(this.requester.get("https://discordapp.com/api/users/@me/guilds"))
		List<Server> servers = []
		for (s in array){
			def serverInReady = this.readyData["guilds"].find { it["id"] == s["id"] }
			servers.add(new Server(this, s << ["channels": serverInReady["channels"], "members": serverInReady["members"], "presences": serverInReady["presences"], "voice_states": serverInReady["voice_states"], "large": serverInReady["large"]]))
		}
		return servers
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
			if (m == null) println "wtf"; continue
			if (!(m.id in ass*.id)){ ass += m.user }
		}
		return ass
	}

	/**
	 * @return a List of Members the client can see. Same users can be different member objects.
	 */
	List<Member> getAllMembers() {
		return this.servers*.members.inject([]){ a, m -> a += m }
	}

	List<Role> getAllRoles(){
		return this.servers*.roles.inject([]){ a, r -> a += r }
	}

	List<User> getUsers(){ return this.allUsers }
	List<Member> getMembers(){ return this.allMembers }
	List<Role> getRoles(){ return this.allRoles }

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
	void kickMember(Member member) {
		member.kick()
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
		this.wsClient.send(["op": 3, "d": ["game": ["name": data["game"]], "idle_since": (data["idle"] != null) ? System.currentTimeMillis() : null]])
		for (s in this.servers){
			this.dispatchEvent("PRESENCE_UPDATE", [
				"fullData": [
					"game": (data["game"] != null) ? ["name": data["game"]] : null,
					"status": (data["idle"] != null) ? "online" : "idle",
					"guild_id": s.id, "user": this.user.object
					],
				"server": s,
				"guild": s,
				"member": s.members.find { try{ it.id == this.user.id }catch (ex){ false } },
				"game": (data["game"] != null) ? data["game"] : null,
				"status": (data["idle"] != null) ? "online" : "idle"
				])
		}
	}

	void play(String game){ this.changeStatus(game: game) }
	void playGame(String game){ this.changeStatus(game: game) }

	/**
	 * Accepts an invite and joins a new server.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return a new Invite object of the accepted invite.
	 */
	Invite acceptInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(this, JSONUtil.parse(this.requester.post("https://discordapp.com/api/invite/${Invite.parseId(link)}", [:])))
		else
			return new Invite(this, JSONUtil.parse(this.requester.post("https://discordapp.com/api/invite/${link}", [:])))
	}

	/**
	 * Gets an Invite object from a link/ID.
	 * @param link - the link of the invite. Can also be an ID, however you have to set
	 * @param isIdAlready - to true.
	 * @return the gotten invite.
	 */
	Invite getInvite(String link, boolean isIdAlready=false){
		if (!isIdAlready)
			return new Invite(this, JSONUtil.parse(this.requester.get("https://discordapp.com/api/invite/${Invite.parseId(link)}")))
		else
			return new Invite(this, JSONUtil.parse(this.requester.get("https://discordapp.com/api/invite/${link}")))
	}

	/**
	 * Creates an invite.
	 * @param dest - The destination for the invite. Can be a Server, a Channel, or the ID of a channel.
	 * @return the created invite.
	 */
	Invite createInvite(def dest, Map data=[:]){
		String id = (dest instanceof Channel) ? dest.id : (dest instanceof Server) ? dest.defaultChannel.id : dest
		return new Invite(this, JSONUtil.parse(this.requester.post("https://discordapp.com/api/channels/${id}/invites", data)))
	}

	List<Invite> getInvitesFor(Server server){
		return server.invites
	}

	List<Invite> getInvitesFor(Channel channel){
		return channel.invites
	}

	List<Connection> getConnections(){
		return JSONUtil.parse(this.requester.get("https://discordapp.com/api/users/@me/connections")).collect { new Connection(this, it) }
	}

	void moveToChannel(Member member, VoiceChannel channel){
		this.requester.patch("https://discordapp.com/api/guilds/${member.server.id}/members/{member.id}", ["channel_id": channel.id])
	}

	/**
	 * Edits the user's profile.
	 * @param data - the data to edit with. Be really careful with this. Can be: <br>
	 * [email: "newemail@dock.org", new_password: "oopsaccidentallygavesomeonemypass", username: "New name new me"] <br>
	 * Note that you can also have an avatar property in the map above, but I'm not encouraging it until I provide a utility function for that.
	 * @return a User object for the edited profile.
	 */
	User editProfile(Map data){
		Map map = ["avatar": this.user.avatarHash, "email": this.email, "password": this.password, "username": this.user.username]
		if (data["avatar"] != null){
			if (data["avatar"] instanceof String && !(data["avatar"].startsWith("data"))){
				data["avatar"] = ConversionUtil.encodeToBase64(data["avatar"] as File)
			}else if (data["avatar"] instanceof File){
				data["avatar"] = ConversionUtil.encodeToBase64(data["avatar"])
			}
		}
		Map response = JSONUtil.parse this.requester.patch("https://discordapp.com/api/users/@me", map << data)
		this.email = response.email
		this.password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : this.password
		this.token = response.token
		this.readyData["user"]["email"] = response.email
		this.readyData["user"]["verified"] = response.verified
		return new User(this, response)
	}

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
		List channels = this.readyData["private_channels"]
		List<PrivateChannel> pcs = new ArrayList<PrivateChannel>()
		for (pc in channels){
			pcs.add(new PrivateChannel(this, pc))
		}
		return pcs
	}

	/**
	 * Gets a text channel by its ID.
	 * @param id - the ID.
	 * @return the text channel. null if not found.
	 */
	TextChannel getTextChannelById(String id){
		for (s in this.servers){
			for (c in s.textChannels){
				if (c.id == id) return c
			}
		}
		for (pc in this.privateChannels){
			if (pc.id == id) return pc
		}
	}

	/**
	 * Gets a voice channel by its ID.
	 * @param id - the ID.
	 * @return the voice channel. null if not found.
	 */
	VoiceChannel getVoiceChannelById(String id){
		for (s in this.servers){
			for (c in s.voiceChannels){
				if (c.id == id) return c
			}
		}
	}



	// Adding built-in listeners. Look above at "removeListenersFor" to understand why I did it like this.

	void addGuildMemberAddListener(){
		this.addListener("guild member add", { Map d ->
			Server server = d.server
			this.readyData["guilds"].find { it["id"] == server.id }["members"].add(d)
			this.readyData["guilds"].find { it["id"] == server.id }["member_count"]++
		})
	}

	void addGuildMemberRemoveListener(){
		this.addListener("guild member remove", { Map d ->
			try{
				Map memberObject = this.readyData["guilds"].find { it["id"] == d.fullData["guild_id"] }["members"].find { it.user.id == d.fullData["user"]["id"] }
				this.readyData["guilds"].find { it["id"] == d.fullData["guild_id"] }["members"].remove(memberObject)
				this.readyData["guilds"].find { it["id"] == d.fullData["guild_id"] }["member_count"]--
			}catch (ex){
				println d
			}
		})
	}

	void addGuildRoleCreateListener(){
		this.addListener("guild role create", { Map d ->
			Server server = d.server
			this.readyData["guilds"].find { it["id"] == server.id }["roles"].add(d.role.object)
		})
	}

	void addGuildRoleDeleteListener(){
		this.addListener("guild role delete", { Map d ->
			Server server = d.server
			Map roleObject = this.readyData["guilds"].find { it["id"] == server.id }["roles"].find { it.id == d.role.id }
			this.readyData["guilds"].find { it["id"] == server.id }["roles"].remove(roleObject)
		})
	}

	void addChannelCreateListener(){
		this.addListener("channel create", { Map d ->
			Server server = d.server
			if (server == null){
				this.readyData["private_channels"].add(d.fullData << ["cached_messages": []])
			}else{
				this.readyData["guilds"].find { it.id == server.id }["channels"].add(d.fullData << ["cached_messages": []])
			}
		})
	}

	void addChannelDeleteListener(){
		this.addListener("channel delete", { Map d ->
			Server server = d.server
			if (server == null){
				Map channelToRemove = this.readyData["private_channels"].find { it["id"] == d.channel.id }
				this.readyData["private_channels"].remove(channelToRemove)
			}else{
				Map channelToRemove = this.readyData["guilds"].find { it.id == server.id }["channels"].find { it["id"] == d.channel.id }
				this.readyData["guilds"].find { it.id == server.id }["channels"].remove(channelToRemove)
			}
		})
	}

	void addChannelUpdateListener(){
		this.addListener("channel update", { Map d ->
			Server server = d.server
			this.readyData["guilds"].find { it.id == server.id }["channels"].find { it.id == d.channel.id } << d.channel.object
		})
	}

	void addMessageCreateListener(){
		this.addListener("message create") { Map d ->
			TextChannel channel = d.message.channel
			if (channel.server == null){
				this.readyData["private_channels"].find { it.id == channel.id }["cached_messages"].add(d.message.object)
			}else{
				this.readyData["guilds"].find { it.id == channel.server.id }["channels"].find { it.id == channel.id }["cached_messages"].add(d.message.object)
			}
		}
	}

	void addMessageUpdateListener(){
		this.addListener("message update") { Map d ->
			if (d.message instanceof Message){
				Channel channel = d.message.channel
				if (channel.server == null){
					this.readyData["private_channels"].find { it.id == channel.id }["cached_messages"].find { it.id == d.message.id }.leftShift(d.message.object)
				}else{
					this.readyData["guilds"].find { it.id == channel.server.id }["channels"].find { it.id == channel.id }["cached_messages"].find { it.id == d.message.id }?.leftShift(d.message.object)
				}
			}else{
				Channel channel = d.channel
				if (channel.server == null){
					this.readyData["private_channels"].find { it.id == channel.id }["cached_messages"].find { it.id == d.message }.leftShift(embeds: d.embeds)
				}else{
					this.readyData["guilds"].find { it.id == channel.server.id }["channels"].find { it.id == channel.id }["cached_messages"].find { it.id == d.message }?.leftShift(embeds: d.embeds)
				}
			}
		}
	}

	void addMessageDeleteListener(){
		this.addListener("message delete") { Map d ->
			if (d.message instanceof Message){
				Channel channel = d.message.channel
				if (channel.server == null){
					Map desiredMessage = this.readyData["private_channels"].find { it.id == d.channel.id }["cached_messages"].find { it.id == d.message.id }
					this.readyData["private_channels"].find { it.id == channel.id }["cached_messages"].remove(desiredMessage)
				}else{
					this.readyData["guilds"].find { it.id == channel.server.id }["channels"].find { it.id == channel.id }["cached_messages"].remove(this.readyData["guilds"].find { it.id == channel.server.id }["channels"].find { it.id == channel.id }["cached_messages"].find { it.id == d.message.id })
				}
			}else{}
		}
	}

	void addGuildCreateListener(){
		this.addListener("guild create", { Map d ->
			Server server = d.server
			this.readyData["guilds"].add(server.object)
		})
	}

	void addGuildDeleteListener(){
		this.addListener("guild delete", { Map d ->
			Server server = d.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == server.id }
			this.readyData["guilds"].remove(serverToRemove)
		})
	}

	void addGuildMemberUpdateListener(){
		this.addListener("guild member update", { Map d ->
			Server server = d.server
			Member member = d.member
			this.readyData["guilds"].find { it["id"] == server.id }["members"].find { it["user"]["id"] == member.id }?.leftShift(member.object) ?: server.requestMembers()
		})
	}

	void addGuildRoleUpdateListener(){
		this.addListener("guild role update", { Map d ->
			Server server = d.server
			Role role = d.role
			this.readyData["guilds"].find { it["id"] == server.id }["roles"].find { it["id"] == role.id } << role.object
		})
	}

	void addGuildUpdateListener(){
		this.addListener("guild update", { Map d ->
			Map newServer = d.server
			this.readyData["guilds"].find { it["id"] == newServer.id } << newServer.object
		})
	}

	void addPresenceUpdateListener(){
		this.addListener("presence change", { Map d ->
			Server server = d.server
			if (d.member == null){
				this.readyData["guilds"].find { it["id"] == server.id }["presences"] += [status: d.status, game: (d.game == "") ? null : [name: d.game], user: [id: d.fullData["user"]["id"]]]
				return
			}
			if (d["newUser"] != null){
				this.readyData["guilds"].find { it["id"] == server.id }["members"].find { it["user"]["id"] == d.newUser.id }?.leftShift(d.newUser.object) ?: server.requestMembers()
			}
			try{ this.readyData["guilds"].find { it["id"] == server.id }["presences"].find { it["user"]["id"] == d.member.id }?.leftShift([status: d.status, game: (d.game == "") ? null : [name: d.game]]) }catch (ex){
				this.readyData["guilds"].find { it["id"] == server.id }["presences"] += [status: d.status, game: (d.game == "") ? null : [name: d.game], user: [id: d.member.id]]
			}
		})
	}

	void addVoiceStateUpdateListener(){
		this.addListener "voice state change", { Map d ->
			/*Server server = d.voiceState.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == server.id }
			Map copyOfServerToRemove = serverToRemove
			if (d.fullData["channel_id"] != null){
				def existingVoiceState = copyOfServerToRemove.voice_states.find { it.user_id == d.fullData["user_id"] }
				if (existingVoiceState != null){ copyOfServerToRemove.voice_states.remove(existingVoiceState) }
				copyOfServerToRemove.voice_states.add(d.voiceState.object)
			}else{
				def existingVoiceState = copyOfServerToRemove.voice_states.find { it.user_id == d.fullData["user_id"] }
				if (existingVoiceState != null){ copyOfServerToRemove.voice_states.remove(existingVoiceState) }
			}
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(copyOfServerToRemove)*/
		}
	}

	void addGuildEmojisUpdateListener(){
		this.addListener "guild emoji change", { Map d ->
			Server server = d.server
			this.readyData["guilds"].find { it.id == server.id }["emojis"] = d.emojis.collect { it.object }
		}
	}

	void addUserUpdateListener(){
		this.addListener "user change", { Map d ->
			this.readyData["user"] << d.fullData
		}
	}
}
