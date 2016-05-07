package hlaaftana.discordg.objects

enum DefaultAvatars {
	BLUE("b3afd12bc47a87507780ce5f53a9d6a1", 0),
	GREY("0d1a93187d96a05e86444f2fc6210d95", 1),
	GRAY("0d1a93187d96a05e86444f2fc6210d95", 1),
	GREEN("a83f572c0b5c2d87f935ce6229be6358", 2),
	YELLOW("907c319873ae4c1d56d0d0e8dce6b476", 3),
	RED("8b3fac6205178732d218265987cdb0dc", 4)

	String hash
	int order
	DefaultAvatars(String hash, int order){ this.hash = hash; this.order = order }
	static DefaultAvatars get(int order){ return DefaultAvatars.class.enumConstants.find { it.order == order } }
}
