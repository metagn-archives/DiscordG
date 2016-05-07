package hlaaftana.discordg.status

import hlaaftana.discordg.conn.JSONRequester

class DiscordStatus {
	static Schedule getActiveSchedule(){ return new Schedule(JSONRequester.get("https://status.discordapp.com/api/v2/scheduled-maintenances/active.json")) }
	static Schedule getUpcomingSchedule(){ return new Schedule(JSONRequester.get("https://status.discordapp.com/api/v2/scheduled-maintenances/upcoming.json")) }
}
