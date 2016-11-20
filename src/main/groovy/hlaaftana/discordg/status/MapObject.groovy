package hlaaftana.discordg.status

/**
 * A basic object with map data.
 * @author Hlaaftana
 */
class MapObject {
	Map object
	MapObject(Map object){ this.object = object }
	String toString(){ object.toString() }
	boolean equals(other){ object == other.object }
}

