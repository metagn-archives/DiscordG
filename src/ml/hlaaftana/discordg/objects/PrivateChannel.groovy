package ml.hlaaftana.discordg.objects

import java.util.Map

class PrivateChannel extends TextChannel {
	PrivateChannel(API api, Map object){
		super(api, object)
	}
	User getUser(){ return new User(api, object["recipient"] )}
	Server getServer(){ return null }
	String getTopic(){ return null }
	String getPosition(){ return null }
	String getType(){ return "text" }
}
