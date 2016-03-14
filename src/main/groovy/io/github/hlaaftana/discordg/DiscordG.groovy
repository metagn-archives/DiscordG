package io.github.hlaaftana.discordg

import io.github.hlaaftana.discordg.objects.Client
import io.github.hlaaftana.discordg.util.Log
import io.github.hlaaftana.discordg.util.MiscUtil

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
		return client
	}
}
