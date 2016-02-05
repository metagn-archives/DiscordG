package ml.hlaaftana.discordg.objects

import com.mashape.unirest.http.*

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.lang.Closure
import ml.hlaaftana.discordg.request.*
import ml.hlaaftana.discordg.util.*
import ml.hlaaftana.discordg.objects.VoiceClient

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

/**
 * Where the fun happens.
 * @author Hlaaftana
 */
class API{
	Requester requester
	String token
	String email
	String password
	WSClient wsClient
	Client client
	VoiceWSClient voiceWsClient
	VoiceClient voiceClient
	Map<String, List<Closure>> listeners = new HashMap<String, List<Closure>>()
	Map readyData
	Map voiceData = [:]
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]
	boolean cacheTokens = true
	String tokenCachePath = "token.json"
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	boolean ignorePresenceUpdate = false // if your bot is on tons of big servers, this might help you lose some CPU
	int largeThreshold // if your bot is on tons of big servers, this might help your request speeds
	boolean copyReady = true // adds fullData to ready to archive the initial ready

	/**
	 * Builds a new API object. This is safe to do.
	 */
	API(){
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
	}

	WSClient getWebSocketClient(){ return wsClient }

	API startAnew(){
		API newApi = new API()
		def (sProps, tProps) = [this, newApi]*.properties*.keySet()
	    def commonProps = sProps.intersect(tProps) - ["class", "metaClass"]
	    commonProps.each { newApi[it] = !(it in ["requester", "wsClient", "client", "listeners", "readyData", "voiceData"]) ? this[it] : null }
		return newApi.with { login(delegate.email, delegate.password) }
	}

	/**
	 * Logs onto Discord.
	 * @param email - the email to log in with.
	 * @param password - the password to use.
	 */
	void login(String email, String password){
		Thread thread = new Thread({
			try{
				Log.info "Logging in..."
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
				SslContextFactory sslFactory = new SslContextFactory()
				WebSocketClient client = new WebSocketClient(sslFactory)
				WSClient socket = new WSClient(this)
				String gateway = JSONUtil.parse(this.requester.get("https://discordapp.com/api/gateway"))["url"]
				client.start()
				ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
				client.connect(socket, new URI(gateway), upgreq)
				this.wsClient = socket
				this.client = new ml.hlaaftana.discordg.objects.Client(this)
				Log.info "Successfully logged in!"
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
		})
		thread.start()
	}

	void logout(){
		try{
			this.requester.post("https://discordapp.com/api/auth/logout", ["token": this.token])
			this.wsClient.keepAliveThread.interrupt()
			this.wsClient.session.close(0, "Logout")
			this.wsClient = null
			this.token = null
			this.requester = null
			this.client = null
			System.exit(0)
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
				listeners.remove(e.key, e.value)
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
	String parseEventType(String str){
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
			try{
				if (type == entry.key){
					for (c in entry.value){
						c.call(data)
					}
				}
			}catch (ex){
				if (Log.enableListenerCrashes) ex.printStackTrace()
				Log.info "Ignoring exception from event " + entry.key
			}
		}
	}

	/**
	 * @return whether or not the api is loaded.
	 */
	boolean isLoaded(){
		return requester != null && token != null && wsClient != null && client != null && readyData != null
	}

	// Adding built-in listeners. Look above at "removeListenersFor" to understand why I did it like this.

	void addGuildMemberAddListener(){
		this.addListener("guild member add", { Map d ->
			Server server = d.server
			Map serverInReady = this.readyData["guilds"].find { it["id"] == server.id }
			Map serverInReady2 = serverInReady
			serverInReady2["members"].add(d)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	void addGuildMemberRemoveListener(){
		this.addListener("guild member remove", { Map d ->
			try{
				Server server = d.server
				Member memberToRemove = d.member
				Map serverInReady = this.readyData["guilds"].find { it["id"] == server.id }
				Map serverInReady2 = serverInReady.subMap(serverInReady.keySet())
				serverInReady2["members"].remove(memberToRemove.object)
				this.readyData["guilds"].remove(serverInReady)
				this.readyData["guilds"].add(serverInReady2)
			}catch (ex){}
		})
	}

	void addGuildRoleCreateListener(){
		this.addListener("guild role create", { Map d ->
			Server server = d.server
			Map serverInReady = this.readyData["guilds"].find { it["id"] == server.id }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].add(d.role.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	void addGuildRoleDeleteListener(){
		this.addListener("guild role delete", { Map d ->
			Server server = d.server
			Role roleToRemove = d.role
			Map serverInReady = this.readyData["guilds"].find { it["id"] == server.id }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].remove(roleToRemove.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
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
			Map channelToRemove = this.readyData["guilds"].find { it.id == server.id }["channels"].find { it.id == d.channel.id }
			Map copyOfChannelToRemove = channelToRemove
			copyOfChannelToRemove << d.channel.object
			this.readyData["guilds"].find { it.id == server.id }["channels"].remove(channelToRemove)
			this.readyData["guilds"].find { it.id == server.id }["channels"].add(copyOfChannelToRemove)
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
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == server.id }
			List membersToEdit = serverToRemove["members"]
			Map memberToEdit = membersToEdit.find { it["user"]["id"] == member.id }
			membersToEdit.remove(memberToEdit)
			membersToEdit.add(member.object)
			this.readyData["guilds"].remove(serverToRemove)
			serverToRemove["members"] = membersToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
	}

	void addGuildRoleUpdateListener(){
		this.addListener("guild role update", { Map d ->
			Server server = d.server
			Role role = d.role
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == server.id }
			List rolesToEdit = serverToRemove["roles"]
			Map roleToEdit = rolesToEdit.find { it["id"] == role.id }
			rolesToEdit.remove(roleToEdit)
			rolesToEdit.add(role.object)
			this.readyData["guilds"].remove(this.readyData["guilds"].find { it["id"] == server.id })
			serverToRemove["roles"] = rolesToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
	}

	void addGuildUpdateListener(){
		this.addListener("guild update", { Map d ->
			Map newServer = d.server.object
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == newServer.id }
			Map copyOfServerToRemove = serverToRemove
			copyOfServerToRemove << newServer
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(copyOfServerToRemove)
		})
	}

	void addPresenceUpdateListener(){
		this.addListener("presence change", { Map d ->
			Server server = d.server
			if (server == null){ server = new Server(this, ["id": d.fullData["guild_id"]]) }
			Map serverToAdd = this.readyData["guilds"].find { it["id"] == d.fullData["guild_id"] }
			Map copyServer = serverToAdd
			List members = copyServer.members.collect({ it })
			if (d["newUser"] != null){
				try{
					Map m = members.find { it["user"]["id"] == d.newUser.id }
					Map m2 = m
					m2.user = d.newUser.object
					serverToAdd.members.remove(m)
					serverToAdd.members.add(m2)
				}catch (ex){

				}
			}
			List presences = copyServer.presences.collect({ it })
			try{
				Map p = presences.find { it["user"]["id"] == d.member.id }
				Map p2 = p
				p2.status = d.status
				if (d.game == ""){
					p2.game = null
				}else{
					p2.game = [name: d.game]
				}
				serverToAdd.presences.remove(p)
				serverToAdd.presences.add(p)
			}catch (ex){

			}
			Map serverToRemove = this.readyData["guilds"].find { it["id"] == server.id }
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(serverToAdd)
		})
	}

	void addVoiceStateUpdateListener(){
		this.addListener "voice state change", { Map d ->
			Server server = d.voiceState.server
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
			this.readyData["guilds"].add(copyOfServerToRemove)
		}
	}
}
