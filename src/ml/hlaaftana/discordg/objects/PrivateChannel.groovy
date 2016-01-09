package ml.hlaaftana.discordg.objects

import java.util.Map

/**
 * A private channel. Extends TextChannel, however some getters always return null.
 * @author Hlaaftana
 */
class PrivateChannel extends TextChannel {
	PrivateChannel(API api, Map object){
		super(api, object)
	}
	/**
	 * @return the user the conversation is with.
	 */
	User getUser(){ return new User(api, object["recipient"] )}
	Server getServer(){ return null }
	String getTopic(){ return null }
	String getPosition(){ return null }
	String getType(){ return "text" }
}
