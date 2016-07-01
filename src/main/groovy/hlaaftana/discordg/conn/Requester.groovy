package hlaaftana.discordg.conn

import java.util.regex.Pattern

import groovy.transform.Memoized
import hlaaftana.discordg.Client;
import hlaaftana.discordg.exceptions.*
import hlaaftana.discordg.objects.RateLimit
import hlaaftana.discordg.objects.TextChannel
import hlaaftana.discordg.util.MiscUtil
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
class Requester {
	static String discordApi = "https://discordapp.com/api/"
	static String canaryApi = "https://canary.discordapp.com/api/"
	static String ptbApi = "https://ptb.discordapp.com/api/"
	String baseUrl = discordApi
	Map<String, RateLimit> ratelimits = [:]

	Client client
	Requester(Client client, String concatUrl = ""){
		this.client = client
		baseUrl = concatUrlPaths(baseUrl, concatUrl)
	}
	Requester(Requester requester, String concatUrl = ""){
		client = requester.client
		baseUrl = requester.baseUrl
		baseUrl = concatUrlPaths(baseUrl, concatUrl)
	}

	def normal(){ baseUrl = discordApi }
	def normalDiscord(){ baseUrl = discordApi }
	def canary(){ baseUrl = canaryApi }
	def ptb(){ baseUrl = ptbApi }

	def parse(String request){
		def a = request.split(/\s+/, 3)
		methodMissing(MiscUtil.unconstantize(a[0]), a.size() == 2 ? a[1] : [a[1], a[2]])
	}

	def methodMissing(String methodName, args){
		List argl = args.class in [List, Object[]] ? args.collect() : [args]
		String url
		List methodParams = MiscUtil.constantize(methodName).split("_") as List
		boolean global = "GLOBAL" in methodParams
		if (global) methodParams -= "GLOBAL"
		boolean json = "JSON" in methodParams
		if (json) methodParams -= "JSON"
		if (global) url = argl[0]
		else url = concatUrlPaths(baseUrl, argl[0])
		String method = MiscUtil.unconstantize(methodParams[0])
		def aa = headerUp(Unirest."$method"(url))
		if (argl.size() > 1){
			def data = argl[1] instanceof CharSequence ? argl[1].toString() : JSONUtil.json(argl[1])
			aa = aa.body(data)
		}
		def bbdbfdffdbfdpbodf = request(aa).body
		json ? JSONUtil.parse(bbdbfdffdbfdpbodf) : bbdbfdffdbfdpbodf
	}

	def headerUp(def request){
		def req = request
		if (req instanceof HttpURLConnection){
			if (client.token != null) req.setRequestProperty("Authorization", client.token)
			if (!(req.requestMethod == "GET")) req.setRequestProperty("Content-Type", "application/json")
			req.setRequestProperty("User-Agent", client.fullUserAgent)
			req
		}else if (req instanceof URL){
			headerUp(req.openConnection())
		}else if (req instanceof BaseRequest){
			if (client.token != null) req = req.header("Authorization", client.token)
			if (!(req.httpRequest.httpMethod == HttpMethod.GET)) req = req.header("Content-Type", "application/json")
			req.header("User-Agent", client.fullUserAgent)
		}else{
			throw new IllegalArgumentException("tried to add headers to a ${request.class}")
		}
	}

	def request(BaseRequest req){
		HttpRequest fuck = req.httpRequest
		String rlUrl = fuck.url.replaceFirst(Pattern.quote(client.requester.baseUrl), "")
		if (ratelimits[simplifyUrl(rlUrl)]){
			client.log.trace "Awaiting ratelimit for $rlUrl"
			while (ratelimits[simplifyUrl(rlUrl)]){}
		}
		def returned = fuck.asString()
		int status = returned.status
		client.log.trace "HTTP REQUEST: $fuck.httpMethod $fuck.url $status", client.log.name + "HTTP"
		if (status == 429){
			client.log.debug "Ratelimited when trying to $fuck.httpMethod to $fuck.url", client.log.name + "HTTP"
			RateLimit rl = new RateLimit(client, JSONUtil.parse(returned.body))
			println rl.bucket
			ratelimits[simplifyUrl(rlUrl)] = rl
			Thread.sleep(rl.retryTime)
			ratelimits.remove(simplifyUrl(rlUrl))
			return request(req)
		}else if (status == 400){
			throw new BadRequestException(fuck.url, JSONUtil.parse(returned.body)["message"])
		}else if (status == 401){
			throw new InvalidTokenException(client.token)
		}else if (status == 403){
			throw new NoPermissionException(fuck.url, JSONUtil.parse(returned.body)["message"])
		}else if (status == 404){
			client.log.warn "URL not found: $fuck.url. Report to hlaaf", client.log.name + "HTTP"
		}else if (status == 405){
			client.log.warn "$fuck.httpMethod not allowed for $fuck.url. Report to hlaaf", client.log.name + "HTTP"
		}else if (status == 502){
			if (client.retryOn502){
				return request(req)
			}else{
				throw new HTTP5xxException(status, fuck.url)
			}
		}else if (status.intdiv(100) == 5){
			throw new HTTP5xxException(status, fuck.url)
		}else if (status.intdiv(100) >= 3){
			client.log.warn "Got status code $status while ${fuck.httpMethod}ing to $fuck.url, this isn't an error but just a warning.", client.log.name + "HTTP"
		}
		returned
	}

	@Memoized
	static String simplifyUrl(String url){
		if (url.indexOf("?") > 0){
			url = url.substring(url.indexOf("?"))
		}
		int i = 0
		url.replaceAll(/\d+/){ "@id${++i}:$it" }
	}

	@Memoized
	static String concatUrlPaths(String...yeah){
		String ass = yeah[0]
		List whoop = (yeah as List)[1..-1]
		whoop.each { String a ->
			if (a.empty) return
			if (ass.toList().last() == "/"){
				if (a.toList().first() == "/") ass += a.substring(1)
				else ass += a
			}else{
				if (a.toList().first() == "/") ass += a
				else ass += "/$a"
			}
		}
		ass
	}
}


