package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class IncidentUpdate extends MapObject {
	IncidentUpdate(Map object){ super(object) }

	String getStatus(){ object["status"] }
	String getBody(){ object["body"] }
	String getRawUpdatedAt(){ object["updated_at"] }
	Date getUpdatedAt(){ ConversionUtil.fromJsonDate(object["updated_at"]) }
	String getRawCreatedAt(){ object["created_at"] }
	Date getCreatedAt(){ ConversionUtil.fromJsonDate(object["created_at"]) }
	String getRawDisplayAt(){ object["display_at"] }
	Date getDisplayAt(){ ConversionUtil.fromJsonDate(object["display_at"]) }
	String getId(){ object["id"] }
	String getIncidentId(){ object["incident_id"] }
}
