package hlaaftana.discordg.oauth

import hlaaftana.discordg.objects.*

class BotAccount extends User {
	BotAccount(Client client, Map object){ super(client, object) }

	String getToken(){ return this.object["token"] }
}
