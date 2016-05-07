package hlaaftana.discordg.status

import hlaaftana.discordg.objects.MapObject;
import hlaaftana.discordg.util.ConversionUtil

class IncidentUpdate extends MapObject {
	IncidentUpdate(Map object){ super(object) }

	String getStatus(){ return this.object["status"] }
	String getBody(){ return this.object["body"] }
	String getRawUpdateTime(){ return this.object["updated_at"] }
	Date getUpdateTime(){ return ConversionUtil.fromJsonDate(this.object["updated_at"]) }
	String getRawCreateTime(){ return this.object["created_at"] }
	Date getCreateTime(){ return ConversionUtil.fromJsonDate(this.object["created_at"]) }
	String getRawDisplayTime(){ return this.object["display_at"] }
	Date getDisplayTime(){ return ConversionUtil.fromJsonDate(this.object["display_at"]) }
	String getId(){ return this.object["id"] }
	String getIncidentId(){ return this.object["incident_id"] }
}
