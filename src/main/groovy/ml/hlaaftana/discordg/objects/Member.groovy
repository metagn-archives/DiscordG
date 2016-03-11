package ml.hlaaftana.discordg.objects

import java.text.SimpleDateFormat
import java.util.List
import java.net.URL

import ml.hlaaftana.discordg.util.*

/**
 * A member of a server. Extends the User object.
 * @author Hlaaftana
 */
class Member extends User{
	Member(Client client, Map object){
		super(client, object)
	}

	String getId(){ return this.user.id }
	String getName(){ return this.user.name }
	String getUsername() { return this.user.username }
	String getAvatarHash(){ return this.user.avatarHash }
	String getAvatar() { return this.user.avatar }
	URL getAvatarURL(){ return this.user.avatarURL }
	URL getAvatarUrl(){ return this.user.avatarUrl }
	String getDiscriminator(){ return this.user.discriminator }
	String getDiscrim(){ return this.user.discriminator }
	/**
	 * @return the User which this Member is.
	 */
	User getUser(){ return new User(client, this.object["user"]) }
	/**
	 * @return the server the member comes from.
	 */
	Server getServer(){ return client.getServerById(this.object["guild_id"]) }
	/**
	 * @return a timestamp of when the member joined the server.
	 */
	String getRawJoinDate(){ return this.object["joined_at"] }
	/**
	 * @return a Date object of when the member joined the server.
	 */
	Date getJoinDate(){ return ConversionUtil.fromJsonDate(this.rawJoinDate) }
	/**
	 * @return the roles this member has.
	 */
	List<Role> getRoles(){
		List array = this.object["roles"]
		List<Role> roles = new ArrayList<Role>()
		for (o in array){
			for (r in this.server.roles){
				if (o == r.id) roles.add(r)
			}
		}
		return roles
	}

	/**
	 * @return the current game the member is playing. Can be null.
	 */
	String getGame(){
		try{
			return this.server.object["presences"].find({ it.user.id == this.user.id }).game.name
		}catch (ex){
			return null
		}
	}

	/**
	 * Mutes or deafens the user.
	 * @param data - the map. <br>
	 * [mute: true, deaf: false]
	 */
	void edit(Map data){
		client.requester.patch("https://discordapp.com/api/guilds/${this.server.id}/members/${this.id}", data)
	}

	void mute(){ this.edit(mute: true) }
	void unmute(){ this.edit(mute: false) }
	void deafen(){ this.edit(deaf: true) }
	void undeafen(){ this.edit(deaf: false) }
	void ban(){ this.server.ban(this) }
	void unban(){ this.server.unban(this) }

	/**
	 * @return the status of the user. e.g. "online", "offline", "idle"...
	 */
	String getStatus(){
		try{
			return this.server.object["presences"].find({ it.user.id == this.user.id }).status
		}catch (ex){
			return "offline"
		}
	}

	/**
	 * Overrides the roles for this member.
	 * @param roles - a list of roles to add.
	 */
	void editRoles(List<Role> roles) {
		this.server.editRoles(this, roles)
	}

	/**
	 * Adds roles to this member.
	 * @param roles - the roles to add.
	 */
	void addRoles(List<Role> roles) {
		this.server.addRoles(this, roles)
	}

	/**
	 * Kicks the member from its server.
	 */
	void kick() {
		this.server.kickMember(this)
	}

	void moveTo(VoiceChannel channel){
		client.requester.patch("https://discordapp.com/api/guilds/${this.server.id}/members/{this.id}", ["channel_id": channel.id])
	}

	User toUser(){ return new User(client, this.object["user"]) }
	def asType(Class target){
		if (target == User) return this.toUser()
	}
}
