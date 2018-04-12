package hlaaftana.discordg.net

class VoiceWSClient {}

/*import java.nio.*
import static java.nio.ByteBuffer.allocate as bytebuf

import org.eclipse.jetty.websocket.api.Session
import hlaaftana.discordg.VoiceClient
import hlaaftana.discordg.util.*

import org.eclipse.jetty.websocket.api.*

class VoiceWSClient extends WebSocketAdapter {
	VoiceClient vc
	Session session
	int heartbeats
	Thread heartbeatThread
	boolean connected
	boolean speaking
	DatagramSocket udpSocket
	Thread udpKeepAliveThread
	byte[] secretKey
	char seq = 0
	BigInteger timestamp

	VoiceWSClient(VoiceClient v){
		vc = v
	}

	void onWebSocketConnect(Session s){
		session = s
		session.policy.maxTextMessageSize = Integer.MAX_VALUE
		session.policy.maxTextMessageBufferSize = Integer.MAX_VALUE
		session.policy.maxBinaryMessageSize = Integer.MAX_VALUE
		session.policy.maxBinaryMessageBufferSize = Integer.MAX_VALUE
		identify()
	}

	void onWebSocketMessage(String message){
		Map content = JSONUtil.parse(message)
		def data = content['d']
		int op = content['op']
		def o = { it == op }
		def event = data
		if (op == 2){
			vc.ssrc = data['ssrc']
			vc.port = data['port']
			vc.heartbeatInterval = data['heartbeat_interval']
			udpSocket = new DatagramSocket()
			udpSocket.bind(new InetSocketAddress(vc.endpoint, vc.port))
			udpSend bytebuf(70).with { putInt(Integer.parseUnsignedInt(
				vc.ssrc.toString())); it }
			DatagramPacket receivedPacket = new DatagramPacket(new byte[70], 70)
			udpSocket.receive(receivedPacket)
			byte[] received = receivedPacket.data
			String selfIp = new String(received).with {
				substring(0, length() - 2).trim()
			}
			int selfPort = ((received[-2] as int) << 8) | received[-1]
			threadUdpKeepAlive()
			selectProtocol(selfIp, selfPort)
			startHeartbeating(vc.heartbeatInterval)
		}else if (op == 3){
			def interval = System.currentTimeMillis() - data
			vc.pingIntervals += interval
			event = [interval: interval, time: data]
		}else if (op == 4){
			secretKey = data.secret_key.collect { (byte) it } as byte[]
			connected = true
		}else if (op == 5){
			event = [speaking: data.speaking, ssrc: data.ssrc,
				user: vc.client.user(data.user_id)]
		}else{
			vc.log.info "Unhandled voice op code $op. " +
				"Full content (please report to Hlaaftana):\n$content', vc.log.name + 'WS"
		}
		vc.dispatchEvent(op, event << [json: data])
	}

	void onWebSocketClose(int code, String reason){
		vc.log.info "Connection closed. Reason: $reason, code: $code', vc.log.name + 'WS"
		Thread.start { vc.dispatchEvent('CLOSE', [code: code, reason: reason, json: [code: code, reason: reason]]) }
		if (heartbeatThread){
			heartbeatThread.interrupt()
			heartbeatThread = null
		}
		if (udpKeepAliveThread){
			udpKeepAliveThread.interrupt()
			udpKeepAliveThread = null
		}
		udpSocket.close()
		session.close()
		Thread.currentThread().close()
	}

	void onWebSocketError(Throwable t){
		t.printStackTrace()
	}

	void identify(){
		send op: 0, d: [
			token: vc.client.token,
			guild_id: vc.id,
			user_id: vc.client.user.id,
			session_id: vc.client.sessionId
		]
	}

	void selectProtocol(String ip, int port){
		send op: 1, d: [
			protocol: 'udp',
			data: [
				address: ip,
				port: port,
				mode: vc.encryptionMode
			]
		]
	}

	void heartbeat(){
		heartbeats++
		send op: 3, d: System.currentTimeMillis().toString()
	}

	void startHeartbeating(long interval){
		heartbeatThread = Thread.startDaemon {
			while (true){
				heartbeat()
				try{ Thread.sleep(interval) }catch (InterruptedException ex){ return }
			}
		}
	}

	void udpKeepAlive(){
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES + 1)
		udpSend bytebuf(65).with {
			put((byte) 0xC9)
			putLong 0
			it
		}
	}

	void threadUdpKeepAlive(long interval = 5000){
		udpKeepAliveThread = Thread.startDaemon {
			while (true){
				udpKeepAlive()
				Thread.sleep(interval)
			}
		}
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

	void udpSend(ByteBuffer buf){ udpSend(buf.array()) }

	void udpSend(byte[] arr){
		udpSocket.send new DatagramPacket(arr, arr.length)
	}
	
	void sendVoice(ByteBuffer buf){ sendVoice(buf.array()) }
	void sendVoice(byte[] bytes){
		ByteBuffer x = bytebuf(24).with {
			put(0x80)
			put(0x78)
			putChar(seq)
			putInt(Integer.parseUnsignedInt(timestamp.toString()))
			putInt(Integer.parseUnsignedInt(vc.ssrc.toString()))
			putLong(0)
			putInt(0)
			it
		}
		seq = seq == Character.MAX_VALUE ? 0 : seq + 1
		timestamp += 24
		udpSend(x.array() + bytes)
	}
}
*/