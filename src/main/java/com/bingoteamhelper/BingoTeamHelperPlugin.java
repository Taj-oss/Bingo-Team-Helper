package com.bingoteamhelper;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.ClientTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Bingo Team Helper",
	description = "Bingo team helper",
	tags = {"bingo"}
)
public class BingoTeamHelperPlugin extends Plugin
{
	private static final BufferedImage PANEL_ICON = createPanelIcon();

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BingoTeamHelperOverlay overlay;

	@Inject
	private BingoTeamHelperPanel panel;

	@Inject
	private TeamService teamService;

	@Inject
	private BingoTeamHelperConfig config;

	@Inject
	private Client client;

	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		teamService.load();
		panel.rebuildTeams();

		navButton = NavigationButton.builder()
			.tooltip("Bingo Team Helper")
			.icon(PANEL_ICON)
			.priority(6)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		overlayManager.add(overlay);
		log.debug("Bingo Team Helper started");
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(overlay);
		log.debug("Bingo Team Helper stopped");
	}

	@Provides
	BingoTeamHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BingoTeamHelperConfig.class);
	}

	@Subscribe(priority = -1)
	public void onClientTick(ClientTick clientTick)
	{
		if (!config.showIndicators() || client.isMenuOpen())
		{
			return;
		}

		for (MenuEntry entry : client.getMenuEntries())
		{
			if (!isPlayerMenuAction(entry.getType()))
			{
				continue;
			}

			Player player = entry.getPlayer();
			if (player == null || player.getName() == null)
			{
				continue;
			}

			PlayerTeamInfo teamInfo = teamService.getTeamInfoForPlayer(player.getName());
			if (teamInfo == null)
			{
				continue;
			}

			entry.setTarget(TeamMenuTargetDecorator.decorate(
				entry.getTarget(),
				teamInfo,
				config.showTags()
			));
		}
	}

	private static boolean isPlayerMenuAction(MenuAction type)
	{
		switch (type)
		{
			case WALK:
			case WIDGET_TARGET_ON_PLAYER:
			case ITEM_USE_ON_PLAYER:
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGHTH_OPTION:
			case RUNELITE_PLAYER:
				return true;
			default:
				return false;
		}
	}

	private static BufferedImage createPanelIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(241, 196, 15));
		graphics.fillRect(0, 0, 16, 16);
		graphics.setColor(new Color(52, 152, 219));
		graphics.fillRect(0, 8, 8, 8);
		graphics.setColor(new Color(231, 76, 60));
		graphics.fillRect(8, 0, 8, 8);
		graphics.dispose();
		return ImageUtil.resizeImage(image, 16, 16);
	}
}
