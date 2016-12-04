package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class Maintenance extends MapObject {
	Maintenance(Map object){ super(object) }

	String getName(){ object["name"] }
	String getStatus(){ object["status"] }
	String getRawUpdatedAt(){ object["updated_at"] }
	Date getUpdatedAt(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
	String getRawCreatedAt(){ object["created_at"] }
	Date getCreatedAt(){ ConversionUtil.fromJsonDate(object["created_at"]) }
	String getShortlink(){ object["shortlink"] }
	String getRawScheduleStart(){ object["scheduled_for"] }
	Date getScheduleStart(){ ConversionUtil.fromJsonDate(object["scheduled_for"]) }
	String getRawScheduleEnd(){ object["scheduled_until"] }
	Date getScheduleEnd(){ ConversionUtil.fromJsonDate(object["scheduled_until"]) }
	String getId(){ object["id"] }
	String getPageId(){ object["page_id"] }
	String getImpact(){ object["impact"] }
	List<IncidentUpdate> getIncidentUpdates(){ object["incident_updates"].collect { new IncidentUpdate(it) } }
	List<IncidentUpdate> getUpdates(){ object["incident_updates"].collect { new IncidentUpdate(it) } }
}
