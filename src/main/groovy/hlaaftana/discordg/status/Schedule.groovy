package hlaaftana.discordg.status

import groovy.transform.CompileStatic

@CompileStatic
class Schedule extends MapObject {
	Schedule(Map object) { super(object) }

	StatusPage getPage() { new StatusPage((Map) object.page) }
	StatusPage getStatusPage() { new StatusPage((Map) object.page) }
	List<Maintenance> getScheduledMaintenances() { object.scheduled_maintanences?.collect { new Maintenance((Map) it) } }
	List<Maintenance> getMaintenances() { object.scheduled_maintanences?.collect { new Maintenance((Map) it) } }
}
