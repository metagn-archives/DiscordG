package hlaaftana.discordg.events

import hlaaftana.discordg.objects.Channel
import org.json.JSONObject;

class TypingEvent extends Event{
	TypingEvent (JSONObject json){ super(json, "TYPING_START") }
}
