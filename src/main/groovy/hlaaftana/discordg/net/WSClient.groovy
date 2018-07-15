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
	Thread heartbeatThread
	ExecutorService threadPool
	Map<Integer, Integer> opCounts = [:]
	Map<String, Integer> eventCounts = [:]
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

	WSClient(Client client) {
		this.client = client
		threadPool = Executors.newFixedThreadPool(client.threadPoolSize)
	}

	void onWebSocketConnect(Session session) {
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

	void onText(String text) {
		def receiveTime = System.currentTimeMillis()
		def content = (Map) JSONUtil.parse(text)
		def op = (int) content.op
		def opCount = opCounts[op]
		opCounts[op] = null == opCount ? 1 : ++opCount
		if (op == 0) {
			def type = (String) content.t
			def count = eventCounts[type]
			eventCounts[type] = null == count ? 1 : ++count
			seq = (int) content.s
			if (!(type in Client.knownDiscordEvents)){
				File file = new File("dumps/${type}_${System.currentTimeMillis()}.json")
				new File('dumps').mkdir()
				JSONUtil.dump(file, content.d)
				client.log.warn "Unhandled websocket message: $type. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + 'WS'
			}
			if (!careAbout(type)) return
			Map data
			try {
				data = (Map<String, Object>) content.d
			} catch (ClassCastException ignored) {
				data = [:]
			}
			if (type == 'READY') {
				readyingState = LoadState.LOADING
				if (!opCounts[7]) {
					cachingState = LoadState.LOADING
					guildCreate = client.confirmedBot || ((Map<String, Object>) data.user).bot ||
							((List) data.guilds).size() >= 100
					if (guildCreate) {
						client.addListener new DiscordRawWSListener() { void fire(String t, Map d) {
							if (t != 'INITIAL_GUILD_CREATE') return
							def id = Snowflake.swornString(d.id)
							def gc = client.guildCache[id] ?: (client.guildCache[id] = new Guild(client))
							gc.fill(d)
							gc.unavailable = false
						} }
					}
					client.fill(client.userObject = (Map<String, Object>) data.user)
					client.sessionId = (String) data.session_id
					if (client.copyReady) client.readyData = new Cache<>(data, client)

					def gl = (List<Map<String, Object>>) data.guilds
					def ga = new ArrayList<Guild>(gl.size())
					for (g in gl) ga.add(new Guild(client, g))
					client.guildCache = new DiscordListCache(ga, client)

					def pcl = (List<Map<String, Object>>) data.private_channels
					def pca = new ArrayList<Channel>(pcl.size())
					for (pc in pcl) pca.add(new Channel(client, pc))
					client.privateChannelCache = new DiscordListCache(pca, client)

					def pl = (List<Map<String, Object>>) data.presences
					def pa = new ArrayList<Presence>(pl.size())
					for (p in pl) pa.add(new Presence(client, p))
					client.presenceCache = new DiscordListCache(pa, client)

					def r = data.relationships
					if (r) {
						def mr = (List<Map<String, Object>>) r
						def x = new ArrayList<Relationship>(mr.size())
						for (a in mr) x.add(new Relationship(client, a))
						client.relationshipCache = new DiscordListCache<>(x, client)
					}

					def ugs = data.user_guild_settings
					if (ugs) {
						def mugs = (List<Map<String, Object>>) ugs
						def x = new HashMap<Snowflake, Map<String, Object>>(mugs.size())
						for (a in mugs) x.put(Snowflake.swornString(a.guild_id), a)
						client.userGuildSettingCache = new Cache<>(x, client)
					}

					cachingState = LoadState.LOADED
				}
				if (guildCreate) {
					client.log.info 'Waiting for guilds.', client.log.name + 'WS'
					long ass = System.currentTimeMillis()
					while (client.anyUnavailableGuilds()) {
						if (System.currentTimeMillis() - ass >= client.guildTimeout) {
							client.log.warn 'Guild timeout exceeded. Logging out.'
							client.logout()
							return
						}
					}
				}
				guildCreatingState = LoadState.LOADED
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
			} else if (type == 'HEARTBEAT_ACK') {
				unackedHeartbeats--
			} else if (type == 'MESSAGE_DELETE_BULK' && client.spreadBulkDelete) {
				for (i in data.ids) {
					onWebSocketText(/{"op":0,"s":$seq,"t":"MESSAGE_DELETE","d":{/ +
							/"channel_id":$data.channel_id,"from":$i,"bulk":true}}/)
				}
			}
			boolean guildCreateInitial = false
			if (type == 'CHANNEL_CREATE') thruChannel(data)
			else if (type == 'GUILD_CREATE') {
				guildCreateInitial = client.guildCache.containsKey(data.id)
				thruGuild(data)
			}
			for (l in client.rawListeners) l.fire(type, data)
			if (!client.listenerSystem.listeners[type]) return
			data.rawType = type
			data.timeReceived = receiveTime
			data.seq = content.s
			if (data.channel_id) data.channel = client.channel(data.channel_id)
			if (data.guild_id) data.guild = client.guild(data.guild_id)
			if (data.user_id) data.user = client.user(data.user_id)
			if (data.message_id) data.message = ((Channel) data.channel).message(data.message_id)
			if (data.user) {
				data.user = client.user(data.user) ?: new User(client, (Map) data.user)
				if (type.contains('MEMBER') && data.guild_id) data.member = ((Guild) data.guild).member(data.user)
			}
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
			if (type.startsWith('PRESENCE_')) data.presence = ((Guild) data.guild).presence(data.user)
			Map<String, Object> event = new HashMap<>(data)
			Thread.start("$type-$count") { client.dispatchEvent(guildCreateInitial ? 'INITIAL_GUILD_CREATE' : type, event) }
			Thread.start("ALL-$opCount") { client.dispatchEvent('ALL', event) }
		} else if (op == 1) seq = (int) content.s
		else if (op == 7) reconnect()
		else if (op == 9) {
			seq = 0
			heartbeats = 0
			identify()
		} else if (op == 10) {
			guildCreatingState = LoadState.LOADING
			client.readyData.putAll((Map) content.d)
			if (justReconnected) {
				dispatch = true
				justReconnected = false
				client.log.info 'Successfully reconnected. Resuming events...'
				resume()
			}
			startHeartbeating((long) client.readyData.heartbeat_interval)
		} else if (op == 11) {
			unackedHeartbeats--
		} else {
			File file = new File("dumps/op_${op}_${System.currentTimeMillis()}.json")
			new File('dumps').mkdir()
			JSONUtil.dump(file, content)
			client.log.warn "Unsupported OP code $op. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + 'WS'
		}
	}

	void onWebSocketText(String text) {
		threadPool.submit {
			try {
				onText(text)
			} catch (ex) {
				ex.printStackTrace()
			}
		}
	}

	void onWebSocketBinary(byte[] payload, int offset, int len) {
		onWebSocketText(new InflaterInputStream(
			new ByteArrayInputStream(payload, offset, len)).text)
	}

	void onWebSocketClose(int code, String reason) {
		client.log.info "Connection closed. Reason: $reason, code: $code", client.log.name + 'WS'
		Thread.start { client.dispatchEvent('CLOSE', new HashMap<String, Object>(
				code: code, reason: reason, json: [code: code, reason: reason])) }
		if (heartbeatThread) {
			heartbeatThread.interrupt()
			heartbeatThread = null
		}
		this.session.close()
		Thread.currentThread().interrupt()
	}

	void onWebSocketError(Throwable t) {
		client.log.error "Websocket connection errored:"
		t.printStackTrace()
	}

	void reconnect(boolean requestGateway = false) {
		dispatch = false
		try { client.closeGateway() } catch (ignored) {}
		while (++reconnectTries) {
			if (reconnectTries > 5) {
				client.log.info 'Failed reconnect. Logging out.', client.log.name + 'WS'
				reconnectTries = 0
				try { client.logout() } catch (ignored) {}
				return
			}
			try {
				client.connectGateway(requestGateway || reconnectTries > 3)
				reconnectTries = 0
				justReconnected = true
				return
			} catch (ignored) {
				client.log.debug "Reconnect $reconnectTries failed", client.log.name + 'WS'
			}
		}
	}

	boolean canDispatch(event) {
		String ass = Client.parseEvent(event)
		if (ass == 'READY') return true
		(dispatch ||
			(guildCreate &&
				ass == 'GUILD_CREATE' &&
				guildCreatingState == LoadState.LOADING)) && cachingState
	}

	boolean careAbout(event) {
		boolean aa = true
		String ass = Client.parseEvent(event)
		if (ass in ['READY', 'GUILD_MEMBERS_CHUNK', 'GUILD_SYNC']) return true
		if (ass == 'GUILD_CREATE' && (guildCreate || client.bot)) return true
		if (client.eventWhitelist) {
			aa = false
			aa |= ass in client.eventWhitelist.collect { Client.parseEvent(it) }
		}
		if (client.eventBlacklist) {
			aa &= !(ass in client.eventBlacklist.collect { Client.parseEvent(it) })
		}
		aa
	}

	void send(message) {
		String ass = message instanceof Map ? JSONUtil.json(message) : message
		client.askPool('wsAnything') {
			session.remote.sendString(ass)
		}
	}

	void send(Map ass, int op) {
		send op: op, d: ass
	}

	void send(int op, Map ass) {
		send ass, op
	}

	void identify() {
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

	def resume() {
		send op: 6, d: [
			token: client.token,
			session_id: client.sessionId,
			seq: seq
		]
	}

	void heartbeat() {
		heartbeats++
		unackedHeartbeats++
		send op: 1, d: seq
	}

	void startHeartbeating(long interval) {
		heartbeatThread = Thread.startDaemon {
			while (true) {
				heartbeat()
				try { Thread.sleep(interval) }
				catch (InterruptedException ignored) { break }
			}
		}
	}

	enum LoadState {
		NOT_LOADED, LOADING, LOADED

		boolean asBoolean() { this == LOADED }
	}


	Map thruChannel(Map c, String guildId = null) {
		if (guildId) c.guild_id = guildId
		if (c.guild_id) {
			def po = c.permission_overwrites
			if (po instanceof List<Map<String, Object>>) {
				def a = new ArrayList<PermissionOverwrite>(po.size())
				for (x in po) {
					def j = new PermissionOverwrite(client, x)
					j.channelId = Snowflake.swornString(c.id)
					a.add(j)
				}
				c.permission_overwrites = new DiscordListCache<PermissionOverwrite>(a, client)
			}
		} else if (c.recipients != null) {
			c.recipients = new DiscordListCache<User>(((List<Map>) c.recipients).collect { new User(client, this) }, client)
		}
		c
	}

	Map thruGuild(Map g) {
		def gid = (String) g.id

		def m = (List<Map>) g.members, ml = new ArrayList<Member>(m.size())
		for (mo in m) {
			def a = new HashMap(mo)
			a.put('guild_id', gid)
			a.putAll((Map) a.user)
			ml.add(new Member(client, a))
		}
		g.members = new DiscordListCache(ml, client)

		def p = (List<Map>) g.presences, pl = new ArrayList<Presence>(p.size())
		for (po in p) {
			def a = new HashMap(po)
			a.put('guild_id', gid)
			a.putAll((Map) a.user)
			pl.add(new Presence(client, a))
		}
		g.presences = new DiscordListCache(pl, client)

		def e = (List<Map>) g.emojis, el = new ArrayList<Emoji>(e.size())
		for (eo in e) {
			def a = new HashMap(eo)
			a.put('guild_id', gid)
			el.add(new Emoji(client, a))
		}
		g.emojis = new DiscordListCache(el, client)

		def r = (List<Map>) g.roles, rl = new ArrayList<Role>(r.size())
		for (ro in r) {
			def a = new HashMap(ro)
			a.put('guild_id', gid)
			rl.add(new Role(client, a))
		}
		g.roles = new DiscordListCache(rl, client)

		def c = (List<Map>) g.channels, cl = new ArrayList<Channel>(c.size())
		for (co in c) cl.add(new Channel(client, thruChannel(new HashMap(co), gid)))
		g.channels = new DiscordListCache(cl, client)

		def vs = (List<Map>) g.voice_states, vsl = new ArrayList<VoiceState>(vs.size())
		for (vso in vs) {
			def a = new HashMap(vso)
			a.put('guild_id', gid)
			a.put('id', (String) a.user_id)
			vsl.add(new VoiceState(client, a))
		}
		g.voice_states = new DiscordListCache(vsl, client)

		g
	}
}


