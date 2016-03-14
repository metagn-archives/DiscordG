package io.github.hlaaftana.discordg.objects

import java.util.Map

/**
 * A private channel. Extends TextChannel, however some getters always return null.
 * @author Hlaaftana
 */
class PrivateChannel extends TextChannel {
	PrivateChannel(Client client, Map object){
		super(client, object)
	}
	/**
	 * @return the user the conversation is with.
	 */
	User getUser(){ return new User(client, this.object["recipient"] )}
	Server getServer(){ return null }
	String getTopic(){ return null }
	String getPosition(){ return null }
	String getType(){ return "text" }
}
