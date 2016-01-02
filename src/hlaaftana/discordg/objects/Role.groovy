package hlaaftana.discordg.objects

import org.json.JSONObject

class Role extends Base{
	Role(API api, JSONObject object){
		super(api, object)
	}

	int getColor() {
		return object.getInt("color")
	}

	boolean isHoist() {
		return object.getBoolean("hoist")
	}

	boolean isManaged() {
		return object.getBoolean("managed")
	}

	int getPermissions() {
		return object.getInt("permissions")
	}

	int getPosition() {
		return position = object.getInt("position")
	}
}
