package ml.hlaaftana.discordg.status

import ml.hlaaftana.discordg.objects.MapObject;
import ml.hlaaftana.discordg.util.ConversionUtil

class Maintenance extends MapObject {
	Maintenance(Map object){ super(object) }

	String getName(){ return this.object["name"] }
	String getStatus(){ return this.object["status"] }
	String getRawUpdateTime(){ return this.object["updated_at"] }
	Date getUpdateTime(){ return ConversionUtil.fromJsonDate(this.object["updated_at"]) }
	String getRawCreateTime(){ return this.object["created_at"] }
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.object["created_at"]) }
	String getShortlink(){ return this.object["shortlink"] }
	String getRawScheduleStartTime(){ return this.object["scheduled_for"] }
	Date getScheduleStartTime(){ return ConversionUtil.fromJsonDate(this.object["scheduled_for"]) }
	String getRawScheduleEndTime(){ return this.object["scheduled_until"] }
	Date getScheduleEndTime(){ return ConversionUtil.fromJsonDate(this.object["scheduled_until"]) }
	String getId(){ return this.object["id"] }
	String getPageId(){ return this.object["page_id"] }
	String getImpact(){ return this.object["impact"] }
	List<IncidentUpdate> getIncidentUpdates(){ return this.object["incident_updates"].collect { new IncidentUpdate(it) } }
	List<IncidentUpdate> getUpdates(){ return this.object["incident_updates"].collect { new IncidentUpdate(it) } }
}
