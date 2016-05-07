package hlaaftana.discordg.objects

class Presence extends DiscordObject {
	Presence(Client client, Map object){
		super(client, object)
	}

	Game getGame(){ return this.object["game"] ? new Game(client, this.object["game"]) : null }
	String getStatus(){ return this.object["status"] }
	String getId(){ return this.object["user"]["id"] }
	Server getServer(){ return client.serverMap[this.object["guild_id"]] }
	Member getMember(){ return this.server ? this.server.memberMap[this.id] : client.members(this.id)[0] }
	String getName(){ return this.member.name }

	static class Game extends DiscordObject {
		Game(Client client, Map object){
			super(client, object)
		}

		String getId(){ return this.object["type"].toString() ?: "0" }
		int getType(){ return this.object["type"] ?: 0 }
		String getUrl(){ return this.object["url"] }
		String toString(){ this.type == 0 ? this.name : "$name ($url)" }
	}
}
