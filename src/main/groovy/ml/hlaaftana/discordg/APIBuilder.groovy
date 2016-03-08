package ml.hlaaftana.discordg

import ml.hlaaftana.discordg.objects.API
import ml.hlaaftana.discordg.util.Log
import ml.hlaaftana.discordg.util.MiscUtil

/**
 * Utility for instantiating the API class.
 * @author Hlaaftana
 */
class APIBuilder {
	/**
	 * Instantiates an API object and logs in with basic setup.
	 * @param email - the email to log in with.
	 * @param password - the password to log in with.
	 * @return the API object.
	 */
	static API build(String email, String password){
		API api = build()
		api.login(email, password)
		while (!api.loaded){}
		api.client.servers*.requestMembers()
		return api
	}

	/**
	 * Instantiates an API object with basic setup.
	 * @return the API object.
	 */
	static API build(){
		return new API()
	}
}
