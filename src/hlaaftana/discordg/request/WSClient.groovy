package hlaaftana.discordg.request

import java.util.concurrent.CountDownLatch

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*
import org.json.JSONObject

import hlaaftana.discordg.events.Event
import hlaaftana.discordg.objects.API;

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
		this.send(new JSONObject()
			.put("op", 2)
			.put("d", new JSONObject()
				.put("token", api.getToken())
				.put("v", 3)
				.put("properties", new JSONObject()
					.put("\$os", System.getProperty("os.name"))
					.put("\$browser", "DiscordG")
					.put("\$device", "Groovy")
					.put("\$referrer", "https://discordapp.com/@me")
					.put("\$referring_domain", "discordapp.com")
				)
			)
		.toString())
		println "Sent API details."
		latch.countDown()
	}

	@OnWebSocketMessage
	void onMessage(Session session, String message) throws IOException{
		JSONObject content = new JSONObject(message)
		String type = content.getString("t")
		JSONObject data = content.get("d")
		int responseAmount = Integer.parseInt(content.get("s").toString())
		if (type.equals("READY") || type.equals("RESUMED")){
			long heartbeat = data.getLong("heartbeat_interval")
			try{
				keepAliveThread = new Thread({
					while (true){
						this.send(new JSONObject().put("op", 1).put("d", System.currentTimeMillis()).toString())
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

	void send(String message){
		try{
			session.getRemote().sendString(message)
		}catch (e){
			e.printStackTrace()
		}
	}

	CountDownLatch getLatch(){
		return latch
	}
}


