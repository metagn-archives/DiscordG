package hlaaftana.discordg.oauth

import groovy.json.*
import hlaaftana.discordg.objects.*
import hlaaftana.discordg.util.*

class BotClient extends Client {
	private String actualToken
	String getToken(){ return "Bot " + actualToken }
	void setToken(String newToken){ actualToken = newToken }

	void login(String token, boolean threaded=true){
		Closure a = {
			this.token = token
			connectGateway()
			while(this.wsClient == null){}
		}
		if (threaded){ Thread.start(a) }
		else{ a() }
	}

	void login(String customBotName, boolean threaded = true, Closure requestToken){
		Closure a = {
			File tokenFile = new File(this.tokenCachePath)
			if (!tokenFile.exists()){
				tokenFile.createNewFile()
				tokenFile.write(JsonOutput.prettyPrint(JsonOutput.toJson([
					bots: [:]
				])))
			}
			Map original = new JsonSlurper().parse(tokenFile)
			String originalToken
			try{
				originalToken = original["bots"][customBotName]["token"]
			}catch (ex){
				original["bots"] = [:]
			}
			if (originalToken == null){
				String newToken = requestToken()
				original["bots"][customBotName] = [token: newToken]
				tokenFile.write(JsonOutput.prettyPrint(JsonOutput.toJson(original)))
				this.token = newToken
			}else{
				try{
					this.token = originalToken
				}catch (ex){
					String newToken = requestToken()
					original["bots"][customBotName] = [token: newToken]
					tokenFile.write(JsonOutput.prettyPrint(JsonOutput.toJson(original)))
					this.token = newToken
				}
			}
			connectGateway()
		}
		if (threaded){ Thread.start(a) }
		else{ a() }
	}

	Application getApplication(){
		return new Application(this, JSONUtil.parse(this.requester.get("oauth2/applications/@me")))
	}

	boolean isLoaded(){
		return this.requester != null && this.token != null && this.wsClient != null && this.wsClient.loaded && !this.cache.empty && !this.servers.any { it.unavailable }
	}
}
