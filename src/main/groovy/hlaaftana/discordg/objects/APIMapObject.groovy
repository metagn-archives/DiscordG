package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;

class APIMapObject extends MapObject {
	Client client
	APIMapObject(Client client, Map object){ super(object); this.client = client }
}
