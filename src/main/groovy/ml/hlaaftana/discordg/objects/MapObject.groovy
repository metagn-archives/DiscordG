package ml.hlaaftana.discordg.objects

/**
 * A basic object with map data.
 * @author Hlaaftana
 */
class MapObject {
	Map object
	/**
	 * An object with a map containing data.
	 * @param object - the map to use.
	 */
	MapObject(Map object){ this.object = object }
	String toString(){ return this.object.toString() }
	boolean equals(def other){ return this.object == other.object }
}

