package hlaaftana.discordg.objects

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.config.DefaultClientConfig
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler

import groovy.lang.Closure
import hlaaftana.discordg.request.*
import hlaaftana.discordg.util.*

import javax.websocket.WebSocketContainer
import javax.websocket.ContainerProvider

import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient

class API{
	Client restClient
	Requester requester
	String token
	WSClient wsClient
	hlaaftana.discordg.objects.Client client
	Map<String, Closure> listeners = [:]
	Map readyData
	static boolean debug
	// if you want to use global variables through the API object. mostly for utility
	Map<String, Object> fields = [:]

	API(boolean debug=false){
		DefaultClientConfig config = new DefaultClientConfig()
		config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true)
		restClient = Client.create(config)
		requester = new Requester(this)
		this.debug = debug

		//add built-in listeners
		//works
		this.addListener("guild member add", { Event e ->
			Server server = e.data.server
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["members"].add(e.data)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
		//works
		this.addListener("guild member remove", { Event e ->
			Server server = e.data.server
			Member memberToRemove = e.data.member
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["members"].remove(memberToRemove.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
		//works
		this.addListener("guild role create", { Event e ->
			Server server = e.data.server
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].add(e.data.role.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
		//works
		this.addListener("guild role delete", { Event e ->
			Server server = e.data.server
			Role roleToRemove = e.data.role
			Map serverInReady = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			Map serverInReady2 = serverInReady
			serverInReady2["roles"].remove(roleToRemove.object)
			this.readyData["guilds"].remove(serverInReady)
			this.readyData["guilds"].add(serverInReady2)
		})
		// our (my) server objects don't read from READY to get their channels
		// however we might in the future because they did that to the
		// members request already
		// feel free to add your own listener in your code / PR
		// to make sure this works to read from READY too
		// works
		this.addListener("channel create", { Event e ->
			Server server = e.data.server
			if (server == null){
				this.readyData["private_channels"].add(e.channel.object)
			}
		})
		// works
		this.addListener("channel delete", { Event e ->
			Server server = e.data.server
			if (server == null){
				Map channelToRemove = this.readyData["private_channels"].find { it["id"].equals(e.data.channel.getId()) }
				this.readyData["private_channels"].remove(channelToRemove)
			}
		})
		// works
		this.addListener("guild create", { Event e ->
			Server server = e.data.server
			this.readyData["guilds"].add(server.object)
		})
		// works
		this.addListener("guild delete", { Event e ->
			Server server = e.data.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			this.readyData["guilds"].remove(serverToRemove)
		})
		// works
		this.addListener("guild member update", { Event e ->
			Server server = e.data.server
			Member member = e.data.member
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			List membersToEdit = serverToRemove["members"]
			Map memberToEdit = membersToEdit.find { it["id"].equals(member.getId()) }
			membersToEdit.remove(memberToEdit)
			membersToEdit.add(member.object)
			this.readyData["guilds"].remove(serverToRemove)
			serverToRemove["members"] = membersToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
		// works
		this.addListener("guild role update", { Event e ->
			Server server = e.data.server
			Role role = e.data.role
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(server.getId()) }
			List rolesToEdit = serverToRemove["roles"]
			Map roleToEdit = rolesToEdit.find { it["id"].equals(role.getId()) }
			rolesToEdit.remove(roleToEdit)
			rolesToEdit.add(role.object)
			this.readyData["guilds"].remove(serverToRemove)
			serverToRemove["roles"] = rolesToEdit
			this.readyData["guilds"].add(serverToRemove)
		})
		// works
		this.addListener("guild update", { Event e ->
			Server newServer = e.data.server
			Map serverToRemove = this.readyData["guilds"].find { it["id"].equals(newServer.getId()) }
			this.readyData["guilds"].remove(serverToRemove)
			this.readyData["guilds"].add(newServer.object)
		})
	}

	Client getRESTClient() { return restClient }
	WSClient getWebSocketClient(){ return wsClient }

	void login(String email, String password){
		Thread thread = new Thread({
			try{
				Log.info "Logging in..."
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
				Log.info "Successfully logged in!"
			}catch (e){
				e.printStackTrace()
				System.exit(0)
			}
		})
		thread.start()
	}

	void addListener(String event, Closure closure) {
		listeners.put(event.replace("change", "update").replace("update".toUpperCase(), "change".toUpperCase()).toUpperCase().replace(' ', '_'), closure)
	}

	void removeListener(String event, Closure closure) {
		listeners.remove(event.replace("change", "update").replace("update".toUpperCase(), "change".toUpperCase()).toUpperCase().replace(' ', '_'), closure)
	}

	void removeListenersFor(String event){
		for (e in listeners.entrySet()){
			if (e.key == event.replace("change", "update").replace("update".toUpperCase(), "change".toUpperCase()).toUpperCase().replace(' ', '_')) listeners.remove(e.key, e.value)
		}
	}

	void addField(String key, value){
		fields.add(key, value)
	}

	def getField(String key){
		return fields[key]
	}

	boolean isLoaded(){
		return restClient != null && requester != null && token != null && wsClient != null && client != null && readyData != null
	}
}
