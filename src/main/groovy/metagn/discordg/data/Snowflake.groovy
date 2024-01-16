package metagn.discordg.data

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class Snowflake extends Number implements Comparable<Snowflake> {
	long value

	Snowflake(String id) { value = Long.parseUnsignedLong(id) }
	Snowflake(long val) { value = val }

	static Snowflake fromMillis(long ms, boolean raised = false) {
		new Snowflake(((ms - 1420070400000L) << 22) + (raised ? 1 << 22 : 0))
	}

	static boolean isId(long x) {
		x > 2000000000000000
	}

	static boolean isId(String x) {
		x.isLong() && x.toLong() > 2000000000000000
	}

	static Snowflake from(thing) {
		if (null == thing) null
		else if (thing instanceof Snowflake) (Snowflake) thing
		else if (thing instanceof Long) new Snowflake(thing.longValue())
		else if (thing instanceof String)
			new Snowflake(isMention(thing) ? mentionToId(thing) : (String) thing)
		else if (thing instanceof DiscordObject) thing.id
		else if (thing instanceof Map) from(thing.id)
		else try { new Snowflake(dynamicId(thing)) }
		catch (ignore) { new Snowflake(thing.toString()) }
	}

	static Snowflake swornString(value) {
		null == value ? null : new Snowflake((String) value)
	}

	static Set<Snowflake> swornStringSet(value) {
		final val = (Collection<String>) value
		def result = new HashSet<Snowflake>(val.size())
		for (id in val) result.add(new Snowflake(id))
		result
	}

	@CompileDynamic
	static String dynamicId(a) { a.id?.toString() }

	static String mentionToId(String mention) {
		mention.substring(Character.isDigit(mention[2] as char) ? 2 : 3, mention.length() - 1)
	}

	static boolean isMention(String x) {
		def a = x.charAt(0), b = x.charAt(1), c
		a == ((char) '<') && b == ((char) '@') || b == ((char) '#') &&
				(Character.isDigit((c = x.charAt(2))) || c == ((char) '&') || c == ((char) '!')) &&
				x.charAt(x.length() - 1) == (char) '>'
	}

	long toMillis() {
		(value >> 22) + 1420070400000L
	}

	String toString() {
		Long.toUnsignedString(value)
	}

	boolean equals(other) {
		other instanceof Snowflake && other.value == value
	}

	int hashCode() {
		Long.hashCode(value)
	}

	int compareTo(Snowflake o) {
		value.compareTo(o.value)
	}

	int intValue() { value.intValue() }
	long longValue() { value }
	float floatValue() { value.floatValue() }
	double doubleValue() { value.doubleValue() }
}
