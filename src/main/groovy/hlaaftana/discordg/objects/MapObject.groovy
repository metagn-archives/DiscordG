package hlaaftana.discordg.objects

/**
 * A basic object with map data.
 * @author Hlaaftana
 */
class MapObject {
	Map object
	MapObject(Map object){ this.object = object }
	String toString(){ object.toString() }
	boolean equals(def other){ object == other.object }
}

