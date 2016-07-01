package hlaaftana.discordg.objects

import groovy.transform.Memoized

enum DefaultAvatars {
	BLUE("6debd47ed13483642cf09e832ed0bc1b", 0),
	GREY("322c936a8c8be1b803cd94861bdfa868", 1),
	GRAY("322c936a8c8be1b803cd94861bdfa868", 1),
	GREEN("dd4dbc0016779df1378e7812eabaa04d", 2),
	YELLOW("0e291f67c9274a1abdddeb3fd919cbaa", 3),
	RED("1cbd08c76f8af6dddce02c5138971129", 4)

	String hash
	int order
	DefaultAvatars(String hash, int order){ this.hash = hash; this.order = order }

	@Memoized
	static DefaultAvatars get(int order){ DefaultAvatars.class.enumConstants.find { it.order == order } }
}
