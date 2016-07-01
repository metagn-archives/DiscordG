package hlaaftana.discordg.status

import hlaaftana.discordg.objects.MapObject;
import hlaaftana.discordg.util.ConversionUtil

class IncidentUpdate extends MapObject {
	IncidentUpdate(Map object){ super(object) }

	String getStatus(){ object["status"] }
	String getBody(){ object["body"] }
	String getRawUpdateTime(){ object["updated_at"] }
	Date getUpdateTime(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
	String getRawCreateTime(){ object["created_at"] }
	Date getCreateTime(){ ConversionUtil.fromJsonDate(object["created_at"]) }
	String getRawDisplayTime(){ object["display_at"] }
	Date getDisplayTime(){ ConversionUtil.fromJsonDate(object["display_at"]) }
	String getId(){ object["id"] }
	String getIncidentId(){ object["incident_id"] }
}
