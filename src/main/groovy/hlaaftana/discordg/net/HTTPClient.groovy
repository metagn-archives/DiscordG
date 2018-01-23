package hlaaftana.discordg.net

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.request.GetRequest
import com.mashape.unirest.request.HttpRequestWithBody
import groovy.transform.CompileStatic

import java.util.regex.Pattern

import groovy.transform.Memoized
import hlaaftana.discordg.Client
import hlaaftana.discordg.DiscordG
import hlaaftana.discordg.exceptions.*
import hlaaftana.discordg.util.JSONUtil

import static hlaaftana.discordg.util.CasingType.*

import com.mashape.unirest.http.Unirest
import com.mashape.unirest.request.BaseRequest
import com.mashape.unirest.request.HttpRequest

/**
 * Provides ways to use the REST API for Discord.
 * @author Hlaaftana
 */
@CompileStatic
class HTTPClient {
	static String discordApi = 'https://discordapp.com/api/'
	static String canaryApi = 'https://canary.discordapp.com/api/'
	static String ptbApi = 'https://ptb.discordapp.com/api/'
	static String defaultApi = discordApi + 'v6/'
	static String latestApi = defaultApi
	static final Map<Integer, String> ERROR_CODES = [
		10001: 'Unknown Account',
		10002: 'Unknown Application',
		10003: 'Unknown Channel',
		10004: 'Unknown Guild',
		10005: 'Unknown Integration',
		10006: 'Unknown Invite',
		10007: 'Unknown Member',
		10008: 'Unknown Message',
		10009: 'Unknown Overwrite',
		10010: 'Unknown Provider',
		10011: 'Unknown Role',
		10012: 'Unknown Token',
		10013: 'Unknown User',
		20001: 'Bots cannot use this endpoint',
		20002: 'Only bots can use this endpoint',
		30001: 'Maximum number of guilds reached (100)',
		30002: 'Maximum number of friends reached (1000)',
		40001: 'Unauthorized',
		50001: 'Missing Access',
		50002: 'Invalid Account Type',
		50003: 'Cannot execute action on a DM channel',
		50004: 'Embed Disabled',
		50005: 'Cannot edit a message authored by another user',
		50006: 'Cannot send an empty message',
		50007: 'Cannot send messages to this user',
		50008: 'Cannot send messages in a voice channel',
		50009: 'Channel verification level is too high',
		50010: 'OAuth2 application does not have a bot',
		50011: 'OAuth2 application limit reached',
		50012: 'Invalid OAuth State',
		50013: 'Missing Permissions',
		50014: 'Invalid authentication token',
		50015: 'Note is too long',
		50016: 'Provided too few or too many messages to delete. Must provide at least 2 and fewer than 100 messages to delete.',
	].asImmutable()
	String baseUrl = defaultApi
	Map<String, RateLimit> ratelimits = [:].asSynchronized()
	Client client

	HTTPClient(Client client, String concatUrl = ''){
		this.client = client
		if (concatUrl) baseUrl = concatUrlPaths(baseUrl, concatUrl)
	}

	HTTPClient(HTTPClient http, String concatUrl = ''){
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
		methodMissing(camel.to(constant, a[0]), a.length == 2 ? a[1] : [a[1], a[2]])
	}

	def get(...args) { methodMissing('get', args) }
	def post(...args) { methodMissing('post', args) }
	def put(...args) { methodMissing('put', args) }
	def patch(...args) { methodMissing('patch', args) }
	def delete(...args) { methodMissing('delete', args) }
	Map<String, Object> jsonGet(...args) { (Map<String, Object>) methodMissing('jsonGet', args) }
	Map<String, Object> jsonPut(...args) { (Map<String, Object>) methodMissing('jsonPut', args) }
	Map<String, Object> jsonPost(...args) { (Map<String, Object>) methodMissing('jsonPost', args) }
	Map<String, Object> jsonPatch(...args) { (Map<String, Object>) methodMissing('jsonPatch', args) }
	List<Map<String, Object>> jsonGets(...args) { (List<Map<String, Object>>) methodMissing('jsonGet', args) }
	List<Map<String, Object>> jsonPuts(...args) { (List<Map<String, Object>>) methodMissing('jsonPut', args) }
	List<Map<String, Object>> jsonPosts(...args) { (List<Map<String, Object>>) methodMissing('jsonPost', args) }
	List<Map<String, Object>> jsonPatches(...args) { (List<Map<String, Object>>) methodMissing('jsonPatch', args) }

	def methodMissing(String methodName, List argl){
		String url
		List methodParams = camel.toWords(methodName)
		boolean global = methodParams.remove('global')
		boolean json = methodParams.remove('json')
		boolean body = methodParams.remove('body')
		boolean request = methodParams.remove('request')
		if (global) url = argl[0]
		else url = concatUrlPaths(baseUrl, argl[0].toString())
		BaseRequest aa = headerUp(startreq(methodParams[0].toString(), url))
		if (argl.size() > 1) {
			def data = argl[1] instanceof CharSequence ? argl[1].toString() : JSONUtil.json(argl[1])
			aa = ((HttpRequestWithBody) aa).body(data)
		}
		if (request) return aa
		HttpResponse<String> response = this.request(aa)
		if (json) JSONUtil.parse(response.body)
		else if (body) response.body
		else response
	}

	def methodMissing(String methodName, Object[] args){
		methodMissing(methodName, args.toList())
	}

	def methodMissing(String methodName, args){
		methodMissing(methodName, [args])
	}

	private static HttpRequest startreq(String method, String url) {
		if (method == 'get') Unirest.get(url)
		else if (method == 'head') Unirest.head(url)
		else if (method == 'post') Unirest.post(url)
		else if (method == 'delete') Unirest.delete(url)
		else if (method == 'put') Unirest.put(url)
		else if (method == 'options') Unirest.options(url)
		else if (method == 'patch') Unirest.patch(url)
		else throw new UnsupportedOperationException('Unirest does not support HTTP method '.concat(method))
	}

	BaseRequest headerUp(HttpRequest req){
		if (null != client.token) req = req.header('Authorization', client.token)
		if (!(req instanceof GetRequest)) req = req.header('Content-Type', 'application/json')
		req.header('User-Agent', null != client ? client.fullUserAgent : DiscordG.USER_AGENT)
	}

	HttpURLConnection headerUp(HttpURLConnection req) {
		if (null != client.token) req.setRequestProperty('Authorization', client.token)
		if (!(req.requestMethod == 'GET')) req.setRequestProperty('Content-Type', 'application/json')
		req.setRequestProperty('User-Agent', null != client ? client.fullUserAgent : DiscordG.USER_AGENT)
		req
	}

	HttpURLConnection headerUp(URL url) {
		headerUp((HttpURLConnection) url.openConnection())
	}

	HttpResponse<String> request(BaseRequest req){
		_request(req, 0)
	}
	
	private HttpResponse<String> _request(BaseRequest req, int rid){
		HttpRequest ft = req.httpRequest
		String rlUrl = ft.url.replaceFirst(Pattern.quote(baseUrl), '')
		if (ratelimits[ratelimitUrl(rlUrl)]){
			int id = rid < 1 ? ratelimits[ratelimitUrl(rlUrl)].newRequest() : rid
			while (ratelimits[ratelimitUrl(rlUrl)]?.requests?.contains(id)) Thread.sleep 10
		}
		def returned = ft.asString()
		int status = returned.status
		if ((returned.headers.containsKey('X-RateLimit-Limit') &&
			returned.headers['X-RateLimit-Remaining'][0].toInteger() < 2) ||
			returned.headers.containsKey('X-RateLimit-Global') ||
			status == 429){
			boolean precaution
			HttpResponse<String> r
			Thread.start {
				if (ratelimits[ratelimitUrl(rlUrl)]) return
				def js = returned.body ? (Map) JSONUtil.parse(returned.body) : [:]
				precaution = !js.containsKey('retry-after')
				client.log.debug precaution ? 
					"Ratelimited when trying to $ft.httpMethod to $ft.url" :
					"Precautioning a ratelimit for $ft.httpMethod $ft.url",
					client.log.name + 'HTTP'
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
			while (null == r) Thread.sleep 10
			return r
		} else if (status == 400) throw new BadRequestException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status == 401) throw new InvalidTokenException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status == 403) throw new NoPermissionException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status == 404) throw new NotFoundException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status == 405) client.log
			.warn "$ft.httpMethod not allowed for $ft.url. Report to hlaaf", client.log.name + 'HTTP'
		else if (status == 502)
			if (client.retryOn502) return request(req)
			else throw new HTTP5xxException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status.intdiv(100) == 5) throw new HTTP5xxException(ft.url, (Map<String, Object>) JSONUtil.parse(returned.body))
		else if (status.intdiv(100) >= 3) client.log
			.warn "Got status code $status while ${ft.httpMethod}ing to $ft.url, " +
				"this isn't an error but just a warning.', client.log.name + 'HTTP"
		returned
	}

	@Memoized
	static String ratelimitUrl(String url){
		if (url.indexOf('?') > 0) url = url.substring(url.indexOf('?'))
		// AAAAAA url.replaceAll(/\/\d+\//) { ...full -> "/@${++i}/" }
		StringBuilder result = new StringBuilder()
		def arr = url.split(/\/\d+\//)
		result.append(arr[0])
		for (int i = 1; i < arr.length; ++i) {
			result.append('/@').append(i).append('/').append(arr[i])
		}
		result.toString()
	}

	@Memoized
	static String concatUrlPaths(String...yeah){
		StringBuilder ass = new StringBuilder(yeah[0])
		def whoop = yeah.drop(1)
		for (int i = 0; i < whoop.length; ++i) {
			def a = whoop[i]
			if (a.empty) continue
			boolean y = ass.charAt(0) == ((char) '/')
			if (ass.charAt(ass.length() - 1) == ((char) '/')) {
				if (y) ass.append a.substring(1)
				else ass.append a
			}
			else if (y) ass.append a
			else ass.append'/'append a
		}
		ass
	}
}


