package ml.hlaaftana.discordg.objects

import ml.hlaaftana.discordg.util.ConversionUtil

class Integration extends DiscordObject {
	Integration(API api, Map object){ super(api, object) }

	int getSubscriberCount(){ return this.object["subscriber_count"] }
	boolean isSyncing(){ return this.object["syncing"] }
	boolean isEnableEmoticons(){ return this.object["enable_emoticons"] }
	int getExpireBehaviour(){ return this.object["expire_behaviour"] }
	User getUser(){ return new User(api, this.object["user"]) }
	DiscordObject getAccount(){ return new DiscordObject(api, this.object["account"]) }
	boolean isEnabled(){ return this.object["enabled"] }
	Role getRole(){ return api.client.roles.find { this.object["role_id"] == it.id } }
	String getRawSyncTime(){ return this.object["synced_at"] }
	Date getSyncTime(){ return ConversionUtil.fromJsonDate(this.object["synced_at"]) }
	String getType(){ return this.object["type"] }
}

