package io.github.hlaaftana.discordg.objects

class APIMapObject extends MapObject {
	Client client
	APIMapObject(Client client, Map object){ super(object); this.client = client }
}
