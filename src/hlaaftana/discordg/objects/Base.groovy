package hlaaftana.discordg.objects

class Base {
	API api
	Map object
	Base(API api, Map object){ this.object = object; this.api = api }
	Map getObject(){ return object }
	String getID(){ return object["id"] }
	String getName(){ return object["name"] }
}
