package ml.hlaaftana.discordg

import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.util.Log

class APIBuilder {
	static API build(String email, String password){
		API api = new API()
		api.login(email, password)
		return api
	}
	static API build(){
		return new API()
	}
}
