package hlaaftana.discordg.objects

@groovy.transform.InheritConstructors
class BotAccount extends User {
	String getToken(){ object["token"] }
}
