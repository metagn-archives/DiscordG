package hlaaftana.discordg

import hlaaftana.discordg.objects.API

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
