package hlaaftana.discordg.data

import groovy.transform.CompileStatic

@CompileStatic
class Intents {
    // essentially EnumSet
    static final Intents NONE = new Intents(0)
    static final Intents ALL = new Intents(Intent.values())
    static final Intents DM = new Intents(
        Intent.DIRECT_MESSAGES, Intent.DIRECT_MESSAGE_REACTIONS, Intent.DIRECT_MESSAGE_TYPING)
    static final Intents GUILD = ALL - DM
    int value = 0

    Intents(int value) { this.value = value }
    Intents(Intent... intents) { for (final i : intents) add(i) }

    int size() { Integer.bitCount(value) }
    boolean isEmpty() { value == 0 }
    boolean contains(Intent o) { has(o) }
    Iterator<Intent> iterator() {
        new Iterator<Intent>() {
            int i = 0

            boolean hasNext() {
                i < Intent.values().length
            }

            @Override
            Intent next() {
                while ((value & (1 << i)) == 0) { ++i }
                Intent.values()[i++]
            }
        }
    }

    void add(Intent intent) {
        value |= intent.value()
    }

    Intents leftShift(Intent intent) { add(intent); this }

    Intents or(Intent intent) { new Intents(value | intent.value()) }
    Intents plus(Intent intent) { or(intent) }

    void remove(Intent intent) {
        // https://issues.apache.org/jira/browse/GROOVY-9704
        value &= intent.value().bitwiseNegate()
    }

    Intents minus(Intent intent) { new Intents(value & ~intent.value()) }

    void set(Intent intent, boolean truth = true) {
        if (truth) add(intent) else remove(intent)
    }
    void putAt(Intent intent, boolean truth) { set(intent, truth) }

    boolean has(Intent intent) { (value & intent.value()) != 0 }
    boolean getAt(Intent intent) { has(intent) }

    Intents or(Intents intents) { new Intents(value | intents.value) }
    Intents plus(Intents intents) { or(intents) }
    Intents and(Intents intents) { new Intents(value & intents.value) }
    Intents multiply(Intents intents) { and(intents) }
    Intents minus(Intents intents) { new Intents(value & (~intents.value)) }

    int hashCode() { value }
    boolean equals(other) {
        (other instanceof Intents && value == other.value) || (other instanceof Number && value == other.intValue())
    }
}

@CompileStatic
enum Intent {
    // can add events later
    GUILDS,
    GUILD_MEMBERS,
    GUILD_BANS,
    GUILD_EMOJIS_AND_STICKERS,
    GUILD_INTEGRATIONS,
    GUILD_WEBHOOKS,
    GUILD_INVITES,
    GUILD_VOICE_STATES,
    GUILD_PRESENCES,
    GUILD_MESSAGES,
    GUILD_MESSAGE_REACTIONS,
    GUILD_MESSAGE_TYPING,
    DIRECT_MESSAGES,
    DIRECT_MESSAGE_REACTIONS,
    DIRECT_MESSAGE_TYPING

    int value() { 1 << ordinal() }
}
