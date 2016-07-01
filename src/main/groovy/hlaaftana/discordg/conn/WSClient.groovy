package hlaaftana.discordg.conn

import java.util.concurrent.*
import java.util.zip.InflaterInputStream

import groovy.json.JsonOutput

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.api.extensions.Frame

import java.util.Map

import hlaaftana.discordg.util.*
import hlaaftana.discordg.*
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.objects.Server.VoiceState

/**
 * The websocket client for the API.
 * @author Hlaaftana
 */
class WSClient extends WebSocketAdapter {
	CountDownLatch latch = new CountDownLatch(1)
	Client client
	Session session
	Thread keepAliveThread
	ThreadPoolExecutor threadPool
	Map messageCounts = [:]
	int seq
	int heartbeats
	int unackedHeartbeats
	List heartbeatTimes
	int reconnectTries
	boolean justReconnected

	LoadState cachingState = LoadState.NOT_LOADED
	boolean guildCreate
	LoadState guildCreatingState = LoadState.NOT_LOADED
	LoadState readyingState = LoadState.NOT_LOADED
	boolean dispatch
	boolean loaded

	WSClient(Client client){
		this.client = client
		threadPool = Executors.newFixedThreadPool(client.eventThreadCount)
	}

	void onWebSocketConnect(Session session){
		client.log.info "Connected to gateway.", client.log.name + "WS"
		this.session = session
		this.session.policy.maxTextMessageSize = Integer.MAX_VALUE
		this.session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		if (justReconnected){
			resume()
			justReconnected = false
		}
		else identify()
		client.log.info "Sent identify packet.", client.log.name + "WS"
		latch.countDown()
	}

	void onWebSocketText(String text){
		threadPool.submit {
			try{
				Map content = JSONUtil.parse(text)
				int op = content["op"]
				if (op){
					if (messageCounts[op]) messageCounts[op]++
					else messageCounts[op] = 1
				}
				if (op == 0){
					String type = content["t"]
					if (messageCounts[op]){
						if (messageCounts[op][type]) messageCounts[op][type]++
						else messageCounts[op][type] = 1
					}else{
						messageCounts[op] = [(type): 1]
					}
					this.seq = content["s"]
					if (!careAbout(type)) return
					Map data = content["d"]
					if (type.equals("READY")){
						readyingState = LoadState.LOADING
						Long heartbeat = data["heartbeat_interval"]
						if (!keepAliveThread){
							if (client.gatewayVersion == 5){
								while (!keepAliveThread){}
							}
							else threadKeepAlive(heartbeat)
						}
						if (!messageCounts[7]){
							cachingState = LoadState.LOADING
							guildCreate = client.confirmedBot || data.user.bot || data.guilds.size() >= 100
							if (guildCreate){
								client.addListener(Events.INITIAL_GUILD_CREATE){ Map d ->
									Map server = d.server.object
									if (client.cache["guilds"].any { k, v -> k == server["id"] }){
										client.cache["guilds"][server["id"]] << server
									}else{
										client.cache["guilds"].add(server)
									}
								}
							}
							client.cache << data
							client.cache["ready_data"] = data
							DiscordListCache guilds = new DiscordListCache(data.guilds.collect { guildCreate ? it : Server.construct(client, it) }, client, Server)
							client.cache["guilds"] = guilds
							DiscordListCache privateChannels = new DiscordListCache(data["private_channels"], client, PrivateChannel)
							client.cache["private_channels"] = privateChannels
							cachingState = LoadState.LOADED
						}
						if (guildCreate){
							client.log.info "Waiting for servers.", client.log.name + "WS"
							guildCreatingState = LoadState.LOADING
							long ass = System.currentTimeMillis()
							while (client.servers.any { it.unavailable }){
								if (System.currentTimeMillis() - ass >= client.serverTimeout){
									client.log.warn "Server timeout exceeded. Logging out."
									client.logout()
								}
							}
							guildCreatingState = LoadState.LOADED
							loaded = true
						}else{
							loaded = true
						}
						while (!client.loaded){}
						dispatch = true
						readyingState = LoadState.LOADED
						client.log.info "Done loading."
					}
					while (!canDispatch(type));
					EventData eventData = new EventData(Events.get(type), [:])
					Closure edm = EventData.&create.curry(type)
					Closure t = { String ty -> ty == type }
					// i removed the switch here because it was slow
					try{
						if (t("READY") || t("USER_GUILD_SETTINGS_UPDATE") ||
							t("USER_SETTINGS_UPDATE") || t("MESSAGE_ACK") ||
							t("GUILD_MEMBERS_CHUNK") || t("CHANNEL_PINS_UPDATE") ||
							t("CHANNEL_PINS_ACK")){
							eventData << data
						}else if (t("HEARTBEAT_ACK")){
							heartbeats++
						}else if (t("CHANNEL_CREATE") || t("CHANNEL_DELETE") || t("CHANNEL_UPDATE")){
							Map c = data
							c = Channel.construct(client, c)
							if (!data.containsKey("guild_id")){
								eventData = edm {
									server { null }
									channel { new PrivateChannel(client, c) }
								}
							}else{
								Channel ass = Channel.typed(new Channel(client, c))
								eventData = edm {
									server { ass.server }
									channel { ass }
								}
							}
						}else if (t("GUILD_BAN_ADD") || t("GUILD_BAN_REMOVE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								user { new User(client, data["user"]) }
							}
						}else if (t("GUILD_CREATE")){
							Map s = data
							s = Server.construct(client, s)
							eventData = edm {
								server { new Server(client, s) }
							}
						}else if (t("GUILD_DELETE")){
							eventData = edm {
								server { client.server(data["id"]) }
							}
						}else if (t("GUILD_INTEGRATIONS_UPDATE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
							}
						}else if (t("GUILD_EMOJIS_UPDATE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								emojis {
									new DiscordListCache(data["emojis"]
										.collect { it + ["guild_id": data["guild_id"]] }
										, client, Emoji)
								}
							}
						}else if (t("GUILD_MEMBER_ADD") || t("GUILD_MEMBER_UPDATE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								member { new Member(client, data) }
							}
						}else if (t("GUILD_MEMBER_REMOVE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								member {
									try{
										client.memberMap[data["guild_id"]][data["user"]["id"]]
									}catch (ex){
										data["user"]
									}
								}
							}
						}else if (t("GUILD_ROLE_CREATE") || t("GUILD_ROLE_UPDATE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								role { new Role(client, data["role"]) }
							}
						}else if (t("GUILD_ROLE_DELETE")){
							eventData = edm {
								server { client.server(data["guild_id"]) }
								role { client.serverMap[data["guild_id"]].roleMap[data["role_id"]] }
							}
						}else if (t("GUILD_UPDATE")){
							Map s = data
							Map oldObject = client.server(s).object.clone()
							s = oldObject << s.with {
								remove("roles")
								remove("members")
								remove("presences")
								remove("emojis")
								remove("voice_states")
								it
							}
							eventData = edm {
								server { new Server(client, s) }
							}
						}else if (t("MESSAGE_CREATE")){
							Message messageO = new Message(client, data)
							if (messageO.channel == null){
								return
							}
							eventData = edm {
								message { messageO }
								sendMessage { messageO.channel.&sendMessage }
								sendFile { messageO.channel.&sendFile }
								author { messageO.author(true) }
								channel { messageO.channel }
							}
						}else if (t("MESSAGE_DELETE")){
							eventData = edm {
								channel { client.textChannelMap[data["channel_id"]] }
								message { client.messages[data["channel_id"]] ? client.textChannel(data["channel_id"]).cachedLogMap[data["id"]] ?: data["id"] : data["id"] }
							}
						}else if (t("MESSAGE_UPDATE")){
							if (data.containsKey("content")){
								eventData = edm {
									message { new Message(client, data) }
								}
							}else{
								eventData = edm {
									channel { client.textChannel(data["channel_id"]) }
									message { client.messages[data["channel_id"]] ? client.textChannel(data["channel_id"]).cachedLogMap[data["id"]] ?: data["id"] : data["id"] }
									embeds { data["embeds"] }
								}
							}
						}else if (t("PRESENCE_UPDATE")){
							if (data.guild_id){
								eventData = edm {
									server { client.server(data.guild_id) }
									member { client.server(data.guild_id).member(data.user) }
									game { data.game ? new Presence.Game(client, data.game) : null }
									status { data.status }
									if (data.user.discriminator){
										newUser { new User(client, data.user) }
									}
								}
							}else{
							}
						}else if (t("TYPING_START")){
							TextChannel channel = client.textChannel(data["channel_id"])
							eventData = edm {
								channel { channel }
								user { channel.private ? channel.user : channel.server.members.find { it.id == data["user_id"] } }
							}
						}else if (t("VOICE_STATE_UPDATE")){
							VoiceState ase = new VoiceState(client, data << [id: data["user_id"]])
							eventData = edm {
								voiceState { ase }
								server { ase.server }
								channel { ase.channel }
								member { ase.member }
							}
							/*if (data["user_id"] == client.user.id){
								if (client.voiceWsClient != null){ // voice connected
									client.voiceData.channel = client.getVoiceChannelById(data["channel_id"])
								}
								client.voiceData.session_id = data["session_id"]
							}*/
						}else if (t("VOICE_SERVER_UPDATE")){
							eventData = edm {
								token { data.token }
								server { client.server(data.guild_id) }
								endpoint { data.endpoint }
							}
						}else if (t("USER_UPDATE")){
							eventData = edm {
								user { new User(client, data) }
							}
						}else if (t("MESSAGE_DELETE_BULK")){
							data.ids.each {
								onWebSocketText(JSONUtil.json([
									op: 0,
									t: "MESSAGE_DELETE",
									s: seq,
									d: [
										channel_id: data.channel_id,
										id: it
									]
								]))
							}
							return
						}else{
							eventData << data
							File file = new File("dumps/${type}_${System.currentTimeMillis()}.json")
							new File("dumps").mkdir()
							JSONUtil.dump(file, data)
							client.log.warn "Unhandled websocket message: $type. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + "WS"
						}
					}catch (ex){
						if (client.enableEventRegisteringCrashes) ex.printStackTrace()
						client.log.info "Ignoring exception from $type object registering", client.log.name + "WS"
					}
					eventData["type"] = type
					if (!t("READY") || client.copyReady) eventData["fullData"] = data
					if (eventData["server"]) eventData["guild"] = eventData["server"]
					Map event = eventData
					if (type == "GUILD_CREATE" && guildCreatingState == LoadState.LOADING){
						Thread.start("$type-${messageCounts[op][type]}"){ client.dispatchEvent("INITIAL_GUILD_CREATE", event) }
					}else{
						Thread.start("$type-${messageCounts[op][type]}"){ client.dispatchEvent(type, event) }
					}
					Thread.start("ALL-${messageCounts[op].values().sum()}"){ client.dispatchEvent("ALL", event) }
				}else if (op == 7){
					reconnect()
				}else if (op == 9){
					seq = 0
					heartbeats = 0
					identify()
				}else if (op == 10){
					client.cache << content.d
					threadKeepAlive(client.cache.heartbeat_interval)
				}else if (op == 11){
					unackedHeartbeats--
				}else{
					File file = new File("dumps/op_${op}_${System.currentTimeMillis()}.json")
					new File("dumps").mkdir()
					JSONUtil.dump(file, content)
					client.log.warn "Unsupported OP code $op. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + "WS"
				}
			}catch (ex){
				ex.printStackTrace()
			}
		}
	}

	void onWebSocketBinary(byte[] payload, int offset, int len){
		onWebSocketText(new InflaterInputStream(
			new ByteArrayInputStream(payload, offset, len)).text)
	}

	void onWebSocketClose(int code, String reason){
		client.log.info "Connection closed. Reason: $reason, code: $code", client.log.name + "WS"
		Thread.start { client.dispatchEvent("CLOSE", [code: code, reason: reason, fullData: [code: code, reason: reason]]) }
		if (keepAliveThread){
			keepAliveThread.interrupt()
			keepAliveThread = null
		}
		this.session.close()
		Thread.currentThread().interrupt()
	}

	void onWebSocketError(Throwable t){
		t.printStackTrace()
	}

	void reconnect(boolean requestGateway = false){
		dispatch = false
		client.closeGateway(false)
		while (++reconnectTries){
			if (reconnectTries > 5){
				client.log.info "Failed reconnect. Logging out.", client.log.name + "WS"
				reconnectTries = 0
				client.logout()
				return
			}
			try{
				client.connectGateway(requestGateway || reconnectTries > 3, false)
				reconnectTries = 0
				justReconnected = true
				return
			}catch (ex){
				client.log.debug "Reconnect $reconnectTries failed", client.log.name + "WS"
			}
		}
	}

	boolean canDispatch(event){
		String ass = Events.get(event).type
		if (ass == "READY") return true
		dispatch ||
			(guildCreate &&
				ass == "GUILD_CREATE" &&
				cachingState &&
				guildCreatingState == LoadState.LOADING)
	}

	boolean careAbout(event){
		boolean aa = true
		String ass = Events.get(event).type
		if (ass in ["READY", "GUILD_MEMBERS_CHUNK"]) return true
		if (Events.get(event).type == "GUILD_CREATE" && (guildCreate || client.bot)) return true
		if (client.includedEvents){
			aa = false
			aa |= Events.get(event).type in client.includedEvents.collect { Events.get(it).type }
		}
		if (client.excludedEvents){
			aa &= !(Events.get(event).type in client.excludedEvents.collect { Events.get(it).type })
		}
		aa
	}

	void send(message){
		String ass = message instanceof Map ? JSONUtil.json(message) : message
		client.askPool("wsAnything"){
			session.remote.sendString(ass)
		}
	}

	void send(Map ass, int op){
		send op: op, d: ass
	}

	void send(int op, Map ass){
		send ass, op
	}

	void identify(){
		Map a = [
			op: 2,
			d: [
				token: client.token,
				large_threshold: client.largeThreshold,
				compress: true,
				properties: [
					os: System.getProperty("os.name"),
					browser: "DiscordG",
					device: "DiscordG"
				]
			]
		]
		if (client.shardTuple) a.d.shard = client.shardTuple
		this.send(a)
	}

	def resume(){
		send op: 6, d: [
			token: client.token,
			session_id: client.sessionId,
			seq: seq
		]
	}

	void keepAlive(){
		heartbeats++
		unackedHeartbeats++
		send op: 1, d: seq
	}

	void threadKeepAlive(long heartbeat){
		keepAliveThread = Thread.startDaemon {
			while (true){
				keepAlive()
				try{ Thread.sleep(heartbeat) }catch (InterruptedException ex){ return }
			}
		}
	}

	static class LoadState {
		static final LoadState NOT_LOADED = new LoadState(-1)
		static final LoadState LOADING = new LoadState(0)
		static final LoadState LOADED = new LoadState(1)

		int number
		LoadState(int number){ this.number = number }

		boolean asBoolean(){ this == LOADED }
		boolean equals(LoadState other){ this.number == other.number }
	}
}


