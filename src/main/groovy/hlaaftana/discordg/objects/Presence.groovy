package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class Presence extends DiscordObject {
	Game getGame(){ object["game"] ? new Game(client, object["game"]) : null }
	String getStatus(){ object["status"] }
	String getId(){ object["user"]["id"] }
	Server getServer(){ client.serverMap[object["guild_id"]] }
	Server getParent(){ server }
	Member getMember(){ server ? server.memberMap[id] : client.members(id)[0] }
	String getName(){ member.name }

	@groovy.transform.InheritConstructors
	static class Game extends DiscordObject {
		String getId(){ object["type"].toString() ?: "0" }
		GameTypes getType(){ GameTypes.get(rawType) }
		int getRawType(){ object["type"] ?: 0 }
		String getUrl(){ object["url"] }
		String toString(){ type == 0 ? name : "$name ($url)" }
	}

	static enum GameTypes {
		PLAYING(0),
		STREAMING(1)

		int id
		GameTypes(int id){ this.id = id }

		static GameTypes get(d){
			d instanceof GameTypes ? d : GameTypes.values().find { it.id == d }
		}
	}
}
