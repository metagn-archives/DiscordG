package hlaaftana.discordg.net

import java.util.concurrent.*
import java.util.zip.InflaterInputStream

import groovy.json.JsonOutput

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.api.extensions.Frame

import java.util.Map

import hlaaftana.discordg.util.*
import static hlaaftana.discordg.util.WhatIs.whatis
import hlaaftana.discordg.*
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.logic.EventData
import hlaaftana.discordg.objects.*

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
		if (!justReconnected) identify()
		client.log.info "Sent identify packet.", client.log.name + "WS"
		latch.countDown()
	}

	void onWebSocketText(String text){
		Closure h = {
			long receiveTime = System.currentTimeMillis()
			Map content = JSONUtil.parse(text)
			int op = content["op"]
			if (op){
				if (messageCounts[op]) messageCounts[op]++
				else messageCounts[op] = 1
			}
			if (op == 0){
				String type = content["t"]
				if (messageCounts[op]){
					if (messageCounts[op][type] != null) messageCounts[op][type]++
					else messageCounts[op][type] = 1
				}else{
					messageCounts[op] = [(type): 1]
				}
				seq = content["s"]
				if (!(type in Client.knownDiscordEvents)){
					File file = new File("dumps/${type}_${System.currentTimeMillis()}.json")
					new File("dumps").mkdir()
					JSONUtil.dump(file, content.d)
					client.log.warn "Unhandled websocket message: $type. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + "WS"
				}
				if (!careAbout(type)) return
				Map data = content["d"]
				if (type == "READY"){
					readyingState = LoadState.LOADING
					Long heartbeat = data["heartbeat_interval"]
					if (!keepAliveThread){
						if (client.gatewayVersion >= 5){
							while (!keepAliveThread);
						}
						else threadKeepAlive(heartbeat)
					}
					if (!messageCounts[7]){
						cachingState = LoadState.LOADING
						guildCreate = client.confirmedBot || data.user.bot || data.guilds.size() >= 100
						if (guildCreate){
							client.addListener("initial guild create"){ Map d ->
								Map server = d.server.object
								if (client.cache["guilds"].any { k, v -> k == server["id"] }){
									client.cache["guilds"][server["id"]] << server
								}else{
									client.cache["guilds"].add(server)
								}
							}
						}
						client.cache << data
						client.object = data.user
						client.cache["ready_data"] = data
						DiscordListCache guilds = new DiscordListCache(data.guilds.collect { guildCreate ? it : Server.construct(client, it) }, client, Server)
						client.cache["guilds"] = guilds
						DiscordListCache privateChannels = new DiscordListCache(data["private_channels"].each { Channel.construct(client, it) }, client, Channel)
						client.cache["private_channels"] = privateChannels
						DiscordListCache presences = new DiscordListCache(data["presences"].collect { it + [id: it.user.id] }, client, Presence)
						client.cache["presences"] = presences
						if (data.relationships){
							DiscordListCache relationships = new DiscordListCache(data["relationships"].collect { it + [id: it.user.id] }, client, Relationship)
							client.cache["relationships"] = relationships
						}
						if (data.user_guild_settings){
							client.cache["user_guild_settings"] = data["user_guild_settings"].collectEntries { [(it.guild_id): it] }
						}
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
								return
							}
						}
						guildCreatingState = LoadState.LOADED
					}
					loaded = true
					while (!client.loaded);
					dispatch = true
					readyingState = LoadState.LOADED
					client.log.info "Done loading."
				}
				if (type == "MESSAGE_CREATE" && data.channel_id in client.mutedChannels)
					return
				while (!canDispatch(type));
				EventData eventData = new EventData(type, [:])
				whatis(type){
					when("RESUMED"){
						client.log.info "Successfully resumed."
						client.cache << data
					}
					when("HEARTBEAT_ACK"){
						unackedHeartbeats--
					}
					when("MESSAGE_DELETE_BULK"){
						if (client.spreadBulkDelete){
							data.ids.each {
								onWebSocketText(JSONUtil.json([
									op: 0,
									t: "MESSAGE_DELETE",
									s: seq,
									d: [
										channel_id: data.channel_id,
										id: it,
										bulk: true
									]
								]))
							}
						}
					}
				}
				try{
					if (client.eventDataCalls.containsKey(type))
						eventData = EventData.create(type,
							client.eventDataCalls[type].curry(client, data))
					else
						eventData << data
				}catch (ex){
					ex.printStackTrace()
					client.log.info "Ignoring exception from $type object registering", client.log.name + "WS"
				}
				eventData["rawType"] = type
				eventData["timeReceived"] = receiveTime
				eventData["seq"] = content.s
				if (type != "READY" || client.copyReady) eventData["json"] = data
				if (eventData["server"]) eventData["guild"] = eventData["server"]
				Map event = eventData.clone()
				Thread.start("$type-${messageCounts[op][type]}"){
					client.dispatchEvent(type == "GUILD_CREATE" && guildCreatingState ==
						LoadState.LOADING ? "INITIAL_GUILD_CREATE" : type, event) }
				Thread.start("ALL-${messageCounts[op].values().sum()}"){
					client.dispatchEvent("ALL", event) }
			}else if (op == 1){
				seq = content.s
			}else if (op == 7){
				reconnect()
			}else if (op == 9){
				seq = 0
				heartbeats = 0
				identify()
			}else if (op == 10){
				client.cache << content.d
				if (justReconnected){
					dispatch = true
					justReconnected = false
					client.log.info "Successfully reconnected. Resuming events..."
					resume()
				}
				threadKeepAlive(client.cache.heartbeat_interval)
			}else if (op == 11){
				unackedHeartbeats--
			}else{
				File file = new File("dumps/op_${op}_${System.currentTimeMillis()}.json")
				new File("dumps").mkdir()
				JSONUtil.dump(file, content)
				client.log.warn "Unsupported OP code $op. Report to me, preferrably with the data in $file.absolutePath.", client.log.name + "WS"
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
		client.log.info "Connection closed. Reason: $reason, code: $code", client.log.name + "WS"
		Thread.start { client.dispatchEvent("CLOSE", [code: code, reason: reason, json: [code: code, reason: reason]]) }
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
		String ass = Client.parseEvent(event)
		if (ass == "READY") return true
		dispatch ||
			(guildCreate &&
				ass == "GUILD_CREATE" &&
				cachingState &&
				guildCreatingState == LoadState.LOADING)
	}

	boolean careAbout(event){
		boolean aa = true
		String ass = Client.parseEvent(event)
		if (ass in ["READY", "GUILD_MEMBERS_CHUNK", "GUILD_SYNC"]) return true
		if (ass == "GUILD_CREATE" && (guildCreate || client.bot)) return true
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
			] + client.extraIdentifyData
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


