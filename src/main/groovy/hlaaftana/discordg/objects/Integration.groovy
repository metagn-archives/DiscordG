package hlaaftana.discordg.objects

import hlaaftana.discordg.Client;
import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.JSONUtil

class Integration extends DiscordObject {
	Integration(Client client, Map object){ super(client, object, "guilds/${client.role(object["role_id"]).object["guild_id"]}/integrations/$object.id") }

	int getSubscriberCount(){ object["subscriber_count"] }
	boolean isSyncing(){ object["syncing"] }
	boolean isEnableEmoticons(){ object["enable_emoticons"] }
	int getExpireBehaviour(){ object["expire_behaviour"] }
	int getExpireGracePeriod(){ object["expire_grace_period"] }
	User getUser(){ new User(client, object["user"]) }
	DiscordObject getAccount(){ new DiscordObject(client, object["account"]) }
	boolean isEnabled(){ object["enabled"] }
	Role getRole(){ client.roles.find { object["role_id"] == it.id } }
	Server getServer(){ role.server }
	String getRawSyncTime(){ object["synced_at"] }
	Date getSyncTime(){ ConversionUtil.fromJsonDate(object["synced_at"]) }
	String getType(){ object["type"] }
	Integration edit(Map data){
		new Integration(client, requester.jsonPatch("", data))
	}
	void delete(){
		requester.delete("")
	}
	void sync(){
		requester.post("sync")
	}
}

