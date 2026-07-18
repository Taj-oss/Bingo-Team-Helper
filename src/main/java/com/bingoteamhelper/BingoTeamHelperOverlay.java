package com.bingoteamhelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

public class BingoTeamHelperOverlay extends Overlay
{
	private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;

	private final Client client;
	private final BingoTeamHelperConfig config;
	private final TeamService teamService;
	private final ChatIconManager chatIconManager;

	@Inject
	BingoTeamHelperOverlay(
		Client client,
		BingoTeamHelperConfig config,
		TeamService teamService,
		ChatIconManager chatIconManager
	)
	{
		this.client = client;
		this.config = config;
		this.teamService = teamService;
		this.chatIconManager = chatIconManager;
		setPosition(OverlayPosition.DYNAMIC);
		// Same layer as Player Indicators (default UNDER_WIDGETS). ABOVE_SCENE
		// always renders before UNDER_WIDGETS, so Player Indicators was drawing
		// on top and hiding our labels.
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showIndicators())
		{
			return null;
		}

		for (Player player : client.getPlayers())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			PlayerTeamInfo teamInfo = teamService.getTeamInfoForPlayer(player.getName());
			if (teamInfo == null)
			{
				continue;
			}

			String name = Text.sanitize(player.getName());
			String displayName = teamInfo.getDisplayName(name, config.showTags());
			java.awt.Color color = teamInfo.getColor();
			int zOffset = player.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;
			Point textLocation = player.getCanvasTextLocation(graphics, displayName, zOffset);

			if (textLocation == null)
			{
				continue;
			}

			BufferedImage rankImage = getRankImage(player);
			if (rankImage != null)
			{
				textLocation = new Point(textLocation.getX() + rankImage.getWidth() / 2, textLocation.getY());
			}

			OverlayUtil.renderTextLocation(graphics, textLocation, displayName, color);
		}

		return null;
	}

	private BufferedImage getRankImage(Player player)
	{
		if (player.isFriendsChatMember())
		{
			FriendsChatManager friendsChatManager = client.getFriendsChatManager();
			FriendsChatMember member = friendsChatManager == null
				? null
				: friendsChatManager.findByName(Text.removeTags(player.getName()));
			if (member != null && member.getRank() != FriendsChatRank.UNRANKED)
			{
				return chatIconManager.getRankImage(member.getRank());
			}
		}

		if (player.isClanMember())
		{
			ClanChannel clanChannel = client.getClanChannel();
			ClanSettings clanSettings = client.getClanSettings();
			if (clanChannel != null && clanSettings != null)
			{
				ClanChannelMember member = clanChannel.findMember(player.getName());
				if (member != null)
				{
					ClanTitle title = clanSettings.titleForRank(member.getRank());
					if (title != null)
					{
						return chatIconManager.getRankImage(title);
					}
				}
			}
		}

		return null;
	}
}
