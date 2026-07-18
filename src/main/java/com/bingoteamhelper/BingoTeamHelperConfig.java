package com.bingoteamhelper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("bingo-team-helper")
public interface BingoTeamHelperConfig extends Config
{
	@ConfigItem(
		keyName = "teams",
		name = "Teams",
		description = "",
		hidden = true
	)
	default String teams()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showTags",
		name = "Show tags",
		description = ""
	)
	default boolean showTags()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showIndicators",
		name = "Show name tags",
		description = ""
	)
	default boolean showIndicators()
	{
		return true;
	}
}
