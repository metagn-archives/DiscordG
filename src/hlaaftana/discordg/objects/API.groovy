package hlaaftana.discordg.objects

import com.sun.jersey.api.client.Client

import groovy.lang.Closure
import hlaaftana.discordg.request.Requester
import hlaaftana.discordg.request.WSClient

import javax.websocket.WebSocketContainer
import javax.websocket.ContainerProvider

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.json.JSONObject

class API{
	private Client restClient
	private Requester requester
	private String token
	private WSClient wsClient
	private hlaaftana.discordg.objects.Client discordClient
	private List<Closure> listeners = new ArrayList<Closure>()
	JSONObject readyData

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
				JSONObject response = new JSONObject(this.getRequester().post("https://discordapp.com/api/auth/login", new JSONObject().put("email", email).put("password", password)))
				token = response.get("token")
				SslContextFactory sslFactory = new SslContextFactory()
				WebSocketClient client = new WebSocketClient(sslFactory)
				WSClient socket = new WSClient(this)
				String gateway = new JSONObject(this.getRequester().get("https://discordapp.com/api/gateway")).get("url")
				client.start()
				ClientUpgradeRequest upgreq = new ClientUpgradeRequest()
				client.connect(socket, new URI(gateway), upgreq)
				this.wsClient = socket
				discordClient = new hlaaftana.discordg.objects.Client(this)
				println "Successfully logged in!"
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
		})
		thread.start()
	}

	hlaaftana.discordg.objects.Client getClient() {
		return discordClient
	}

	void addListener(Closure closure) {
		listeners.add(closure)
	}

	void removeListener(Closure closure) {
		listeners.remove(closure)
	}

	List<Closure> getListeners() {
		return listeners
	}

	boolean isLoaded(){
		return restClient != null && requester != null && token != null && wsClient != null && discordClient != null
	}
}
