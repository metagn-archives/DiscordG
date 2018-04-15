package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class Maintenance extends MapObject {
	Maintenance(Map object) { super(object) }

	String getName() { (String) object.name }
	String getStatus() { (String) object.status }
	String getRawUpdatedAt() { (String) object.updated_at }
	Date getUpdatedAt() { ConversionUtil.fromJsonDate(object.updated_at) }
	String getRawCreatedAt() { (String) object.created_at }
	Date getCreatedAt() { ConversionUtil.fromJsonDate(object.created_at) }
	String getShortlink() { (String) object.shortlink }
	String getRawScheduleStart() { (String) object.scheduled_for }
	Date getScheduleStart() { ConversionUtil.fromJsonDate(object.scheduled_for) }
	String getRawScheduleEnd() { (String) object.scheduled_until }
	Date getScheduleEnd() { ConversionUtil.fromJsonDate(object.scheduled_until) }
	String getId() { (String) object.id }
	String getPageId() { (String) object.page_id }
	String getImpact() { (String) object.impact }
	List<IncidentUpdate> getIncidentUpdates() { object.incident_updates.collect { new IncidentUpdate(it) } }
	List<IncidentUpdate> getUpdates() { object.incident_updates.collect { new IncidentUpdate(it) } }
}
