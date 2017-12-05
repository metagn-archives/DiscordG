package hlaaftana.discordg.status

import hlaaftana.discordg.util.ConversionUtil

class IncidentUpdate extends MapObject {
	IncidentUpdate(Map object){ super(object) }

	String getStatus() { (String) object.status }
	String getBody() { (String) object.body }
	String getRawUpdatedAt() { (String) object.updated_at }
	Date getUpdatedAt(){ ConversionUtil.fromJsonDate(object.updated_at) }
	String getRawCreatedAt() { (String) object.created_at }
	Date getCreatedAt(){ ConversionUtil.fromJsonDate(object.created_at) }
	String getRawDisplayAt() { (String) object.display_at }
	Date getDisplayAt(){ ConversionUtil.fromJsonDate(object.display_at) }
	String getId() { (String) object.id }
	String getIncidentId() { (String) object.incident_id }
}
