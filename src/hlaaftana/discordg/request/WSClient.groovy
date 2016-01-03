package hlaaftana.discordg.request

import java.util.concurrent.CountDownLatch

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*

import java.util.Map

import hlaaftana.discordg.util.JSONUtil
import hlaaftana.discordg.objects.*

@WebSocket
class WSClient{
	private CountDownLatch latch = new CountDownLatch(1)
	private API api
	private Session session
	private Thread keepAliveThread
	WSClient(API api){ this.api = api }

	@OnWebSocketConnect
	void onConnect(Session session){
		println "Connected to server."
		this.session = session
		this.session.getPolicy().setMaxTextMessageSize(Integer.MAX_VALUE)
		this.session.getPolicy().setMaxTextMessageBufferSize(Integer.MAX_VALUE)
		this.send([
			"op": 2,
			"d": [
				"token": api.getToken(),
				"v": 3,
				"properties": [
					"\$os": System.getProperty("os.name"),
					"\$browser": "DiscordG",
					"\$device": "Groovy",
					"\$referrer": "https://discordapp.com/@me",
					"\$referring_domain": "discordapp.com",
				],
			],
		])
		println "Sent API details."
		latch.countDown()
	}

	@OnWebSocketMessage
	void onMessage(Session session, String message) throws IOException{
		Map content = JSONUtil.parse(message)
		String type = content["t"]
		Map data = content["d"]
		int responseAmount = content["s"]
		if (type.equals("READY") || type.equals("RESUMED")){
			long heartbeat = data["heartbeat_interval"]
			try{
				keepAliveThread = new Thread({
					while (true){
						this.send(["op": 1, "d": System.currentTimeMillis().toString()])
						Thread.sleep(heartbeat)
					}
				})
				keepAliveThread.setDaemon(true)
				keepAliveThread.start()
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
			api.readyData = data

			//add built-in listeners
			//works
			api.addListener("guild member add", { Event e ->
				Server server = e.data.server
				Map serverInReady = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				Map serverInReady2 = serverInReady
				serverInReady2["members"].add(e.data)
				api.readyData["guilds"].remove(serverInReady)
				api.readyData["guilds"].add(serverInReady2)
			})
			//works
			api.addListener("guild member remove", { Event e ->
				Server server = e.data.server
				Member memberToRemove = e.data.member
				Map serverInReady = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				Map serverInReady2 = serverInReady
				serverInReady2["members"].remove(memberToRemove.object)
				api.readyData["guilds"].remove(serverInReady)
				api.readyData["guilds"].add(serverInReady2)
			})
			//works
			api.addListener("guild role create", { Event e ->
				Server server = e.data.server
				Map serverInReady = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				Map serverInReady2 = serverInReady
				serverInReady2["roles"].add(e.data.role.object)
				api.readyData["guilds"].remove(serverInReady)
				api.readyData["guilds"].add(serverInReady2)
			})
			//works
			api.addListener("guild role delete", { Event e ->
				Server server = e.data.server
				Role roleToRemove = e.data.role
				Map serverInReady = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				Map serverInReady2 = serverInReady
				serverInReady2["roles"].remove(roleToRemove.object)
				api.readyData["guilds"].remove(serverInReady)
				api.readyData["guilds"].add(serverInReady2)
			})
			// our (my) server objects don't read from READY to get their channels
			// however we might in the future because they did that to the
			// members request already
			// feel free to add your own listener in your code / PR
			// to make sure this works to read from READY too
			// works
			api.addListener("channel create", { Event e ->
				Server server = e.data.server
				if (server == null){
					api.readyData["private_channels"].add(e.channel.object)
				}
			})
			// works
			api.addListener("channel delete", { Event e ->
				Server server = e.data.server
				if (server == null){
					Map channelToRemove = api.readyData["private_channels"].find { it["id"].equals(e.data.channel.getID()) }
					api.readyData["private_channels"].remove(channelToRemove)
				}
			})
			// works
			api.addListener("guild create", { Event e ->
				Server server = e.data.server
				api.readyData["guilds"].add(server.object)
			})
			// works
			api.addListener("guild delete", { Event e ->
				Server server = e.data.server
				Map serverToRemove = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				api.readyData["guilds"].remove(serverToRemove)
			})
			// works
			api.addListener("guild member update", { Event e ->
				Server server = e.data.server
				Member member = e.data.member
				Map serverToRemove = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				List membersToEdit = serverToRemove["members"]
				Map memberToEdit = membersToEdit.find { it["id"].equals(member.getID()) }
				membersToEdit.remove(memberToEdit)
				membersToEdit.add(member.object)
				api.readyData["guilds"].remove(serverToRemove)
				serverToRemove["members"] = membersToEdit
				api.readyData["guilds"].add(serverToRemove)
			})
			// works
			api.addListener("guild role update", { Event e ->
				Server server = e.data.server
				Role role = e.data.role
				Map serverToRemove = api.readyData["guilds"].find { it["id"].equals(server.getID()) }
				List rolesToEdit = serverToRemove["roles"]
				Map roleToEdit = rolesToEdit.find { it["id"].equals(role.getID()) }
				rolesToEdit.remove(roleToEdit)
				rolesToEdit.add(role.object)
				api.readyData["guilds"].remove(serverToRemove)
				serverToRemove["roles"] = rolesToEdit
				api.readyData["guilds"].add(serverToRemove)
			})
			// works
			api.addListener("guild update", { Event e ->
				Server newServer = e.data.server
				Map serverToRemove = api.readyData["guilds"].find { it["id"].equals(newServer.getID()) }
				api.readyData["guilds"].remove(serverToRemove)
				api.readyData["guilds"].add(newServer.object)
			})
			println "Done loading."
		}
		Map eventData = [:]
		try{
			switch (type){
				case "READY":
					eventData = data
					break
				case "CHANNEL_CREATE":
				case "CHANNEL_DELETE":
				case "CHANNEL_UPDATE":
					if (!data.containsKey("guild_id")){
						eventData = [
							server: null,
							guild: null,
							channel: new PrivateChannel(api, data)
							]
					}else if (data["type"].equals("text")){
						eventData = [
							server: api.client.getServerById(data["guild_id"]),
							guild: api.client.getServerById(data["guild_id"]),
							channel: new TextChannel(api, data)
							]
					}else if (data["type"].equals("voice")){
						eventData = [
							server: api.client.getServerById(data["guild_id"]),
							guild: api.client.getServerById(data["guild_id"]),
							channel: new VoiceChannel(api, data)
							]
					}
					break
				case "GUILD_BAN_ADD":
				case "GUILD_BAN_REMOVE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"]),
						user: new User(api, data["user"])
						]
					break
				case "GUILD_CREATE":
					if (!data.containsKey("unavailable")){
						eventData = [
							server: new Server(api, data),
							guild: new Server(api, data)
							]
					}else{
						eventData = data
					}
					break
				case "GUILD_DELETE":
					if (!data.containsKey("unavailable")){
						eventData = [
							server: api.client.getServerById(data["id"]),
							guild: api.client.getServerById(data["id"])
							]
					}else{
						eventData = data
					}
					break
				case "GUILD_INTEGRATIONS_UPDATE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"])
						]
					break
				case "GUILD_MEMBER_ADD":
				case "GUILD_MEMBER_UPDATE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"]),
						member: new Member(api, data)
						]
					break
				case "GUILD_MEMBER_REMOVE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"]),
						member: api.client.getServerById(data["guild_id"]).getMembers().find { it.getUser().getID().equals(data["user"]["id"]) }
						]
					break
				case "GUILD_ROLE_CREATE":
				case "GUILD_ROLE_UPDATE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"]),
						role: new Role(api, data["role"])
						]
					break
				case "GUILD_ROLE_DELETE":
					eventData = [
						server: api.client.getServerById(data["guild_id"]),
						guild: api.client.getServerById(data["guild_id"]),
						role: api.client.getServerById(data["guild_id"]).getRoles().find { it.getID().equals(data["role_id"]) }
						]
					break
				case "GUILD_UPDATE":
					Map newData = data
					Server oldServer = api.client.getServerById(newData["id"])
					List<Map> memberJsons = new ArrayList<Map>()
					for (m in oldServer.getMembers()){
						memberJsons.add(m.object)
					}
					newData.put("members", memberJsons)
					eventData = [
						server: new Server(api, newData),
						guild: new Server(api, newData)
						]
					break
				case "MESSAGE_CREATE":
					eventData = [
						message: new Message(api, data)
						]
					break
				case "MESSAGE_DELETE":
					List<TextChannel> channels = new ArrayList<TextChannel>()
					for (s in api.client.getServers()) channels.addAll(s.getTextChannels())
					channels.addAll(api.client.getPrivateChannels())
					eventData = [
						channel: channels.find { it.getID().equals(data["channel_id"]) },
						messageID: data["id"]
						]
					break
				case "MESSAGE_UPDATE":
					if (data.containsKey("content")){
						eventData = [
							message: new Message(api, data)
							]
					}else{
						eventData = [
							channel: api.client.getTextChannelById(data["channel_id"]),
							messageID: data["id"],
							embeds: data["embeds"]
							]
					}
					break
				// this is acting weird
				/*case "PRESENCE_UPDATE":
					eventData = [
						member: api.client.getServerById(data["guild_id"]).getMembers().find { it.getUser().getID().equals(data["user"]["id"]) },
						//game: data["game"]["name"],
						status: data["status"]
						]
					break*/
				case "TYPING_START":
					eventData = [
						channel: api.client.getTextChannelById(data["channel_id"]),
						user: api.client.getUserById(data["user_id"])
						]
					break
				default:
					eventData = data
					break
			}
		}catch (ex){
			ex.printStackTrace()
			println "Ignoring exception from event object registering"
		}
		eventData.put("fullData", data)
		Event event = new Event(eventData, type)
		if (api.isLoaded()){
			api.listeners.each { Map.Entry<String, Closure> entry ->
				try{
					if (type.equals(entry.key)) entry.value.call(event)
				}catch (ex){
					ex.printStackTrace()
					println "Ignoring exception from event " + entry.key
				}
			}
		}
	}

	@OnWebSocketClose
	void onClose(Session session, int code, String reason){
		if (keepAliveThread != null) keepAliveThread.interrupt(); keepAliveThread = null
		reason.
		println "Connection closed. Reason: " + reason + ", code: " + code.toString()
	}

	@OnWebSocketError
	void onError(Throwable t){
		t.printStackTrace()
	}

	void send(Object message){
		try{
			if (message instanceof Map)
				session.getRemote().sendString(JSONUtil.json(message))
			else
				session.getRemote().sendString(message.toString())
		}catch (e){
			e.printStackTrace()
		}
	}

	CountDownLatch getLatch(){
		return latch
	}
}


