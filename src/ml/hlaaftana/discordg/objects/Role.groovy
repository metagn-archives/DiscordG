package ml.hlaaftana.discordg.objects

class Role extends Base{
	Role(API api, Map object){
		super(api, object)
	}

	int getColor(){ return object["color"] }
	boolean isHoist(){ return object["hoist"] }
	boolean isManaged(){ return object["managed"] }
	int getPermissions(){ return object["permissions"] }
	int getPosition(){ return object["position"] }
}
