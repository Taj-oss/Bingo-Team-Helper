package com.bingoteamhelper;

import java.awt.Dimension;
import java.awt.Font;
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
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showIndicators())
		{
			return null;
		}

		Font overlayFont = graphics.getFont();

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

			graphics.setFont(overlayFont);
			if (!TeamService.isDefaultNameFont(teamInfo.getFontSize(), teamInfo.isBold()))
			{
				graphics.setFont(createTeamFont(overlayFont, teamInfo));
			}

			String name = Text.sanitize(player.getName());
			java.awt.Color color = teamInfo.getColor();
			int zOffset = player.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;

			Point textLocation = player.getCanvasTextLocation(graphics, name, zOffset);
			if (textLocation == null)
			{
				continue;
			}

			textLocation = applyRankOffset(graphics, player, textLocation);

			if (config.showTags() && teamInfo.getTeamName() != null && !teamInfo.getTeamName().isEmpty())
			{
				String tagPrefix = "[" + teamInfo.getTeamName() + "]";
				int tagWidth = graphics.getFontMetrics().stringWidth(tagPrefix);
				OverlayUtil.renderTextLocation(
					graphics,
					new Point(textLocation.getX() - tagWidth, textLocation.getY()),
					tagPrefix,
					color
				);
			}

			OverlayUtil.renderTextLocation(graphics, textLocation, name, color);
		}

		graphics.setFont(overlayFont);
		return null;
	}

	private static Font createTeamFont(Font overlayFont, PlayerTeamInfo teamInfo)
	{
		int style = teamInfo.isBold() ? Font.BOLD : Font.PLAIN;
		if (teamInfo.getFontSize() <= 0)
		{
			return overlayFont.deriveFont(style);
		}
		return overlayFont.deriveFont(style, (float) TeamService.resolveFontSize(teamInfo.getFontSize()));
	}

	private Point applyRankOffset(Graphics2D graphics, Player player, Point textLocation)
	{
		BufferedImage rankImage = getRankImage(player);
		if (rankImage == null)
		{
			return textLocation;
		}

		int imageWidth = rankImage.getWidth();
		int textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getMaxDescent();
		Point imageLocation = new Point(
			textLocation.getX() - imageWidth / 2 - 1,
			textLocation.getY() - textHeight / 2 - rankImage.getHeight() / 2
		);
		OverlayUtil.renderImageLocation(graphics, imageLocation, rankImage);
		return new Point(textLocation.getX() + imageWidth / 2, textLocation.getY());
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
