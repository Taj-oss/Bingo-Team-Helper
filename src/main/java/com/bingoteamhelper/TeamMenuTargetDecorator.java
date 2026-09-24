package com.bingoteamhelper;

import java.awt.Color;
import net.runelite.client.util.ColorUtil;

final class TeamMenuTargetDecorator
{
	private TeamMenuTargetDecorator()
	{
	}

	static String decorate(String oldTarget, PlayerTeamInfo teamInfo, boolean showTags)
	{
		String prefix = "";
		String target = oldTarget;

		int arrowIdx = target.indexOf("->");
		if (arrowIdx != -1)
		{
			prefix = target.substring(0, arrowIdx + 3);
			target = target.substring(arrowIdx + 3);
		}

		int tagEnd = target.indexOf('>');
		if (tagEnd != -1)
		{
			target = target.substring(tagEnd + 1);
		}

		String display = target;
		if (showTags && teamInfo.getTeamName() != null && !teamInfo.getTeamName().isEmpty())
		{
			display = "[" + teamInfo.getTeamName() + "]" + display;
		}

		Color color = teamInfo.getColor();
		return prefix + ColorUtil.prependColorTag(display, color);
	}
}
