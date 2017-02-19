package hlaaftana.discordg.net

import java.util.regex.Pattern

import groovy.transform.Memoized
import hlaaftana.discordg.Client;
import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.exceptions.*
import hlaaftana.discordg.util.MiscUtil
import hlaaftana.discordg.util.JSONUtil
import hlaaftana.discordg.util.Log
import static hlaaftana.discordg.util.CasingType.*

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.BaseRequest
import com.mashape.unirest.request.HttpRequest
import com.mashape.unirest.http.HttpMethod

/**
 * Provides ways to use the REST API for Discord.
 * @author Hlaaftana
 */
class HTTPClient {
	static String discordApi = "https://discordapp.com/api/"
	static String canaryApi = "https://canary.discordapp.com/api/"
	static String ptbApi = "https://ptb.discordapp.com/api/"
	static String defaultApi = discordApi + "v6/"
	static String latestApi = defaultApi
	static Map<Integer, String> errorCodes = [
		10001: "Unknown Account",
		10002: "Unknown Application",
		10003: "Unknown Channel",
		10004: "Unknown Guild",
		10005: "Unknown Integration",
		10006: "Unknown Invite",
		10007: "Unknown Member",
		10008: "Unknown Message",
		10009: "Unknown Overwrite",
		10010: "Unknown Provider",
		10011: "Unknown Role",
		10012: "Unknown Token",
		10013: "Unknown User",
		20001: "Bots cannot use this endpoint",
		20002: "Only bots can use this endpoint",
		30001: "Maximum number of guilds reached (100)",
		30002: "Maximum number of friends reached (1000)",
		40001: "Unauthorized",
		50001: "Missing Access",
		50002: "Invalid Account Type",
		50003: "Cannot execute action on a DM channel",
		50004: "Embed Disabled",
		50005: "Cannot edit a message authored by another user",
		50006: "Cannot send an empty message",
		50007: "Cannot send messages to this user",
		50008: "Cannot send messages in a voice channel",
		50009: "Channel verification level is too high",
		50010: "OAuth2 application does not have a bot",
		50011: "OAuth2 application limit reached",
		50012: "Invalid OAuth State",
		50013: "Missing Permissions",
		50014: "Invalid authentication token",
		50015: "Note is too long",
		50016: "Provided too few or too many messages to delete. Must provide at least 2 and fewer than 100 messages to delete.",
	]
	String baseUrl = defaultApi
	Map<String, RateLimit> ratelimits = [:]

	Client client
	HTTPClient(Client client, String concatUrl = ""){
		this.client = client
		if (concatUrl) baseUrl = concatUrlPaths(baseUrl, concatUrl)
	}
	HTTPClient(HTTPClient http, String concatUrl = ""){
		client = http.client
		baseUrl = http.baseUrl
		if (concatUrl) baseUrl = concatUrlPaths(baseUrl, concatUrl)
	}

	def normal(){ baseUrl = discordApi }
	def normalDiscord(){ baseUrl = discordApi }
	def canary(){ baseUrl = canaryApi }
	def ptb(){ baseUrl = ptbApi }

	def setBaseUrl(String n){
		baseUrl = n
		latestApi = n
	}

	def parse(String request){
		def a = request.split(/\s+/, 3)
		methodMissing(CAMEL.convert(a[0], CONSTANT), a.size() == 2 ? a[1] : [a[1], a[2]])
	}

	def methodMissing(String methodName, args){
		List argl = args.class in [List, Object[]] ? args.collect() : [args]
		String url
		List methodParams = CAMEL.convert(methodName, CONSTANT).split("_") as List
		boolean global = methodParams.remove('GLOBAL')
		boolean json = methodParams.remove('JSON')
		boolean body = methodParams.remove('BODY')
		boolean request = methodParams.remove('REQUEST')
		if (global) url = argl[0]
		else url = concatUrlPaths(baseUrl, argl[0])
		String method = CONSTANT.convert(methodParams[0], CAMEL)
		def aa = headerUp(Unirest."$method"(url))
		if (argl.size() > 1){
			def data = argl[1] instanceof CharSequence ? argl[1].toString() : JSONUtil.json(argl[1])
			aa = aa.body(data)
		}
		if (request) return aa
		def response = this.request(aa)
		if (json) JSONUtil.parse(response.body)
		else if (body) response.body
		else response
	}

	def headerUp(request){
		def req = request
		if (req instanceof HttpURLConnection){
			if (client?.token) req.setRequestProperty("Authorization", client.token)
			if (!(req.requestMethod == "GET")) req.setRequestProperty("Content-Type", "application/json")
			req.setRequestProperty("User-Agent", client ? client.fullUserAgent : DiscordG.USER_AGENT)
			req
		}else if (req instanceof URL){
			headerUp(req.openConnection())
		}else if (req instanceof BaseRequest){
			if (client?.token) req = req.header("Authorization", client.token)
			if (!(req.httpRequest.httpMethod == HttpMethod.GET)) req = req.header("Content-Type", "application/json")
			req.header("User-Agent", client ? client.fullUserAgent : DiscordG.USER_AGENT)
		}else{
			throw new IllegalArgumentException("tried to add headers to a ${request.class}")
		}
	}

	def request(BaseRequest req){
		_request(req, 0)
	}
	
	private _request(BaseRequest req, int rid){
		HttpRequest ft = req.httpRequest
		String rlUrl = ft.url.replaceFirst(Pattern.quote(baseUrl), "")
		if (ratelimits[ratelimitUrl(rlUrl)]){
			int id = rid < 1 ? ratelimits[ratelimitUrl(rlUrl)].newRequest() : rid
			while (ratelimits[ratelimitUrl(rlUrl)]?.requests?.contains(id)){}
		}
		def returned = ft.asString()
		int status = returned.status
		if ((returned.headers.containsKey('X-RateLimit-Limit') &&
			returned.headers['X-RateLimit-Remaining'][0].toInteger() < 2) ||
			returned.headers.containsKey('X-RateLimit-Global') ||
			status == 429){
			boolean precaution
			def r
			Thread.start {
				if (ratelimits[ratelimitUrl(rlUrl)]) return
				def js = returned.body ? JSONUtil.parse(returned.body) : [:]
				precaution = !js.containsKey('retry-after')
				client.log.debug precaution ? 
					"Ratelimited when trying to $ft.httpMethod to $ft.url" :
					"Precautioning a ratelimit for $ft.httpMethod $ft.url",
					client.log.name + "HTTP"
				RateLimit rl = new RateLimit(client, precaution ?
					[global: false, retry_after: 
						Math.abs((returned.headers['X-RateLimit-Reset'][0].toLong() * 1000) -
						System.currentTimeMillis()), message: 'Precautionary ratelimit'] : js)
				ratelimits[ratelimitUrl(rlUrl)] = rl
				int xxx = ratelimits[ratelimitUrl(rlUrl)].newRequest()
				if (!precaution) Thread.start {
					r = _request(req, xxx)
				}
				while (rl.requests){
					Thread.sleep(rl.retryTime)
					rl.requests.removeAll(rl.requests.sort().take(
						returned.headers['X-RateLimit-Limit'][0].toInteger() - 1))
					// remove 1 from the limit just to be safe
				}
				ratelimits.remove(ratelimitUrl(rlUrl))
			}
			if (precaution) return returned
			while (null == r){}
			return r
		}else if (status == 400){
			throw new BadRequestException(ft.url, JSONUtil.parse(returned.body))
		}else if (status == 401){
			throw new InvalidTokenException(ft.url, JSONUtil.parse(returned.body))
		}else if (status == 403){
			throw new NoPermissionException(ft.url, JSONUtil.parse(returned.body))
		}else if (status == 404){
			throw new NotFoundException(ft.url, JSONUtil.parse(returned.body))
		}else if (status == 405){
			client.log.warn "$ft.httpMethod not allowed for $ft.url. Report to hlaaf", client.log.name + "HTTP"
		}else if (status == 502){
			if (client.retryOn502){
				return request(req)
			}else{
				throw new HTTP5xxException(ft.url, JSONUtil.parse(returned.body))
			}
		}else if (status.intdiv(100) == 5){
			throw new HTTP5xxException(ft.url, JSONUtil.parse(returned.body))
		}else if (status.intdiv(100) >= 3){
			client.log.warn "Got status code $status while ${ft.httpMethod}ing to $ft.url, this isn't an error but just a warning.", client.log.name + "HTTP"
		}
		returned
	}

	@Memoized
	static String ratelimitUrl(String url){
		if (url.indexOf("?") > 0){
			url = url.substring(url.indexOf("?"))
		}
		int i = 0
		url.replaceAll(/\/\d+\//){ "/@${++i}/" }
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


