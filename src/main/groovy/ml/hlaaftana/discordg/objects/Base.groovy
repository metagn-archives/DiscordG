package ml.hlaaftana.discordg.objects

/**
 * A basic Discord object.
 * @author Hlaaftana
 */
class Base {
	API api
	Map object
	/**
	 * A Discord object with a map containing data and an API object to use.
	 * @param api - the API object.
	 * @param object - the map to use.
	 */
	Base(API api, Map object){ this.object = object; this.api = api }
	/**
	 * @return the ID of the object.
	 */
	String getId(){ return object["id"] }
	/**
	 * @return the name of the object.
	 */
	String getName(){ return object["name"] }
	String toString(){ return this.name }
	boolean equals(Base other){ return this.id == other.id }
}
