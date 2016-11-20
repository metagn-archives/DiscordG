package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class Maintenance extends MapObject {
	Maintenance(Map object){ super(object) }

	String getName(){ object["name"] }
	String getStatus(){ object["status"] }
	String getRawUpdateTime(){ object["updated_at"] }
	Date getUpdateTime(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
	String getRawCreateTime(){ object["created_at"] }
	Date getCreateTime(){ ConversionUtil.fromJsonDate(object["created_at"]) }
	String getShortlink(){ object["shortlink"] }
	String getRawScheduleStartTime(){ object["scheduled_for"] }
	Date getScheduleStartTime(){ ConversionUtil.fromJsonDate(object["scheduled_for"]) }
	String getRawScheduleEndTime(){ object["scheduled_until"] }
	Date getScheduleEndTime(){ ConversionUtil.fromJsonDate(object["scheduled_until"]) }
	String getId(){ object["id"] }
	String getPageId(){ object["page_id"] }
	String getImpact(){ object["impact"] }
	List<IncidentUpdate> getIncidentUpdates(){ object["incident_updates"].collect { new IncidentUpdate(it) } }
	List<IncidentUpdate> getUpdates(){ object["incident_updates"].collect { new IncidentUpdate(it) } }
}
