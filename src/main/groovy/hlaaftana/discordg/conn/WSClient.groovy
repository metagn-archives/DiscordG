package hlaaftana.discordg.conn

import java.util.concurrent.*
import java.util.zip.Inflater

import groovy.json.JsonOutput

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.api.extensions.Frame

import java.util.Map

import hlaaftana.discordg.util.*
import hlaaftana.discordg.oauth.BotClient
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.objects.Server.VoiceState

/**
 * The websocket client for the API.
 * @author Hlaaftana
 */
@WebSocket
class WSClient{
	CountDownLatch latch = new CountDownLatch(1)
	Client client
	Session session
	Thread keepAliveThread
	ExecutorService threadPool
	int seq
	int heartbeats

	// believe me this works
	boolean dispatch
	boolean guildCreating
	boolean initialGuildCreates
	boolean waitingForReady
	boolean readying
	boolean readied
	boolean loaded

	WSClient(Client client){
		this.client = client
		this.initialGuildCreates = client instanceof BotClient
		threadPool = Executors.newFixedThreadPool(client.eventThreadCount)
	}

	@OnWebSocketConnect
	void onConnect(Session session){
		Log.info "Connected to gateway."
		this.session = session
		this.session.policy.maxTextMessageSize = Integer.MAX_VALUE
		this.session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		identify()
		Log.info "Sent identify packet."
		waitingForReady = true
		latch.countDown()
	}

	@OnWebSocketMessage
	void onMessage(Session session, String message){
		threadPool.submit({
			try{
				Map content = JSONUtil.parse(message)
				int op = content["op"]
				if (op == 0){
					String type = content["t"]
					this.seq = content["s"]
					if (!careAbout(type)) return
					Map data = content["d"]
					if (type.equals("READY")){
						waitingForReady = false
						readying = true
						long heartbeat = data["heartbeat_interval"]
						try{
							keepAliveThread = new Thread({
								while (true){
									this.send(op: 1, d: seq)
									try{ Thread.sleep(heartbeat) }catch (InterruptedException ex){ return }
								}
							})
							keepAliveThread.daemon = true
							keepAliveThread.start()
						}catch (e){
							e.printStackTrace()
							System.exit(0)
						}
						client.cache["ready_data"] = data
						DiscordListCache guilds = new DiscordListCache(data.guilds, client, Server)
						guilds.mapList.each { Map g ->
							if (g["unavailable"]) return
							g.members.each { Map m ->
								m["guild_id"] = g["id"]
								m << m["user"]
							}
							g["members"] = new DiscordListCache(g["members"], client, Member)
							g.presences.each { Map p ->
								p["guild_id"] = g["id"]
								p << p["user"]
							}
							g["presences"] = new DiscordListCache(g["presences"], client, Presence)
							g.channels.each { Map c ->
								c["guild_id"] = g["id"]
								if (c["type"] == "text"){
									c["cached_messages"] = []
								}
								c["permission_overwrites"].each { Map po ->
									po["channel_id"] = c["id"]
								}
								c["permission_overwrites"] = new DiscordListCache(c["permission_overwrites"], client, Channel.PermissionOverwrite)
							}
							g["channels"] = new ChannelListCache(g["channels"], client)
							g.emojis.each { Map e ->
								e["guild_id"] = g["id"]
							}
							g["emojis"] = new DiscordListCache(g["emojis"], client, Emoji)
							g.roles.each { Map e ->
								e["guild_id"] = g["id"]
							}
							g["roles"] = new DiscordListCache(g["roles"], client, Role)
							g.presences.each { Map p ->
								p["guild_id"] = g["id"]
							}
							g["presences"] = new DiscordListCache(g["presences"], client, Presence)
						}
						client.cache["guilds"] = guilds
						DiscordListCache privateChannels = new DiscordListCache(data["private_channels"], client, PrivateChannel)
						privateChannels.mapList.each { Map pc ->
							pc["cached_messages"] = []
						}
						client.cache["private_channels"] = privateChannels
						data.each { k, v ->
							if (!(k in client.cache.keySet())){
								client.cache[k] = v
							}
						}
						readying = false
						if (client instanceof BotClient){
							Log.info "Waiting for servers."
							guildCreating = true
							while (client.servers.any { it.unavailable }){}
							guildCreating = false
							loaded = true
						}else{
							loaded = true
						}
						while (!client.loaded){}
						dispatch = true
						readied = true
						Log.info "Done loading."
					}
					if (!dispatch){
						if (!guildCreating){
							if (type == "GUILD_CREATE"){
								while (!guildCreating){}
							}else{
								while (!dispatch){}
							}
						}else{
							if (type != "GUILD_CREATE"){
								while (!dispatch){}
							}
						}
					}
					Map eventData = [:]
					Closure t = { String ty -> return ty == type }
					// i removed the switch here because it was slow
					try{
						if (t("READY") || t("USER_GUILD_SETTINGS_UPDATE") || t("USER_SETTINGS_UPDATE") || t("MESSAGE_ACK")){
							eventData = data
						}else if (t("HEARTBEAT_ACK")){
							heartbeats++
						}else if (t("CHANNEL_CREATE") || t("CHANNEL_DELETE") || t("CHANNEL_UPDATE")){
							if (!data.containsKey("guild_id")){
								eventData = [
									server: null,
									guild: null,
									channel: new PrivateChannel(client, data)
									]
							}else if (data["type"].equals("text")){
								eventData = [
									server: client.getServerById(data["guild_id"]),
									guild: client.getServerById(data["guild_id"]),
									channel: new TextChannel(client, data)
									]
							}else if (data["type"].equals("voice")){
								eventData = [
									server: client.getServerById(data["guild_id"]),
									guild: client.getServerById(data["guild_id"]),
									channel: new VoiceChannel(client, data)
									]
							}
						}else if (t("GUILD_BAN_ADD") || t("GUILD_BAN_REMOVE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								user: new User(client, data["user"])
								]
						}else if (t("GUILD_CREATE")){
							eventData = [
								server: new Server(client, data),
								guild: new Server(client, data)
								]
						}else if (t("GUILD_DELETE")){
							eventData = [
								server: client.getServerById(data["id"]),
								guild: client.getServerById(data["id"])
								]
						}else if (t("GUILD_INTEGRATIONS_UPDATE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"])
								]
						}else if (t("GUILD_EMOJIS_UPDATE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								emojis: new DiscordListCache(data["emojis"].collect { it + ["guild_id": data["guild_id"]] }, client, Emoji).list
								]
						}else if (t("GUILD_MEMBER_ADD") || t("GUILD_MEMBER_UPDATE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								member: new Member(client, data)
								]
						}else if (t("GUILD_MEMBER_REMOVE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								member: { -> try{
									return client.memberMap[data["guild_id"]][data["user"]["id"]]
								}catch (ex){
									return data["user"]
								} }()
							]
						}else if (t("GUILD_ROLE_CREATE") || t("GUILD_ROLE_UPDATE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								role: new Role(client, data["role"])
								]
						}else if (t("GUILD_ROLE_DELETE")){
							eventData = [
								server: client.getServerById(data["guild_id"]),
								guild: client.getServerById(data["guild_id"]),
								role: client.serverMap[data["guild_id"]].roleMap[data["role_id"]]
								]
						}else if (t("GUILD_UPDATE")){
							eventData = [
								server: new Server(client, data),
								guild: new Server(client, data)
								]
						}else if (t("MESSAGE_CREATE")){
							Message messageO = new Message(client, data)
							if (messageO.channel == null){
								dispatch && (dispatch = false)
								return
							}else{
								dispatch || (dispatch = true)
							}
							eventData = [
								message: messageO,
								sendMessage: messageO.channel.&sendMessage,
								sendFile: messageO.channel.&sendFile,
								author: { try{ messageO.channel?.server?.members?.find { try{ it?.id == messageO.author.id }catch (ex){ false } } }catch (ex){ null } }() ?: messageO.author
								]
						}else if (t("MESSAGE_DELETE")){
							eventData = [
								channel: client.textChannelMap[data["channel_id"]],
								message: data["id"]
								]
						}else if (t("MESSAGE_UPDATE")){
							if (data.containsKey("content")){
								eventData = [
									message: new Message(client, data)
									]
							}else{
								eventData = [
									channel: client.getTextChannelById(data["channel_id"]),
									message: data["id"],
									embeds: data["embeds"]
									]
							}
						}else if (t("PRESENCE_UPDATE")){
							eventData = [
								server: client.servers.find { it.id == data["guild_id"] },
								guild: client.servers.find { it.id == data["guild_id"] },
								member: { ->
									try{
										return client.members.find { it.id == data["user"]["id"] && it.server.id == client.servers.find { it.id == data["guild_id"] }.id } ?: client.members.find { it.id == data["user"]["id"] }
									}catch (ex){
										return null
									}
								}(),
								game: (data["game"] != null) ? data["game"]["name"] : "",
								status: data["status"]
							]
						}else if (t("TYPING_START")){
							eventData = [
								channel: client.getTextChannelById(data["channel_id"]),
								user: client.getTextChannelById(data["channel_id"]).server.members.find { it.id == data["user_id"] }
								]
						}else if (t("VOICE_STATE_UPDATE")){
							eventData = [
								voiceState: new VoiceState(client, data)
								]
							if (data["user_id"] == client.user.id){
								if (client.voiceWsClient != null){ // voice connected
									client.voiceData.channel = client.getVoiceChannelById(data["channel_id"])
								}
								client.voiceData.session_id = data["session_id"]
							}
						}else if (t("VOICE_SERVER_UPDATE")){
							client.voiceData << data
							eventData = data
						}else if (t("USER_UPDATE")){
							eventData = [
								user: new User(client, data)
							]
						}else{
							eventData = data
							String fileName = "dumps/${type}_${System.currentTimeMillis()}.json"
							Log.info "Unhandled websocket message: $type. Report to me, preferrably with the data in $fileName."
							File dump = new File(fileName)
							new File("dumps").mkdirs()
							dump.createNewFile()
							dump.write(JsonOutput.prettyPrint(JsonOutput.toJson(data)))
						}
					}catch (ex){
						if (Log.enableEventRegisteringCrashes) ex.printStackTrace()
						Log.info "Ignoring exception from $type object registering"
					}
					if (!t("READY") || client.copyReady) eventData["fullData"] = data
					Map event = eventData
					if (dispatch || type == "READY" || (guildCreating && type == "GUILD_CREATE")){
						Thread.start { client.dispatchEvent(type, event) }
						Thread.start { client.dispatchEvent("ALL", event) }
					}
				}else if (op == 9){
					seq = 0
					heartbeats = 0
					identify()
				}else{
					Log.warn "Unsupported OP code ${op}. Report to me."
				}
			}catch (ex){
				ex.printStackTrace()
			}
		} as Callable)
	}

	@OnWebSocketClose
	void onClose(Session session, int code, String reason){
		Log.info "Connection closed. Reason: $reason, code: $code"
		if (keepAliveThread){
			keepAliveThread.interrupt()
			keepAliveThread = null
		}
		this.session.close()
		Thread.currentThread().interrupt()
	}

	@OnWebSocketError
	void onError(Throwable t){
		t.printStackTrace()
	}

	boolean careAbout(event){
		boolean aa = true
		if (client.includedEvents){
			aa = false
			aa |= Events.get(event).type in client.includedEvents.collect { Events.get(it).type }
		}
		if (client.excludedEvents){
			aa &= !(Events.get(event).type in client.excludedEvents.collect { Events.get(it).type })
		}
		return aa
	}

	/**
	 * Converts an object to a string then sends it as a websocket message. Converts Maps to JSON strings.
	 * @param message - the object to convert to a string.
	 */
	void send(message){
		try{
			if (message instanceof Map)
				session.remote.sendString(JSONUtil.json(message))
			else
				session.remote.sendString(message.toString())
		}catch (e){
			e.printStackTrace()
		}
	}

	void identify(){
		Map a = [
			op: 2,
			d: [
				token: client.token,
				v: 4,
				large_threshold: client.largeThreshold,
				properties: [
					$os: System.getProperty("os.name"),
					$browser: "DiscordG",
					$device: "Groovy",
					$referrer: "https://discordapp.com/@me",
					$referring_domain: "discordapp.com",
				],
			],
		]
		if (client.shardTuple) a.shard = client.shardTuple
		this.send(a)
	}
}


