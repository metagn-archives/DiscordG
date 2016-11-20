package hlaaftana.discordg.status

import hlaaftana.discordg.util.JSONSimpleHTTP

class DiscordStatus {
	static Schedule getActiveSchedule(){ new Schedule(JSONSimpleHTTP.get("https://status.discordapp.com/api/v2/scheduled-maintenances/active.json")) }
	static Schedule getUpcomingSchedule(){ new Schedule(JSONSimpleHTTP.get("https://status.discordapp.com/api/v2/scheduled-maintenances/upcoming.json")) }
}
