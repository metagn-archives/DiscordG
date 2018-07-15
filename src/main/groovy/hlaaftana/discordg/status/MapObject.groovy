package hlaaftana.discordg.status

import groovy.transform.CompileStatic

@CompileStatic
class MapObject {
	Map object
	MapObject(Map object) { this.object = object }
	String toString() { object.toString() }
	boolean equals(other) { other instanceof MapObject && object == ((MapObject) other).object }
}

