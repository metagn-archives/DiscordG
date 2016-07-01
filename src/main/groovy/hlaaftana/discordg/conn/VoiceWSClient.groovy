package hlaaftana.discordg.conn

import java.nio.*
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session

import hlaaftana.discordg.Client;
import hlaaftana.discordg.VoiceClient
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.util.*

import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.api.annotations.*

@WebSocket
class VoiceWSClient {
	VoiceClient client
	Session session
	Thread keepAliveThread
	boolean connected
	boolean speaking
	DatagramSocket udpSocket
	Thread udpKeepAliveThread

	VoiceWSClient(VoiceClient client){
		this.client = client
	}

	@OnWebSocketConnect
	void onConnect(Session session){
		this.session = session
		this.session.policy.maxTextMessageSize = Integer.MAX_VALUE
		this.session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		this.session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		identify()
	}

	@OnWebSocketMessage
	void onMessage(Session session, String message){
		Map content = JSONUtil.parse(message)
		def data = content["d"]
		int op = content["op"]
		def o = { it == op }
		if (o(2)){
			client.ssrc = data["ssrc"]
			int port = data["port"]
			long heartbeat = data["heartbeat_interval"]

			udpSocket = new DatagramSocket()

			ByteBuffer buffer = ByteBuffer.allocate(70)
			buffer.putInt(ssrc)

			DatagramPacket discoveryPacket = new DatagramPacket(buffer.array(), buffer.array().length, new InetSocketAddress(client.voiceData["endpoint"], port))
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
                    DatagramPacket keepAlivePacket = new DatagramPacket(buffer2.array(), buffer2.array().length, new InetSocketAddress(client.voiceData["endpoint"], port))
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

			client.dispatchEvent("VOICE_READY", data << ["fullData": data])
		}else if(o(3)){
			this.ping = System.currentTimeMillis() - data
			client.dispatchEvent("VOICE_PING_UPDATE", ["ping": this.ping, "fullData": data])
		}else if(o(4)){
			this.connected = true
			client.dispatchEvent("VOICE_CONNECTED", ["fullData": data])
		}else if(o(5)){
			client.dispatchEvent("USER_SPEAKING_UPDATE", ["speaking": data["speaking"], "user": client.getUserById(data["user_id"]), "fullData": data])
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

	void identify(){
		Map a = [
			op: 0,
			d: [
				token: client.token,
				server_id: channel.server.id,
				user_id: client.user.id,
				session_id: client.sessionId
			],
		]
		send(a)
	}

	void send(message){
		String ass = message instanceof Map ? JSONUtil.json(message) : message.toString()
		session.remote.sendString(ass)
	}

	void send(Map ass, int op){
		send op: op, d: ass
	}

	void send(int op, Map ass){
		send ass, op
	}
}
