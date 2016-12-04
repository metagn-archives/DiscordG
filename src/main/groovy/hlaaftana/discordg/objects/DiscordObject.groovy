package hlaaftana.discordg.objects

import hlaaftana.discordg.Client
import hlaaftana.discordg.collections.DiscordListCache;
import hlaaftana.discordg.net.HTTPClient
import hlaaftana.discordg.util.JSONable
import java.util.Date;

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
		(url as URL).newInputStream(requestProperties:
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
		(ms - 1420070400000L << 22) + (raised ? 1 << 22 : 0)
	}

	static forId(String id){
		new DiscordObject(null, [id: id])
	}

	static find(Collection ass, value){
		String bong = id(value)
		if (!bong) return null
		if (bong.long){
			findId(ass, bong)
		}else{
			findName(ass, bong)
		}
	}

	static find(Collection ass, Map idMap, value){
		String bong = id(value)
		if (!bong) return null
		if (bong.long){
			idMap[bong]
		}else{
			findName(ass, bong)
		}
	}

	static find(DiscordListCache cache, value){
		String a = id(value)
		if (!a) return null
		def b = a.long ? cache[a] : findName(cache, a)
		b ? cache.class_.newInstance(cache.client, b) : null
	}

	static findAll(DiscordListCache cache, value){
		String a = id(value)
		if (!a) return null
		def b = a.long ? [cache[a]] : findAllName(cache, a)
		b ? b.collect { cache.class_.newInstance(cache.client, it) } : []
	}

	static findNested(DiscordListCache cache, name, Class class_, value){
		String x = id(value)
		if (!x) return null
		boolean a = x.long
		for (g in cache.values()){
			if (a && g[name].containsKey(x)) return class_.newInstance(
				cache.client, g[name][x])
			else {
				def y = g[name].values().find { it.containsKey("username") ?
					it.username == value : it.name == value }
				if (y) return class_.newInstance(cache.client, y)
			}
		}
		null
	}

	static findAllNested(DiscordListCache cache, name, Class class_, value){
		String x = id(value)
		if (!x) return []
		boolean a = x.long
		def d = []
		for (g in cache.values()){
			if (a && g[name].containsKey(x)) d.add(
				class_.newInstance(cache.client, g[name][x]))
			else {
				def y = g[name].values().find { it.containsKey("username") ?
					it.username == value : it.name == value }
				if (y) d.add(class_.newInstance(cache.client, y))
			}
		}
		d
	}

	static String resolveId(thing){
		try {
			thing?.id
		}catch (ex){
			thing.toString()
		}
	}

	static String id(thing){ resolveId(thing) }

	static find(Collection ass, String propertyName, value){
		ass.find { it.getProperty(propertyName) == value }
	}

	static findName(Collection ass, value){
		ass.find { it.containsKey("username") ?
			it.username == value : it.name == value }
	}

	static findName(DiscordListCache cache, value){
		cache.mapList.find { it.containsKey("username") ?
			it.username == value : it.name == value }
	}

	static findAllName(DiscordListCache cache, value){
		cache.mapList.findAll { it.containsKey("username") ?
			it.username == value : it.name == value }
	}

	static findId(Collection ass, value){
		ass.find { it.id == value }
	}

	static get(Client client, thing, Class cast = DiscordObject){
		if (thing in cast) thing
		else client.everything.find { it.id == id(thing) && it in cast }
	}

	static get(Client client, thing, parent, Class cast = DiscordObject){
		if (thing in cast) thing
		else client.everything.find { it.id == id(thing) && it in cast && id(it?.parent) == id(parent) }
	}

	def get(thing, Class cast = DiscordObject){ get(client, thing, cast) }
	def get(thing, parent, Class cast = DiscordObject){ get(client, thing, parent, cast) }

	DiscordObject swapClient(Client newClient){
		def oldNotClient = this
		oldNotClient.client = newClient
		oldNotClient // this is the spiritually longest and guiltiest method i have written in the entire lib
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
		id <=> id(other)
	}

	def json() {
		rawObject
	}
}
