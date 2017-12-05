package hlaaftana.discordg.net

import groovy.transform.CompileStatic
import hlaaftana.discordg.collections.Cache

import java.util.concurrent.*
import java.util.zip.InflaterInputStream

import org.eclipse.jetty.websocket.api.*
import hlaaftana.discordg.util.*
import hlaaftana.discordg.*
import hlaaftana.discordg.collections.DiscordListCache
import hlaaftana.discordg.objects.*

/**
 * The websocket client for the API.
 * @author Hlaaftana
 */
@CompileStatic
class WSClient extends WebSocketAdapter {
	CountDownLatch latch = new CountDownLatch(1)
	Client client
	Session session
	Thread keepAliveThread
	ExecutorService threadPool
	Map<Integer, Object> messageCounts = [:]
	int seq
	int heartbeats
	int unackedHeartbeats
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
		client.log.info 'Connected to gateway.', client.log.name + 'WS'
		this.session = session
		this.session.policy.maxTextMessageSize = Integer.MAX_VALUE
		this.session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		if (!justReconnected) identify()
		client.log.info 'Sent identify packet.', client.log.name + 'WS'
		latch.countDown()
	}

	void onWebSocketText(String text){
		Closure h = {
			long receiveTime = System.currentTimeMillis()
			Map content = (Map) JSONUtil.parse(text)
			int op = (int) content.op
			if (op){
				if (messageCounts[op]) ((int) messageCounts[op])++
				else messageCounts[op] = 1
			}
			if (op == 0) {
				String type = (String) content.t
				if (messageCounts[op]){
					if (messageCounts[op][type] != null) ((int) messageCounts[op][type])++
					else messageCounts[op][type] = 1
				}else{
					messageCounts[op] = new HashMap<String, Integer>()
					((Map<String, Integer>) messageCounts[op]).put(type, 1)
				}
				seq = (int) content.s
				if (!(type in Client.knownDiscordEvents)){
					File file = new File("dumps/${type}_${System.currentTimeMillis()}.json")
					new File('dumps').mkdir()
					JSONUtil.dump(file, content.d)
					client.log.warn "Unhandled websocket message: $type. Report to me, preferrably with the data in $file.absolutePath.', client.log.name + 'WS"
				}
				if (!careAbout(type)) return
				Map data = (Map<String, Object>) content.d
				if (type == 'READY'){
					readyingState = LoadState.LOADING
					if (!messageCounts[7]){
						cachingState = LoadState.LOADING
						guildCreate = client.confirmedBot || ((Map<String, Object>) data.user).bot ||
								((List) data.guilds).size() >= 100
						if (guildCreate) {
							client.addListener new DiscordRawWSListener() { void fire(String t, Map d) {
								if (t != 'INITIAL_GUILD_CREATE') return
								d.unavailable = false
								if (client.guildCache.containsKey(d.id)) client.guildCache[(String) d.id].putAll d
								else client.guildCache.add(d)
							} }
						}
						client.userObject = client.object = (Map<String, Object>) data.user
						client.sessionId = (String) data.session_id
						if (client.copyReady) client.readyData = new Cache<>(data, client)

						def gl = (List<Map<String, Object>>) data.guilds
						def ga = new ArrayList(gl.size())
						for (g in gl) ga.add(guildCreate ? g : Guild.construct(client, g))
						client.guildCache = new DiscordListCache(ga, client, Guild)

						def pcl = (List<Map<String, Object>>) data.private_channels
						def pca = new ArrayList(pcl.size())
						for (pc in pcl) pca.add(Channel.construct(client, pc))
						client.privateChannelCache = new DiscordListCache(pca, client, Channel)

						def pl = (List<Map<String, Object>>) data.presences
						def pa = new ArrayList(pl.size())
						for (p in pl) {
							def b = new HashMap<String, Object>(p)
							b.put('id', ((Map<String, Object>) p.user).id)
							pa.add(b)
						}
						client.presenceCache = new DiscordListCache(pa, client, Presence)

						def r = data.relationships
						if (r) {
							def mr = (List<Map<String, Object>>) r
							def x = new ArrayList(mr.size())
							for (a in mr) {
								def b = new HashMap<String, Object>(a)
								b.put('id', ((Map<String, Object>) a.user).id)
								x.add(b)
							}
							client.relationshipCache = new DiscordListCache<>(x, client, Relationship)
						}

						def ugs = data.user_guild_settings
						if (ugs) {
							def mugs = (List<Map<String, Object>>) ugs
							def x = new HashMap<String, Map<String, Object>>(mugs.size())
							for (a in mugs) x.put((String) a.guild_id, a)
							client.userGuildSettingCache = new Cache<>(x, client)
						}

						cachingState = LoadState.LOADED
					}
					if (guildCreate) {
						client.log.info 'Waiting for guilds.', client.log.name + 'WS'
						guildCreatingState = LoadState.LOADING
						long ass = System.currentTimeMillis()
						while (client.anyUnavailableGuilds()) {
							if (System.currentTimeMillis() - ass >= client.guildTimeout) {
								client.log.warn 'Guild timeout exceeded. Logging out.'
								client.logout()
								return
							}
						}
						guildCreatingState = LoadState.LOADED
					}
					loaded = true
					while (!client.loaded) { Thread.sleep 10 }
					dispatch = true
					readyingState = LoadState.LOADED
					client.log.info 'Done loading.'
				}
				if (type == 'MESSAGE_CREATE' && client.mutedChannels.contains((String) data.channel_id))
					return
				while (!canDispatch(type)) { Thread.sleep 10 }
				if (type == 'RESUMED') {
					client.log.info 'Successfully resumed.'
					if (client.copyReady) client.readyData.putAll data
				} else if (type == 'HEARTBEAT_ACK'){
					unackedHeartbeats--
				} else if (type == 'MESSAGE_DELETE_BULK' && client.spreadBulkDelete){
					for (i in data.ids) {
						onWebSocketText(/{"op":0,"s":$seq,"t":"MESSAGE_DELETE","d":{/ +
								/"channel_id":$data.channel_id,"id":$i,"bulk":true}}/)
					}
				}
				if (type == 'CHANNEL_CREATE') Channel.construct(client, data)
				else if (type == 'GUILD_CREATE') Guild.construct(client, data)
				for (l in client.rawListeners) l.fire(type, data)
				if (!client.listenerSystem.listeners[type]) return
				data.rawType = type
				data.timeReceived = receiveTime
				data.seq = content.s
				if (data.channel_id) data.channel = client.channel(data.channel_id)
				if (data.guild_id) data.guild = client.guild(data.guild_id)
				if (data.user_id) data.user = client.user(data.user_id)
				if (data.message_id) data.message = ((Channel) data.channel).message(data.message_id)
				if (data.user) data.user = client.user(data.user) ?: new User(client, (Map) data.user)
				if (data.id) {
					if (type.contains('MEMBER')) data.member = ((Guild) data.guild).member(data.id)
					else if (type.contains('GUILD')) data.guild = client.guild(data.id) ?: new Guild(client, data)
					else if (type.contains('USER') && !data.user)
						data.user = client.user(data.id) ?: new User(client, data)
					else if (type.contains('CHANNEL'))
						data.channel = client.channel(data.id) ?: new Channel(client, data)
					else if (type.contains('MESSAGE'))
						data.message = ((Channel) data.channel).message(data.id, false) ?: new Message(client, data)
				}
				if (type.contains('MESSAGE')) {
					data.respond = data.sendMessage = ((Channel) data.channel).&sendMessage
					data.sendFile = ((Channel) data.channel).&sendFile
				}
				if (type.startsWith('PRESENCE')) data.presence = ((Guild) data.guild).presence(data.user)
				Map event = new HashMap(data)
				Thread.start("$type-${messageCounts[op][type]}"){
					client.dispatchEvent(type == 'GUILD_CREATE' && guildCreatingState ==
						LoadState.LOADING ? 'INITIAL_GUILD_CREATE' : type, event) }
				Thread.start("ALL-${((Map<String, Integer>) messageCounts[op]).values().sum()}"){
					client.dispatchEvent('ALL', event) }
			}
			else if (op == 1) seq = (int) content.s
			else if (op == 7) reconnect()
			else if (op == 9) {
				seq = 0
				heartbeats = 0
				identify()
			} else if (op == 10) {
				client.readyData.putAll((Map) content.d)
				if (justReconnected){
					dispatch = true
					justReconnected = false
					client.log.info 'Successfully reconnected. Resuming events...'
					resume()
				}
				threadKeepAlive((long) client.readyData.heartbeat_interval)
			}else if (op == 11){
				unackedHeartbeats--
			}else{
				File file = new File("dumps/op_${op}_${System.currentTimeMillis()}.json")
				new File('dumps').mkdir()
				JSONUtil.dump(file, content)
				client.log.warn "Unsupported OP code $op. Report to me, preferrably with the data in $file.absolutePath.', client.log.name + 'WS"
			}
		}
		threadPool.submit {
			try {
				h()
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
		client.log.info "Connection closed. Reason: $reason, code: $code", client.log.name + 'WS'
		Thread.start { client.dispatchEvent('CLOSE', [code: code, reason: reason, json: [code: code, reason: reason]]) }
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
			if (reconnectTries > 5) {
				client.log.info 'Failed reconnect. Logging out.', client.log.name + 'WS'
				reconnectTries = 0
				client.logout()
				return
			}
			try {
				client.connectGateway(requestGateway || reconnectTries > 3, false)
				reconnectTries = 0
				justReconnected = true
				return
			} catch (ignored) {
				client.log.debug "Reconnect $reconnectTries failed', client.log.name + 'WS"
			}
		}
	}

	boolean canDispatch(event){
		String ass = Client.parseEvent(event)
		if (ass == 'READY') return true
		dispatch ||
			(guildCreate &&
				ass == 'GUILD_CREATE' &&
				cachingState &&
				guildCreatingState == LoadState.LOADING)
	}

	boolean careAbout(event){
		boolean aa = true
		String ass = Client.parseEvent(event)
		if (ass in ['READY', 'GUILD_MEMBERS_CHUNK', 'GUILD_SYNC']) return true
		if (ass == 'GUILD_CREATE' && (guildCreate || client.bot)) return true
		if (client.includedEvents){
			aa = false
			aa |= ass in client.includedEvents.collect { Client.parseEvent(it) }
		}
		if (client.excludedEvents){
			aa &= !(ass in client.excludedEvents.collect { Client.parseEvent(it) })
		}
		aa
	}

	void send(message){
		String ass = message instanceof Map ? JSONUtil.json(message) : message
		client.askPool('wsAnything'){
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
		Map<String, Object> d = new HashMap<String, Object>()
		// this becomes LinkedHashMap<String, Serializable> which cannot cast to Map<String, Object> for whatever reason
		d.putAll(
			token: client.token,
			large_threshold: client.largeThreshold,
			compress: true,
			properties: [
				os: System.getProperty('os.name'),
				browser: 'DiscordG',
				device: 'DiscordG'
			]
		)
		d.putAll(client.extraIdentifyData)
		if (client.shardTuple) d.shard = client.shardTuple
		send(op: 2, d: d)
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
			while (true) {
				keepAlive()
				try { Thread.sleep(heartbeat) }
				catch (InterruptedException ignored) { break }
			}
		}
	}

	enum LoadState {
		NOT_LOADED(-1), LOADING(0), LOADED(1)

		int number
		LoadState(int number){ this.number = number }

		boolean asBoolean(){ this == LOADED }
		boolean equals(LoadState other){ this.number == other.number }
	}
}


