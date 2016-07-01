package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;
import hlaaftana.discordg.conn.Requester
import hlaaftana.discordg.util.JSONable
import java.util.Date;

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
class DiscordObject extends APIMapObject implements Comparable {
	String concatUrl = ""
	Requester requester
	DiscordObject(Client client, Map object, String concatUrl = ""){ super(client, object); this.concatUrl = concatUrl; setClient(client) }
	void setClient(Client other){ super.setClient(other); if (client) requester = new Requester(client.requester, concatUrl) }
	String getId(){ object["id"] }
	String getName(){ object["name"] }
	String toString(){ name }
	String inspect(){ "'$name' ($id)" }
	Date getCreateTime(){ new Date(createTimeMillis) }
	long getCreateTimeMillis(){ ((Long.parseLong(id) >> 22) + (1420070400000 as long)) as long }

	static forId(String id, Class<? extends DiscordObject> clazz = this.class){
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
