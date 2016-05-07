package io.github.hlaaftana.discordg.request

import io.github.hlaaftana.discordg.objects.Client
import io.github.hlaaftana.discordg.util.JSONUtil

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest

/**
 * Provides ways to use the REST API for Discord.
 * @author Hlaaftana
 */
class Requester{
	static String discordApi = "https://discordapp.com/api/"
	
	Client client
	Requester(Client client){ this.client = client }

	/**
	 * GETs from a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
    def get(String url){
        return headerUp(Unirest.get(url), true).asString().getBody()
    }

	/**
	 * DELETEs a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
    def delete(String url){
        return headerUp(Unirest.delete(url)).asString().getBody()
    }

	/**
	 * POSTs to a URL with a body.
	 * @param url - the URL string.
	 * @param body - the body as a Map which will be converted to JSON.
	 * @return the response as a string.
	 */
    def post(String url, Map body){
        return headerUp(Unirest.post(url)).body(JSONUtil.json(body)).asString().getBody()
    }

	/**
	 * PATCHes a URL with a body.
	 * @param url - the URL string.
	 * @param body - the body as a Map which will be converted to JSON.
	 * @return the response as a string.
	 */
    def patch(String url, Map body){
        return headerUp(Unirest.patch(url)).body(JSONUtil.json(body)).asString().getBody()
    }

	/**
	 * PUTs to a URL.
	 * @param url - the URL string.
	 * @return the response as a string.
	 */
	def put(String url, Map body){
        return headerUp(Unirest.put(url)).body(JSONUtil.json(body)).asString().getBody()
	}

	/**
	 * Provides required headers to the request.
	 * @param request - the request object. Can be one of the HTTP methods in this class.
	 * @return the request with headers.
	 */
	def headerUp(def request, boolean isGet=false){
		def req = request
		if (req instanceof URLConnection){
			if (client.token != null) req.setRequestProperty("Authorization", client.token)
			if (!isGet) req.setRequestProperty("Content-Type", "application/json")
			req.setRequestProperty("User-Agent", client.fullUserAgent)
			return req
		}else if (req instanceof URL){
			return this.headerUp(req.openConnection(), isGet)
		}else{
			if (client.token != null) req = req.header("Authorization", client.token)
			if (!isGet) req = req.header("Content-Type", "application/json")
			return req.header("User-Agent", client.fullUserAgent)
		}
	}
}


