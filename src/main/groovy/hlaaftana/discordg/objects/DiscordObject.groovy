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
class DiscordObject implements Comparable {
	Client client
	Map object
	String concatUrl = ""
	HTTPClient http
	DiscordObject(Client c, Map o, String cu = ""){
		concatUrl = cu
		object = o
		setClient(c)
	}

	void setClient(Client c){
		this.client = c
		if (client) http = new HTTPClient(client.http, concatUrl)
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
	Date getCreateTime(){ new Date(createTimeMillis) }
	long getCreateTimeMillis(){ idToMillis(id) }

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
		ass.find { it.name == value }
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
}
