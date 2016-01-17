import ml.hlaaftana.discordg.APIBuilder
import ml.hlaaftana.discordg.objects.*
import ml.hlaaftana.discordg.util.Log
import ml.hlaaftana.discordg.util.bot.*

// All boolean configurations set here are the exact opposite of the default ones.

// Toggles logging for the DEBUG level.
Log.enableDebug = true
// Toggles logging for the ERROR level.
Log.enableError = false
// Toggles logging for the INFO level.
Log.enableInfo = false
// Toggles logging for the WARN level.
Log.enableWarn = false
// Toggles printing crashes from event listeners.
Log.enableListenerCrashes = false
// Toggles printing crashes from event object registering.
Log.enableEventRegisteringCrashes = true

API api = APIBuilder.build()
// Whether or not to cache tokens.
api.cacheTokens = false
// How many threads the threadpool for event handling can contain at maximum. 3 by default.
api.eventThreadCount = 4
// Ignores PRESENCE_UPDATE. This allows your client to run in much bigger servers with less performance issues.
api.ignorePresenceUpdate = true
// I'll soon add an API#ignoreEvent method to ignore specific events.

CommandBot bot = new CommandBot(api)
// Whether or not the account of the bot itself can trigger commands.
bot.acceptOwnCommands = true
// The default prefix for the bot. Can be a String or List of Strings.
bot.defaultPrefix = "!"