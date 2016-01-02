package hlaaftana.discordg.objects

import com.sun.jersey.api.client.Client

import groovy.lang.Closure
import hlaaftana.discordg.request.Requester
import hlaaftana.discordg.request.WSClient
import hlaaftana.discordg.util.JSONUtil

import javax.websocket.WebSocketContainer
import javax.websocket.ContainerProvider

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

class API{
	private Client restClient
	private Requester requester
	private String token
	private WSClient wsClient
	private hlaaftana.discordg.objects.Client client
	private Map<String, Closure> listeners = new HashMap<String, Closure>()
	Map readyData

	API(){
		restClient = Client.create()
		requester = new Requester(this)
	}

	Client getRESTClient() { return restClient }
	Requester getRequester() { return requester }
	WSClient getWebSocketClient(){ return wsClient }
	String getToken(){ return token }

	void login(String email, String password){
		Thread thread = new Thread({
			try{
				Map response = JSONUtil.parse(this.getRequester().post("https://discordapp.com/api/auth/login", ["email": email, "password": password]))
				token = response["token"]
				SslContextFactory sslFactory = new SslContextFactory()
				WebSocketClient client = new WebSocketClient(sslFactory)
				WSClient socket = new WSClient(this)
				String gateway = JSONUtil.parse(this.getRequester().get("https://discordapp.com/api/gateway"))["url"]
				client.start()
				ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
				client.connect(socket, new URI(gateway), upgreq)
				this.wsClient = socket
				this.client = new hlaaftana.discordg.objects.Client(this)
				println "Successfully logged in!"
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
		})
		thread.start()
	}

	hlaaftana.discordg.objects.Client getClient() {
		return client
	}

	void addListener(String event, Closure closure) {
		listeners.put(event.toUpperCase().replace(' ', '_'), closure)
	}

	void removeListener(String event, Closure closure) {
		listeners.remove(event.toUpperCase().replace(' ', '_'), closure)
	}

	Map<String, Closure> getListeners(){ return listeners }

	boolean isLoaded(){
		return restClient != null && requester != null && token != null && wsClient != null && client != null
	}
}
