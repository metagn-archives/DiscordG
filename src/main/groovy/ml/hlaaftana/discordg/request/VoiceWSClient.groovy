package ml.hlaaftana.discordg.request

import java.nio.*

import org.eclipse.jetty.websocket.api.Session

import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.util.*

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*

@WebSocket
class VoiceWSClient {
	API api
	Session session
	Thread keepAliveThread
	VoiceChannel channel
	int ssrc
	String endpoint
	long ping
	boolean connected
	boolean speaking
	DatagramSocket udpSocket
	Thread udpKeepAliveThread
	VoiceWSClient(API api){ this.api = api; this.channel = api.voiceData.channel; this.endpoint = api.voiceData.endpoint }

	@OnWebSocketConnect
	void onConnect(Session session){
		this.session = session
		this.session.policy.maxTextMessageSize = Integer.MAX_VALUE
		this.session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		Map a = [
			"op": 0,
			"d": [
				"token": api.token,
				"server_id": channel.server.id,
				"user_id": api.client.user.id,
				"session_id": api.client.sessionId
			],
		]
		this.send(a)
	}

	@OnWebSocketMessage
	void onMessage(Session session, String message) throws IOException{
		// jda
		Map content = JSONUtil.parse(message)
		def data = content["d"]
		int op = content["op"]
		def o = { it == op }
		if (o(2)){
			ssrc = data["ssrc"]
			int port = data["port"]
			long heartbeat = data["heartbeat_interval"]

			udpSocket = new DatagramSocket()

			ByteBuffer buffer = ByteBuffer.allocate(70)
			buffer.putInt(ssrc)

			DatagramPacket discoveryPacket = new DatagramPacket(buffer.array(), buffer.array().length, new InetSocketAddress(api.voiceData["endpoint"], port))
			udpSocket.send(discoveryPacket)

			DatagramPacket receivedPacket = new DatagramPacket(new byte[70], 70)
			udpSocket.receive(receivedPacket)

			byte[] received = receivedPacket.data

			String ourIP = new String(receivedPacket.data)
			ourIP = ourIP.substring(0, ourIP.length() - 2)
			ourIP = ourIP.trim()

			byte[] portBytes = new byte[2]
			portBytes[0] = received[received.length - 1]
			portBytes[1] = received[received.length - 2]

			int firstByte = (0x000000FF & ((int) portBytes[0]))
			int secondByte = (0x000000FF & ((int) portBytes[1]))

			int ourPort = (firstByte << 8) | secondByte

			udpKeepAliveThread = new Thread({
				while (true){
					ByteBuffer buffer2 = ByteBuffer.allocate(Long.BYTES + 1);
                    buffer2.put((byte)0xC9);
                    buffer2.putLong(0)
                    DatagramPacket keepAlivePacket = new DatagramPacket(buffer2.array(), buffer2.array().length, new InetSocketAddress(api.voiceData["endpoint"], port))
                    udpSocket.send(keepAlivePacket)

                    Thread.sleep(5000)
				}
			})
			udpKeepAliveThread.daemon = true
			udpKeepAliveThread.start()

			InetSocketAddress address = new InetSocketAddress(ourIP, ourPort);

			this.send([
				op: 1,
				d: [
					protocol: "udp",
					data: [
						address: address.hostString,
						port: address.port,
						mode: "plain"
					]
				]
			])

			keepAliveThread = new Thread({
				while (true){
					this.send(["op": 3, "d": System.currentTimeMillis().toString()])
					try{ Thread.sleep(heartbeat) }catch (InterruptedException ex){}
				}
			})
			keepAliveThread.daemon = true
			keepAliveThread.start()

			api.dispatchEvent("VOICE_READY", data << ["fullData": data])
		}else if(o(3)){
			this.ping = System.currentTimeMillis() - data
			api.dispatchEvent("VOICE_PING_UPDATE", ["ping": this.ping, "fullData": data])
		}else if(o(4)){
			this.connected = true
			api.dispatchEvent("VOICE_CONNECTED", ["fullData": data])
		}else if(o(5)){
			api.dispatchEvent("USER_SPEAKING_UPDATE", ["speaking": data["speaking"], "user": api.client.getUserById(data["user_id"]), "fullData": data])
		}else{
			Log.info "Unhandled voice op code ${op}. Full content (please report to Hlaaftana):\n${content}"
		}
	}

	@OnWebSocketClose
	void onClose(Session session, int code, String reason){
		Log.info "Connection closed. Reason: " + reason + ", code: " + code.toString()
		try{
			this.close()
		}catch (ex){

		}
	}

	@OnWebSocketError
	void onError(Throwable t){
		t.printStackTrace()
	}

	void send(Object message){
		try{
			if (message instanceof Map)
				session.remote.sendString(JSONUtil.json(message))
			else
				session.remote.sendBytes(message)
		}catch (e){
			e.printStackTrace()
		}
	}

	void close(boolean mute=false, boolean deaf=false){
		if (keepAliveThread != null){ keepAliveThread.interrupt(); keepAliveThread = null }

		api.wsClient.send([
			"op": 4,
			"d": [
				"guild_id": null,
				"channel_id": null,
				"self_mute": mute,
				"self_deaf": deaf,
			]
		])
		connected = false

		this.session.close()
	}
}
