package hlaaftana.discordg.objects

import hlaaftana.discordg.util.ConversionUtil
import hlaaftana.discordg.util.JSONUtil

class Integration extends DiscordObject {
	Integration(Client client, Map object){ super(client, object) }

	int getSubscriberCount(){ return this.object["subscriber_count"] }
	boolean isSyncing(){ return this.object["syncing"] }
	boolean isEnableEmoticons(){ return this.object["enable_emoticons"] }
	int getExpireBehaviour(){ return this.object["expire_behaviour"] }
	int getExpireGracePeriod(){ return this.object["expire_grace_period"] }
	User getUser(){ return new User(client, this.object["user"]) }
	DiscordObject getAccount(){ return new DiscordObject(client, this.object["account"]) }
	boolean isEnabled(){ return this.object["enabled"] }
	Role getRole(){ return client.roles.find { this.object["role_id"] == it.id } }
	Server getServer(){ return this.role.server }
	String getRawSyncTime(){ return this.object["synced_at"] }
	Date getSyncTime(){ return ConversionUtil.fromJsonDate(this.object["synced_at"]) }
	String getType(){ return this.object["type"] }
	Integration edit(Map data){
		return new Integration(JSONUtil.parse(client.requester.patch("guilds/${this.server.id}/integrations/${this.id}", data)))
	}
	void delete(){
		client.requester.delete("guilds/${this.server.id}/integrations/${this.id}")
	}
	void sync(){
		client.requester.post("guilds/${this.server.id}/integrations/${this.id}/sync", [:])
	}
}

