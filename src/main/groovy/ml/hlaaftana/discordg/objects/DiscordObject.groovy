package ml.hlaaftana.discordg.objects

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
class DiscordObject extends APIMapObject {
	/**
	 * A Discord object with a map containing data and an API object to use.
	 * @param api - the API object.
	 * @param object - the map to use.
	 */
	DiscordObject(API api, Map object){ super(api, object) }
	/**
	 * @return the ID of the object.
	 */
	String getId(){ return this.object["id"] }
	/**
	 * @return the name of the object.
	 */
	String getName(){ return this.object["name"] }
	String toString(){ return this.name }
	boolean equals(def other){ return this.id == other.id }
}
