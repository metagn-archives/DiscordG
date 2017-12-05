package hlaaftana.discordg

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import hlaaftana.discordg.collections.DiscordListCache
import hlaaftana.discordg.util.JSONable
import hlaaftana.discordg.util.CasingType
import hlaaftana.discordg.util.ConversionUtil

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
@CompileStatic
class DiscordObject implements Comparable, JSONable {
	Client client
	Map object
	DiscordObject(Client c, Map o){
		client = c
		object = o
	}

	InputStream inputStreamFromDiscord(String url){
		url.toURL().newInputStream(requestProperties:
			['User-Agent': client.fullUserAgent, Accept: '*/*'])
	}

	File downloadFileFromDiscord(String url, file){
		File f = file as File
		f.withOutputStream { out ->
			out << inputStreamFromDiscord(url)
			new File(f.path)
		}
		f
	}

	Map getRawObject(){
		def x = new HashMap(object.size())
		for (e in x) {
			x.put(e.key, e.value instanceof DiscordListCache ? ((DiscordListCache) e.value).rawList() : e.value)
		}
		x
	}

	Map getPatchableObject(){
		def x = new HashMap(object.size())
		for (e in x) {
			if (!(e.value instanceof DiscordListCache)) x.put(e.key, e.value)
		}
		x
	}

	String getId(){ object.id.toString() }
	String getName(){ object.name.toString() }
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

	static boolean isId(long x){
		x instanceof long && x > 2000000000000000
	}

	static boolean isId(String x){
		x.isLong() && x.toLong() > 2000000000000000
	}

	static boolean isMention(String x){
		def a = x.charAt(0), b = x.charAt(1), c
		a == ((char) '<') && b == ((char) '@') || b == ((char) '#') &&
				(Character.isDigit((c = x.charAt(2))) || c == ((char) '&') || c == ((char) '!')) &&
				x.charAt(x.length() - 1) == (char) '>'
	}

	static Map patchData(Map data, ...imageKeys = ['avatar', 'icon']){
		Map a = new HashMap(data.size())
		for (e in data) {
			a.put(CasingType.CAMEL.to(CasingType.SNAKE, e.key.toString()), e.value)
		}
		for (k in imageKeys) {
			def key = k.toString()
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

	static <T> T find(Collection<T> ass, value){
		String bong = id(value)
		if (!bong) return null
		if (isId(bong)){
			findId(ass, bong) ?: findName(ass, bong)
		}else{
			findName(ass, bong)
		}
	}

	static <T> T find(Collection<T> ass, Map<String, T> idMap, value){
		String bong = id(value)
		if (!bong) return null
		if (isId(bong) && idMap.containsKey(bong)){
			idMap[bong]
		}else{
			findName(ass, bong)
		}
	}

	static <T extends DiscordObject> T find(DiscordListCache<T> cache, value){
		String a = id(value)
		if (!a) return null
		def b = isId(a) && cache.containsKey(a) ? cache[a] : findName(cache, a)
		b ? cache.class().newInstance(cache.client(), b) : null
	}

	static <T extends DiscordObject> List<T> findAll(DiscordListCache<T> cache, value){
		String a = id(value)
		if (!a) return null
		def b = isId(a) && cache.containsKey(a) ? [cache[a]] : findAllName(cache, a)
		b ? b.collect { cache.class().newInstance(cache.client(), it) } : []
	}

	static <T extends DiscordObject> T findNested(DiscordListCache cache, name, value){
		String x = id(value)
		if (!x) return null
		boolean a = isId(x)
		for (e in cache){
			def c = (DiscordListCache<T>) ((Map) e.value).get(name)
			if (null == c || c.isEmpty()) return null
			if (a && c.containsKey(x)) return c.class().newInstance(cache.client(), c[x])
			else {
				def y = findName(c.values(), x)
				if (y) return c.class().newInstance(cache.client(), y)
			}
		}
		null
	}

	static <T extends DiscordObject> List<T> findAllNested(DiscordListCache cache, name, value){
		String x = id(value)
		if (!x) return []
		boolean a = isId(x)
		def d = []
		for (e in cache){
			def c = (DiscordListCache<T>) ((Map) e.value).get(name)
			if (null == c || c.isEmpty()) return []
			if (a && c.containsKey(x)) d.add c.class().newInstance(cache.client(), c[x])
			else {
				def y = findName(c.values(), x)
				if (y) d.add c.class().newInstance(cache.client(), y)
			}
		}
		d
	}

	static String resolveId(thing){
		if (null == thing) null
		else if (thing instanceof String) isMention(thing) ? mentionToId(thing) : thing
		else if (thing instanceof DiscordObject) thing.id
		else if (thing instanceof Map) thing.id
		else try { dynamicid thing }
		catch (ignore) { thing.toString() }
	}

	@CompileDynamic
	private static String dynamicid(a) { (String) a.id }
	@CompileDynamic
	private static dynamicprop(a, String name) { a."$name" }

	static String id(thing){ resolveId(thing) }

	static <T> T find(Collection<T> ass, String propertyName, value){
		int hash = value.hashCode()
		for (x in ass) {
			def a = dynamicprop(x, propertyName)
			if (a.hashCode() == hash && a == value) return x
		}
		null
	}
	
	private static Closure nameClosure = { Map o, v ->
		def bool = false
		if (o.containsKey('username')) bool |= o.username == v
		else if (o.containsKey('name')) bool |= o.name == v
		else if (o.containsKey('nick')) bool |= o.nick == v
		else if (o.containsKey('user') && ((Map) o.user).containsKey('username'))
			bool |= ((Map) o.user).username == v
		bool
	}

	static <T> T findName(Collection<T> ass, value){
		ass.find(nameClosure.rcurry(value))
	}

	static <T extends DiscordObject> T findName(DiscordListCache<T> cache, value){
		def i = cache.rawList().find(nameClosure.rcurry(value))
		null == i ? null : cache.at(i.id)
	}

	static <T extends DiscordObject> List<T> findAllName(DiscordListCache<T> cache, value){
		def i = cache.rawList().findAll(nameClosure.rcurry(value))
		if (i.empty) return []
		def res = new ArrayList<T>(i.size())
		for (x in i) res.add cache.at(x.id)
		res
	}

	static <T> T findId(Collection<T> ass, value){
		int hash = value.hashCode()
		for (x in ass) {
			def a = dynamicid(x)
			if (a.hashCode() == hash && a == value) return x
		}
		null
	}

	DiscordObject swapClient(Client newClient){
		def oldNotClient = this
		oldNotClient.client = newClient
		oldNotClient
	}

	boolean isCase(other){ id(other) in id }
	boolean equals(other){ id == id(other) }

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
