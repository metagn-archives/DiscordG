package metagn.discordg;

import java.util.Map;

public interface DiscordRawWSListener {
	default void fire(String type, Map<String, Object> data) {}
}
