package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.net.HTTPClient
import hlaaftana.discordg.util.JSONable
import hlaaftana.discordg.util.CasingType
import hlaaftana.discordg.util.ConversionUtil

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
class DiscordObject implements Comparable, JSONable {
	Client client
	Map object
	DiscordObject(Client c, Map o){
		client = c
		object = o
	}

	InputStream inputStreamFromDiscord(url){
		url.toURL().newInputStream(requestProperties:
			["User-Agent": client.fullUserAgent, Accept: "*/*"])
	}

	File downloadFileFromDiscord(url, file){
		File f = file as File
		f.withOutputStream { out ->
			out << inputStreamFromDiscord(url)
			new File(f.path)
		}
	}

	Map getRawObject(){
		object.collectEntries { k, v -> [(k): v instanceof DiscordListCache ?
			v.mapList : v] }
	}

	Map getPatchableObject(){
		object.findAll { k, v -> !(v instanceof DiscordListCache) }
	}

	String getId(){ object["id"] }
	String getName(){ object["name"] }
	String toString(){ name }
	String inspect(){ "'$name' ($id)" }
	Date getCreatedAt(){ new Date(createdAtMillis) }
	long getCreatedAtMillis(){ idToMillis(id) }

	static long idToMillis(id){
		(Long.parseLong(this.id(id)) >> 22) + 1420070400000L
	}

	static String millisToId(long ms, boolean raised = false){
		((ms - 1420070400000L) << 22) + (raised ? 1 << 22 : 0)
	}

	static String mentionToId(String mention){
		mention.substring(Character.isDigit(mention[2] as char) ? 2 : 3, mention.length() - 1)
	}
	
	static boolean isId(x){
		try{
			x.toString().toCharArray().every(Character.&isDigit)
		}catch (ex){
			false
		}
	}
	
	static boolean isMention(x){
		def y = x.toString()
		y[0] == '<' && y[1] in ['@', '#'] && (Character.isDigit(y[2] as char) ||
			y[2] in ['&', '!', ]) && y[y.length() - 1] == '>'
	}
	
	static forId(String id){
		new DiscordObject(null, [id: id])
	}

	static Map patchData(Map data, ...imageKeys = ["avatar", "icon"]){
		Map a = (Map) data.clone()
		a = a.collectEntries { k, v -> [(CasingType.CAMEL.convert(k, CasingType.SNAKE)): v] }
		imageKeys*.toString().each { String key ->
			if (a.containsKey(key)){
				if (ConversionUtil.isImagable(a[key])){
					a[key] = ConversionUtil.encodeImage(a[key])
				}else{
					throw new IllegalArgumentException("$key cannot be resolved " +
						"for class ${data[key].getClass()}")
				}
			}
		}
		a
	}

	static find(Collection ass, value){
		String bong = id(value)
		if (!bong) return null
		if (isId(bong)){
			def x = findId(ass, bong)
			x ? x : findName(ass, bong)
		}else{
			findName(ass, bong)
		}
	}

	static find(Collection ass, Map idMap, value){
		String bong = id(value)
		if (!bong) return null
		if (isId(bong) && idMap.containsKey(bong)){
			idMap[bong]
		}else{
			findName(ass, bong)
		}
	}

	static find(DiscordListCache cache, value){
		String a = id(value)
		if (!a) return null
		def b = isId(a) && cache.containsKey(a) ? cache[a] : findName(cache, a)
		b ? cache.class_.newInstance(cache.client, b) : null
	}

	static findAll(DiscordListCache cache, value){
		String a = id(value)
		if (!a) return null
		def b = isId(a) && cache.containsKey(a) ? [cache[a]] : findAllName(cache, a)
		b ? b.collect { cache.class_.newInstance(cache.client, it) } : []
	}

	static findNested(DiscordListCache cache, name, value){
		String x = id(value)
		if (!x) return null
		boolean a = isId(x)
		for (g in cache.values()){
			def c = g[name]
			if (!c) return null
			if (a && c.containsKey(x)) return c.class_.newInstance(
				cache.client, c[x])
			else {
				def y = findName(c.values(), x)
				if (y) return c.class_.newInstance(cache.client, y)
			}
		}
		null
	}

	static findAllNested(DiscordListCache cache, name, value){
		String x = id(value)
		if (!x) return []
		boolean a = isId(x)
		def d = []
		for (g in cache.values()){
			def c = g[name]
			if (!c) return []
			if (a && c.containsKey(x)) d.add(
				c.class_.newInstance(cache.client, c[x]))
			else {
				def y = findName(c.values(), x)
				if (y) d.add(c.class_.newInstance(cache.client, y))
			}
		}
		d
	}

	static String resolveId(thing){
		if (null == thing) null
		else if (thing instanceof String){
			if (isMention(thing)) mentionToId(thing)
			else thing
		}else{
			try{
				thing.id
			}catch (ex){
				thing.toString()
			}
		}
	}

	static String id(thing){ resolveId(thing) }

	static find(Collection ass, String propertyName, value){
		ass.find { it.getProperty(propertyName) == value }
	}
	
	private static Closure nameClosure = { o, v ->
		def bool = false
		if (o.containsKey('username')) bool |= o.username == v
		if (o.containsKey('name')) bool |= o.name == v
		if (o.containsKey('nick')) bool |= o.nick == v
		if (o.containsKey('user') && o.user.containsKey('username'))
			bool |= o.user.username == v
		bool
	}

	static findName(Collection ass, value){
		ass.find(nameClosure.rcurry(value))
	}

	static findName(DiscordListCache cache, value){
		cache.mapList.find(nameClosure.rcurry(value))
	}

	static findAllName(DiscordListCache cache, value){
		cache.mapList.findAll(nameClosure.rcurry(value))
	}

	static findId(Collection ass, value){
		ass.find { it.id == value }
	}

	DiscordObject swapClient(Client newClient){
		def oldNotClient = this
		oldNotClient.client = newClient
		oldNotClient
	}

	boolean isCase(other){ id.isCase(id(other)) }
	boolean equals(other){ id == id(other) }
	def asMap(){
		def getters = metaClass.methods.findAll { it.name.startsWith("get") || it.name.startsWith("is") }.collect { this.&"$it.name" }
		Map map = [:]
		getters.each { map[it.name.startsWith("get") ? it.name[3].toLowerCase() + it.name.substring(4) : it.name[2].toLowerCase() + it.name.substring(3)] = it() }
		map
	}
	int hashCode(){
		id.hashCode()
	}
	/// compares creation dates
	int compareTo(other){
		id.toLong() <=> id(other).toLong()
	}

	def json() {
		rawObject
	}
}
