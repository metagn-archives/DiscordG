package hlaaftana.discordg.conn

import groovy.transform.Memoized

import hlaaftana.discordg.exceptions.*
import hlaaftana.discordg.objects.Client
import hlaaftana.discordg.objects.RateLimit
import hlaaftana.discordg.util.JSONUtil
import hlaaftana.discordg.util.Log

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.BaseRequest
import com.mashape.unirest.request.HttpRequest
import com.mashape.unirest.http.HttpMethod

/**
 * Provides ways to use the REST API for Discord.
 * @author Hlaaftana
 */
class Requester{
	static String discordApi = "https://discordapp.com/api/"
	static String canaryApi = "https://canary.discordapp.com/api/"
	static String ptbApi = "https://ptb.discordapp.com/api/"
	String baseUrl = discordApi
	Map<String, RateLimit> ratelimits = [:]

	Client client
	Requester(Client client){ this.client = client }

	def normal(){ baseUrl = discordApi }
	def normalDiscord(){ baseUrl = discordApi }
	def canary(){ baseUrl = canaryApi }
	def ptb(){ baseUrl = canaryApi }

	def parse(String request){
		def a = request.split(/\s+/, 3)
		return methodMissing(unmethodize(a[0]), a.size() == 2 ? a[1] : [a[1], a[2]])
	}

	def methodMissing(String methodName, args){
		List argl = args.class in [List, Object[]] ? args.collect() : [args]
		String url = methodName.startsWith("global") ? argl[0] : baseUrl + argl[0]
		String method = methodName.startsWith("global") ? methodName.substring(6)[0].toLowerCase() + methodName.substring(7) : methodName
		def aa = headerUp(Unirest."$method"(url))
		if (argl.size() == 1){
			return request(aa).body
		}else{
			return request(aa.body(argl[1] instanceof CharSequence ? argl[1].toString() : JSONUtil.json(argl[1]))).body
		}
	}

	/**
	 * Provides required headers to the request.
	 * @param request - the request object. Can be one of the HTTP methods in this class.
	 * @return the request with headers.
	 */
	def headerUp(def request){
		def req = request
		if (req instanceof HttpURLConnection){
			if (client.token != null) req.setRequestProperty("Authorization", client.token)
			if (!(req.requestMethod == "GET")) req.setRequestProperty("Content-Type", "application/json")
			req.setRequestProperty("User-Agent", client.fullUserAgent)
			return req
		}else if (req instanceof URL){
			return this.headerUp(req.openConnection())
		}else if (req instanceof BaseRequest){
			if (client.token != null) req = req.header("Authorization", client.token)
			if (!(req.httpRequest.httpMethod == HttpMethod.GET)) req = req.header("Content-Type", "application/json")
			return req.header("User-Agent", client.fullUserAgent)
		}else{
			throw new IllegalArgumentException("tried to add headers to a ${request.class}")
		}
	}

	def request(BaseRequest req){
		HttpRequest fuck = req.httpRequest
		if (ratelimits[simplifyUrl(fuck.url)]){
			Log.trace "Awaiting ratelimit for $fuck.url"
			while (ratelimits[simplifyUrl(fuck.url)]){}
		}
		def returned = fuck.asString()
		int status = returned.status
		Log.trace "HTTP REQUEST: $fuck.httpMethod $fuck.url $status"
		if (status == 429){
			Log.debug "Ratelimited when trying to $fuck.httpMethod to $fuck.url"
			RateLimit rl = new RateLimit(JSONUtil.parse(returned.body))
			ratelimits[simplifyUrl(fuck.url)] = rl
			Thread.sleep(rl.retryTime)
			ratelimits.remove(simplifyUrl(fuck.url))
			return request(req)
		}else if (status == 400){
			throw new BadRequestException(fuck.url)
		}else if (status == 401){
			throw new InvalidTokenException()
		}else if (status == 403){
			throw new NoPermissionException(fuck.url)
		}else if (status == 404){
			Log.warn "URL not found: $fuck.url. Report to hlaaf"
		}else if (status == 405){
			Log.warn "$fuck.httpMethod not allowed for $fuck.url. Report to hlaaf"
		}else if (status == 502){
			if (client.retryOn502){
				return request(req)
			}else{
				throw new HTTP5xxException(status, fuck.url)
			}
		}else if (status.intdiv(100) == 5){
			throw new HTTP5xxException(status, fuck.url)
		}else if (status.intdiv(100) >= 3){
			Log.warn "Got status code $status while ${fuck.httpMethod}ing to $fuck.url, this isn't an error but just a warning."
		}
		return returned
	}

	@Memoized
	static String simplifyUrl(String url){
		if (url.indexOf("?") > 0){
			url = url.substring(url.indexOf("?"))
		}
		return url.replaceAll(/\d+/, /numbers/)
	}

	@Memoized
	static String methodize(String method){
		return method.replaceAll(/[A-Z]/){ method.startsWith(it) ? it : "_$it" }.toUpperCase()
	}

	@Memoized
	static String unmethodize(String method){
		return method.toLowerCase().replaceAll(/_([a-z])/){ full, ch -> ch.toUpperCase() }
	}
}


