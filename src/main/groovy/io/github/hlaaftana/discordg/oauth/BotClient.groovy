package io.github.hlaaftana.discordg.oauth

import groovy.json.*
import io.github.hlaaftana.discordg.objects.*
import io.github.hlaaftana.discordg.util.*

class BotClient extends Client {
	/*private String actualToken
	String getToken(){ return "Bot " + actualToken }
	void setToken(String newToken){ actualToken = newToken }*/

	void login(String token, boolean threaded=true){
		Closure a = {
			super.login(token, false)
			while(!this.loaded){}
			this.token = "Bot $token"
		}
		if (threaded){ Thread.start(a) }
		else{ a() }
	}

	void login(String customBotName, Closure requestToken, boolean threaded = true){
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
				this.login(newToken, false)
			}else{
				try{
					this.login(originalToken, false)
				}catch (ex){
					String newToken = requestToken()
					original["bots"][customBotName] = [token: newToken]
					tokenFile.write(JsonOutput.prettyPrint(JsonOutput.toJson(original)))
					this.login(newToken, false)
				}
			}
		}
		if (threaded){ Thread.start(a) }
		else{ a() }
	}

	User editProfile(Map data, Random random=new Random()){
		Map map = ["avatar": this.user.avatarHash, "email": "${new BigInteger(130, random).toString(16)}@null.null", "password": this.password, "username": this.user.username]
		if (data["avatar"] != null){
			if (data["avatar"] instanceof String && !(data["avatar"].startsWith("data"))){
				data["avatar"] = ConversionUtil.encodeToBase64(data["avatar"] as File)
			}else if (data["avatar"] instanceof File){
				data["avatar"] = ConversionUtil.encodeToBase64(data["avatar"])
			}
		}
		Map response = JSONUtil.parse this.requester.patch("https://discordapp.com/api/users/@me", map << data)
		this.email = response.email
		this.password = (data["new_password"] != null && data["new_password"] instanceof String) ? data["new_password"] : this.password
		this.token = "Bot " + response.token
		this.readyData["user"]["verified"] = response.verified
		return new User(this, response)
	}
}
