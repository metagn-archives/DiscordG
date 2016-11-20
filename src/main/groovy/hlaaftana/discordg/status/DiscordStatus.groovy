package hlaaftana.discordg.status

import hlaaftana.discordg.net.JSONRequester

class DiscordStatus {
	static Schedule getActiveSchedule(){ new Schedule(JSONRequester.get("https://status.discordapp.com/api/v2/scheduled-maintenances/active.json")) }
	static Schedule getUpcomingSchedule(){ new Schedule(JSONRequester.get("https://status.discordapp.com/api/v2/scheduled-maintenances/upcoming.json")) }
}
