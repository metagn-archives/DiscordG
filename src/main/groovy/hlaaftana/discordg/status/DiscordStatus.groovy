package hlaaftana.discordg.status

import groovy.transform.CompileStatic
import hlaaftana.discordg.util.JSONSimpleHTTP

@CompileStatic
class DiscordStatus {
	static Schedule getActiveSchedule() { new Schedule((Map) JSONSimpleHTTP.get('https://status.discordapp.com/api/v2/scheduled-maintenances/active.json')) }
	static Schedule getUpcomingSchedule() { new Schedule((Map) JSONSimpleHTTP.get('https://status.discordapp.com/api/v2/scheduled-maintenances/upcoming.json')) }
}
