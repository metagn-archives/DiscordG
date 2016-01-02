package hlaaftana.discordg.objects

import org.json.JSONObject

class Role{
	int color
	boolean hoist
	String id
	boolean managed
	String name
	int permissions
	int position
	Role(JSONObject object){
		color = object.getInt("color"); hoist = object.getBoolean("hoist"); id = object.getString("id"); managed = object.getBoolean("managed"); name = object.getString("name"); permissions = object.getInt("permissions"); position = object.getInt("position")
	}

	String getID() {
		return id
	}

	String getName() {
		return name
	}

	int getColor() {
		return color
	}

	boolean isHoist() {
		return hoist
	}

	boolean isManaged() {
		return managed
	}

	int getPermissions() {
		return permissions
	}

	int getPosition() {
		return position
	}
}
