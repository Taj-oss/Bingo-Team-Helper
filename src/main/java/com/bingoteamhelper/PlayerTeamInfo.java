package com.bingoteamhelper;

import java.awt.Color;
import lombok.Value;

@Value
class PlayerTeamInfo
{
	Color color;
	String teamName;

	String getDisplayName(String playerName, boolean showTag)
	{
		if (showTag && teamName != null && !teamName.isEmpty())
		{
			return "[" + teamName + "]" + playerName;
		}
		return playerName;
	}
}
