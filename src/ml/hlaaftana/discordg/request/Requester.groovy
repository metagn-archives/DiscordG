package ml.hlaaftana.discordg.request

import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest

class Requester{
	API api
	Requester(API api){ this.api = api }

    def get(String url){
        return headerUp(Unirest.get(url), true).asString().getBody()
    }

    def delete(String url){
        return headerUp(Unirest.delete(url)).asString().getBody()
    }

    def post(String url, Map body){
        return headerUp(Unirest.post(url)).body(body).asString().getBody()
    }

    def patch(String url, Map body){
        return headerUp(Unirest.patch(url)).body(body).asString().getBody()
    }

	def put(String url){
        return headerUp(Unirest.put(url)).asString().getBody()
	}

	def headerUp(def request, boolean isGet=false){
		def req = request
		if (api.token != null) req = req.header("Authorization", api.token)
		if (!isGet) req = req.header("Content-Type", "application/json")
		return req.header("User-Agent", "https://github.com/hlaaftana/DiscordG, 1.0")
	}
}


