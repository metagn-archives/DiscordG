package hlaaftana.discordg.status

class Schedule extends MapObject {
	Schedule(Map object){ super(object) }

	StatusPage getPage(){ new StatusPage(object.page) }
	StatusPage getStatusPage(){ new StatusPage(object.page) }
	List<Maintenance> getScheduledMaintenances(){ object.scheduled_maintanences.collect { new Maintenance(it) } }
	List<Maintenance> getMaintenances(){ object.scheduled_maintanences.collect { new Maintenance(it) } }
}
