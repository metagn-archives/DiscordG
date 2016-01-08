package ml.hlaaftana.discordg.objects

class Base {
	API api
	Map object
	Base(API api, Map object){ this.object = object; this.api = api }
	String getId(){ return object["id"] }
	String getName(){ return object["name"] }
	String toString(){ return this.id }
}
