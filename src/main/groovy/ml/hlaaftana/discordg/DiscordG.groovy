package ml.hlaaftana.discordg

import ml.hlaaftana.discordg.objects.Client
import ml.hlaaftana.discordg.util.Log
import ml.hlaaftana.discordg.util.MiscUtil

class DiscordG {
	/**
	 * Instantiates a client object and logs in with basic setup.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 * @return the object.
	 */
	static Client withLogin(String email, String password){
		Client client = new Client()
		client.login(email, password)
		while (!client.loaded){}
		client.servers*.requestMembers()
		return client
	}
}
