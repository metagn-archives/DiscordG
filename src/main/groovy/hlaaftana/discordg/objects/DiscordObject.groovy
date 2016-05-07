package hlaaftana.discordg.objects

import java.util.Date;

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
class DiscordObject extends APIMapObject {
	/**
	 * A Discord object with a map containing data and an API object to use.
	 * @param client - the API object.
	 * @param object - the map to use.
	 */
	DiscordObject(Client client, Map object){ super(client, object) }
	/**
	 * @return the ID of the object.
	 */
	String getId(){ return this.object["id"] }
	/**
	 * @return the     of the object.
	 */
	String getName(){ return this.object["name"] }
	String toString(){ return this.name }
	/**
	 * @return when the thing was created. Deduces it from the ID of the thing.
	 */
	Date getCreateTime(){ return new Date(this.createTimeMillis) }
	long getCreateTimeMillis(){ return ((Long.parseLong(this.id) >> 22) + (1420070400000 as long)) as long }
	static forId(String id, Class<? extends DiscordObject> clazz= this.class){ return new DiscordObject(null, [id: id]) }
	static findWithId(def withId, List coll){
		return coll.find { it.id == withId }
	}
	static resolveId(def thing){
		try {
			return thing.id
		}catch (ex){
			return thing.toString()
		}
	}
	def get(List collection, String propertyName, def value){
		return collection.find { it.getProperty(propertyName) == value }
	}
	def get(String collPropName, String propName, def value){
		return this.getProperty("collPropName").find { it.getProperty(propName) == value }
	}
	DiscordObject swapClient(Client newClient){
		def oldNotClient = this
		oldNotClient.client = newClient
		return oldNotClient // this is the spiritually longest and guiltiest method i have written in the entire lib
	}
	boolean isCase(def other){ return this.id.isCase(other) }
	boolean equals(def other){ return this.id == other.id }
	def asMap(){
		def getters = this.metaClass.methods.findAll { it.name.startsWith("get") || it.name.startsWith("is") }.collect { this.&"$it.name" }
		Map map = [:]
		getters.each { map[it.name.startsWith("get") ? it.name[3].toLowerCase() + it.name.substring(4) : it.name[2].toLowerCase() + it.name.substring(3)] = it() }
		return map
	}
}
