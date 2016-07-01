package hlaaftana.discordg.objects

import java.util.Map

/**
 * A private channel. Extends TextChannel, however some getters always return null.
 * @author Hlaaftana
 */
@groovy.transform.InheritConstructors
class PrivateChannel extends TextChannel {
	User getUser(){ new User(client, object["recipient"] )}
	Server getServer(){ null }
	String getTopic(){ null }
	int getPosition(){ null }
	String getType(){ "text" }
}
