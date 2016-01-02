package hlaaftana.discordg.request

import java.util.concurrent.CountDownLatch

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*
import hlaaftana.discordg.util.JSONUtil

import hlaaftana.discordg.events.Event
import hlaaftana.discordg.objects.API
import hlaaftana.discordg.objects.Server

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
			api.addListener { Event e ->
				if (e.getType().equals("GUILD_MEMBER_ADD")){
					Server server
					for (s in api.client.getServers()){
						if (s.getID().equals(e.json()["guild_id"])){
							server = s
						}
					}
					server.object["members"].add(e.json())
				}
			}
		}
		Event event = new Event(data, type)
		if (api.isLoaded()){
			for (listener in api.getListeners()){
				listener(event)
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


