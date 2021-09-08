package hlaaftana.discordg

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import hlaaftana.discordg.collections.Cache
import hlaaftana.discordg.data.Snowflake
import hlaaftana.discordg.data.Member
import hlaaftana.discordg.util.CasingType
import hlaaftana.discordg.util.ConversionUtil

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
@CompileStatic
abstract class DiscordObject implements Comparable {
	Client client

	DiscordObject(Client c) {
		client = c
	}

	DiscordObject(Client c, Map<?, ?> obj) {
		this(c)
		fill(obj)
	}

	InputStream inputStreamFromDiscord(String url) {
		url.toURL().newInputStream(requestProperties:
			['User-Agent': client.fullUserAgent, Accept: '*/*'])
	}

	File downloadFileFromDiscord(String url, file) {
		File f = file as File
		f.withOutputStream { out ->
			out << inputStreamFromDiscord(url)
			new File(f.path)
		}
		f
	}

	abstract Snowflake getId()
	abstract String getName()
	// will get replaced with annotation processor later
	abstract void jsonField(String name, value)

	/*Object jsonField(String name) {
		throw new UnsupportedOperationException("Can't get json field on ${this.class} (field $name)")
	}*/

	void fill(Map map) {
		if (null == map) return
		def f = map.id
		if (f instanceof String) jsonField('id', (String) f)
		def auth = map.author
		if (auth instanceof Map) jsonField('author', (Map) auth)
		for (e in map.entrySet()) if (e.key instanceof String) jsonField((String) e.key, e.value)
	}

	String toString() { name }
	String inspect() { "'$name' ($id)" }
	Date getCreatedAt() { new Date(createdAtMillis) }
	long getCreatedAtMillis() { id.toMillis() }


	static Map patchData(Map data, ...imageKeys = ['avatar', 'icon']) {
		def a = new HashMap(data.size())
		for (e in data) {
			a.put(CasingType.camel.to(CasingType.snake, e.key.toString()), e.value)
		}
		for (final k : imageKeys) {
			final key = k.toString()
			if (a.containsKey(key)){
				if (ConversionUtil.isImagable(a[key])){
					a[key] = ConversionUtil.encodeImageBase64(a[key])
				} else {
					throw new IllegalArgumentException("$key cannot be resolved " +
						"for class ${data[key].getClass()}")
				}
			}
		}
		a
	}

	static Map find(Collection<Map> col, value) {
		if (null == col || col.empty || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			findId(col, id)
		} catch (NumberFormatException ignored) {
			findName(col, value)
		}
	}

	static Map find(Collection<Map> col, Map<Snowflake, Map> idMap, value) {
		if (null == col || col.empty || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			idMap[id]
		} catch (NumberFormatException ignored) {
			findName(col, value)
		}
	}

	static <T extends DiscordObject> T findBuilt(Collection<T> col, value) {
		if (null == col || col.empty || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			findId(col, id)
		} catch (NumberFormatException ignored) {
			findNameBuilt(col, value)
		}
	}

	static <T extends DiscordObject> T findBuilt(Map<Snowflake, T> idMap, value) {
		if (null == idMap || idMap.isEmpty() || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			idMap[id]
		} catch (NumberFormatException ignored) {
			findNameBuilt(idMap.values(), value)
		}
	}

	static <T extends DiscordObject> T find(Cache<T> cache, value) {
		if (null == cache || cache.isEmpty() || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			cache[id]
		} catch (NumberFormatException ignored) {
			findName(cache, value)
		}
	}

	static <T extends DiscordObject> List<T> findAll(Cache<T> cache, value) {
		if (null == cache || cache.isEmpty() || null == value) return null
		try {
			final id = Snowflake.from(value)
			if (null == id) return null
			final c = cache[id]
			c ? [c] : Collections.emptyList()
		} catch (NumberFormatException ignored) {
			findAllName(cache, value)
		}
	}

	static <T extends DiscordObject> T findNested(Cache<? extends DiscordObject> cache, name, value) {
		if (null == cache || cache.isEmpty() || null == value) return null
		Snowflake id
		String n
		try {
			id = Snowflake.from(value)
		} catch (NumberFormatException ignored) {
			n = value.toString()
		}
		final prop = name.toString()
		for (e in cache.entrySet()) {
			def c = (Cache<T>) dynamicprop(e.value, prop)
			if (null == c || c.isEmpty()) return null
			if (null != id) {
				final j = c[id]
				if (null != j) return j
			} else {
				final y = findNameBuilt(c.values(), n)
				if (y) return y
			}
		}
		null
	}

	static <T extends DiscordObject> List<T> findAllNested(Cache<? extends DiscordObject> cache, name, value) {
		if (null == cache || cache.isEmpty() || null == value) return null
		Snowflake id
		String n
		try {
			id = Snowflake.from(value)
		} catch (NumberFormatException ignored) {
			n = value.toString()
		}
		final prop = name.toString()
		def result = new ArrayList<T>()
		for (e in cache.entrySet()) {
			def c = (Cache<T>) dynamicprop(e.value, prop)
			if (null == c || c.isEmpty()) continue
			if (null != id) {
				final j = c[id]
				if (null != j) result.add(j)
			} else {
				final y = findNameBuilt(c.values(), n)
				if (y) result.add(y)
			}
		}
		result
	}

	@CompileDynamic
	private static dynamicprop(a, String name) { a."$name" }

	static <T> T find(Collection<T> ass, String propertyName, value) {
		if (null == ass || ass.empty || null == value) return null
		int hash = value.hashCode()
		for (x in ass) {
			def a = dynamicprop(x, propertyName)
			if (a.hashCode() == hash && a == value) return x
		}
		null
	}

	private static final Closure<Boolean> nameClosure = { Map o, String v ->
		(o.containsKey('username') && o.username == v) ||
				(o.containsKey('name') && o.name == v)
	}

	static Map findName(Collection<Map> ass, value) {
		if (null == ass || ass.empty || null == value) return null
		ass.find(nameClosure.rcurry(value.toString()))
	}

	static <T extends DiscordObject> T findNameBuilt(Collection<T> ass, value) {
		if (!ass || null == value) return null
		final s = value.toString()
		final h = s.hashCode()
		for (c in ass) if (c.name.hashCode() == h && c.name == s) return c
		null
	}

	static <T extends DiscordObject> T findName(Cache<T> cache, value) {
		if (null == value || !cache) return null
		final s = value.toString()
		final h = s.hashCode()
		for (c in cache) if (c.name.hashCode() == h && c.name == s) return c
		null
	}

	static Member findMemberName(Cache<Member> cache, value) {
		if (null == value || !cache) return null
		final s = value.toString()
		final h = s.hashCode()
		for (c in cache)
			if ((c.user.username.hashCode() == h && c.user.username == s) ||
				(c.nick.hashCode() == h && c.nick == s))
				return c
		null
	}

	static <T extends DiscordObject> List<T> findAllName(Cache<T> cache, value) {
		if (null == value || !cache) return null
		def i = cache.list().findAll(nameClosure.rcurry(value))
		if (i.empty) return []
		def res = new ArrayList<T>(i.size())
		for (x in i) res.add cache.get(x.id)
		res
	}

	static <T> T findId(Collection<T> ass, value) {
		if (null == value || !ass) return null
		int hash = value.hashCode()
		for (x in ass) {
			def a = Snowflake.dynamicId(x)
			if (a.hashCode() == hash && a == value) return x
		}
		null
	}

	DiscordObject swapClient(Client newClient) {
		def oldNotClient = this
		oldNotClient.client = newClient
		oldNotClient
	}

	boolean isCase(other) { Snowflake.from(other) in id }
	boolean equals(other) { id == Snowflake.from(other) }

	int hashCode() {
		id.hashCode()
	}
	/// compares creation dates
	int compareTo(other) {
		id.toLong() <=> Snowflake.from(other).toLong()
	}
}
