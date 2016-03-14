package io.github.hlaaftana.discordg.objects

import io.github.hlaaftana.discordg.util.ConversionUtil

class Integration extends DiscordObject {
	Integration(Client client, Map object){ super(client, object) }

	int getSubscriberCount(){ return this.object["subscriber_count"] }
	boolean isSyncing(){ return this.object["syncing"] }
	boolean isEnableEmoticons(){ return this.object["enable_emoticons"] }
	int getExpireBehaviour(){ return this.object["expire_behaviour"] }
	User getUser(){ return new User(client, this.object["user"]) }
	DiscordObject getAccount(){ return new DiscordObject(client, this.object["account"]) }
	boolean isEnabled(){ return this.object["enabled"] }
	Role getRole(){ return client.roles.find { this.object["role_id"] == it.id } }
	String getRawSyncTime(){ return this.object["synced_at"] }
	Date getSyncTime(){ return ConversionUtil.fromJsonDate(this.object["synced_at"]) }
	String getType(){ return this.object["type"] }
}

