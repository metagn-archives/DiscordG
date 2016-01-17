package ml.hlaaftana.discordg.objects

import java.nio.channels.MembershipKey;

import com.mashape.unirest.http.*

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.lang.Closure
import ml.hlaaftana.discordg.request.*
import ml.hlaaftana.discordg.util.*

import javax.websocket.WebSocketContainer
import javax.websocket.ContainerProvider

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
	ml.hlaaftana.discordg.objects.Client client
	Map<String, List<Closure>> listeners = new HashMap<String, List<Closure>>()
	Map readyData
	Map voiceData = [:]
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]
	boolean cacheTokens = true
	int eventThreadCount = 3 // if your bot is on tons of big servers, this might help however take up some CPU
	boolean ignorePresenceUpdate = false // if your bot is on tons of big servers, this might help you lose some CPU

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
	}

	WSClient getWebSocketClient(){ return wsClient }

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
				File tokenCache = new File("token.json")
				if (tokenCache.exists()){
					try{
						if (cacheTokens){
							token = new JsonSlurper().parse(tokenCache)["token"]
						}else{
							token = JSONUtil.parse(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
						}
					}catch (ex){
						if (cacheTokens){
							Log.warn "Token cache file seems to be malformed."
							Log.debug "Token cache file seems to be malformed, full file: " + tokenCache.text

							tokenCache.createNewFile()
							tokenCache.write(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))

							token = new JsonSlurper().parseFile(tokenCache)["token"]
						}else{
							token = JSONUtil.parse(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
						}
					}
				}else{
					if (cacheTokens){
						tokenCache.createNewFile()
						tokenCache.write(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))

						token = new JsonSlurper().parse(tokenCache)["token"]
					}else{
						token = JSONUtil.parse(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))["token"]
					}
				}
				SslContextFactory sslFactory = new SslContextFactory()
				WebSocketClient client = new WebSocketClient(sslFactory)
				WSClient socket = new WSClient(this)
				String gateway = JSONUtil.parse(this.getRequester().get("https://discordapp.com/api/gateway"))["url"]
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

	/**
	 * Returns an event name from a string by; <br>
	 * 1. Replacing "CHANGE" and "change" with "UPDATE" and "update" respectively, <br>
	 * 2. Making it uppercase, and <br>
	 * 3. Replacing spaces with underscores.
	 * @param str - the string.
	 * @return the event name.
	 */
	String parseEventType(String str){
		return str.replace("change", "update").replace("update".toUpperCase(), "change".toUpperCase()).toUpperCase().replace(' ', '_')
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
	void dispatchEvent(Event event){
		this.listeners.each { Map.Entry<String, List<Closure>> entry ->
			try{
				if (event.type.equals(entry.key)){
					for (c in entry.value){
						c.call(event)
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
		this.addListener("guild member add", { Event e ->
			Server server = e.data.server
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["members"].add(e.data)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	void addGuildMemberRemoveListener(){
		this.addListener("guild member remove", { Event e ->
			Server server = e.data.server
			Member memberToRemove = e.data.member
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["members"].remove(memberToRemove.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	void addGuildRoleCreateListener(){
		this.addListener("guild role create", { Event e ->
			Server server = e.data.server
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].add(e.data.role.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	void addGuildRoleDeleteListener(){
		this.addListener("guild role delete", { Event e ->
			Server server = e.data.server
			Role roleToRemove = e.data.role
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].remove(roleToRemove.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
	}

	// our (my) server objects don't read from READY to get their channels
	// however we might in the future because they did that to the
	// members request already
	// feel free to add your own listener in your code / PR
	// to make sure this works to read from READY too
	void addChannelCreateListener(){
		this.addListener("channel create", { Event e ->
			Server server = e.data.server
			if (server == null){
				this.readyData["private_channels"].add(e.data.fullData)
			}
		})
	}

	void addChannelDeleteListener(){
		this.addListener("channel delete", { Event e ->
			Server server = e.data.server
			if (server == null){
				Map channelToRemove = this.readyData["private_channels"].find { it["id"].equals(e.data.channel.getId()) }
				this.readyData["private_channels"].remove(channelToRemove)
			}
		})
	}

	void addGuildCreateListener(){
		this.addListener("guild create", { Event e ->
			Server server = e.data.server
			this.readyData["guilds"].add(server.object)
		})
	}

	void addGuildDeleteListener(){
		this.addListener("guild delete", { Event e ->
			Server server = e.data.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			this.readyData["guilds"].remove(serverToRemove)
		})
	}

	void addGuildMemberUpdateListener(){
		this.addListener("guild member update", { Event e ->
			Server server = e.data.server
			Member member = e.data.member
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			List membersToEdit = serverToRemove["members"]
			Map memberToEdit = membersToEdit.find { it["user"]["id"].equals(member.getId()) }
			membersToEdit.remove(memberToEdit)
			membersToEdit.add(member.object)
			this.readyData["guilds"].remove(serverToRemove)
			serverToRemove["members"] = membersToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
	}

	void addGuildRoleUpdateListener(){
		this.addListener("guild role update", { Event e ->
			Server server = e.data.server
			Role role = e.data.role
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			List rolesToEdit = serverToRemove["roles"]
			Map roleToEdit = rolesToEdit.find { it["id"].equals(role.getId()) }
			rolesToEdit.remove(roleToEdit)
			rolesToEdit.add(role.object)
			this.readyData["guilds"].remove(this.readyData["guilds"].find { it["id"].equals(server.id) })
			serverToRemove["roles"] = rolesToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
	}

	void addGuildUpdateListener(){
		this.addListener("guild update", { Event e ->
			Server newServer = e.data.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(newServer.getId()) }
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(newServer.object)
		})
	}

	void addPresenceUpdateListener(){
		this.addListener("presence change", { Event e ->
			Server server = e.data.server
			if (server == null){ server = new Server(this, ["id": e.data.fullData["guild_id"]]) }
			Map serverToAdd = this.readyData["guilds"].find { it["id"] == e.data.fullData["guild_id"] }
			Map copyServer = serverToAdd
			List members = copyServer.members.collect({ it })
			if (e.data["newUser"] != null){
				try{
					Map m = members.find { it["user"]["id"] == e.data.newUser.id }
					Map m2 = m
					m2.user = e.data.newUser.object
					serverToAdd.members.remove(m)
					serverToAdd.members.add(m2)
				}catch (ex){

				}
			}
			List presences = copyServer.presences.collect({ it })
			try{
				Map p = presences.find { it["user"]["id"] == e.data.member.id }
				Map p2 = p
				p2.status = e.data.status
				if (e.data.game == ""){
					p2.game = null
				}else{
					p2.game = [name: e.data.game]
				}
				serverToAdd.presences.remove(p)
				serverToAdd.presences.add(p)
			}catch (ex){

			}
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.id) }
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(serverToAdd)
		})
	}
}
