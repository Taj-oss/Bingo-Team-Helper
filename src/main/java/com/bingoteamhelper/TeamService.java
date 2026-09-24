package com.bingoteamhelper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

@Singleton
public class TeamService
{
	private static final String CONFIG_GROUP = "bingo-team-helper";
	private static final String CONFIG_KEY = "teams";
	/** Matches {@link net.runelite.client.ui.FontManager#getRunescapeFont()} / Player Indicators. */
	static final int DEFAULT_NAME_FONT_SIZE = 16;
	private static final int LEGACY_NAME_FONT_SIZE = 9;
	static final int MIN_NAME_FONT_SIZE = 6;
	static final int MAX_NAME_FONT_SIZE = 32;

	private static final Color[] PRESET_COLORS = {
		new Color(231, 76, 60),
		new Color(52, 152, 219),
		new Color(46, 204, 113),
		new Color(241, 196, 15),
		new Color(155, 89, 182),
		new Color(230, 126, 34),
		new Color(26, 188, 156),
		new Color(236, 112, 99),
	};

	private static final Type TEAM_LIST_TYPE = new TypeToken<List<TeamData>>() {}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	private List<TeamData> teams = new ArrayList<>();
	private Map<String, PlayerTeamInfo> playerTeams = Collections.emptyMap();

	@Inject
	TeamService(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	void load()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);
		List<TeamData> loaded;

		if (json == null || json.trim().isEmpty())
		{
			loaded = createDefaultTeams();
		}
		else
		{
			loaded = gson.fromJson(json, TEAM_LIST_TYPE);
			if (loaded == null || loaded.isEmpty())
			{
				loaded = createDefaultTeams();
			}
		}

		teams = new ArrayList<>(loaded);
		for (TeamData team : teams)
		{
			normalizeTeamName(team);
			normalizeNameFont(team);
		}
		rebuildLookup();
	}

	List<TeamData> getTeams()
	{
		return new ArrayList<>(teams);
	}

	void saveTeams(List<TeamData> updatedTeams)
	{
		teams = new ArrayList<>(updatedTeams);
		configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY, gson.toJson(teams));
		rebuildLookup();
	}

	String exportTeamsJson()
	{
		return gson.toJson(teams);
	}

	boolean importTeamsJson(String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return false;
		}

		List<TeamData> imported = gson.fromJson(json.trim(), TEAM_LIST_TYPE);
		if (imported == null || imported.isEmpty())
		{
			return false;
		}

		for (TeamData team : imported)
		{
			if (team.getMembers() == null)
			{
				team.setMembers("");
			}

			normalizeTeamName(team);
			normalizeNameFont(team);
		}

		saveTeams(imported);
		return true;
	}

	TeamData createDefaultTeam(int teamNumber)
	{
		TeamData team = new TeamData();
		team.setName("Team " + teamNumber);
		team.setColor(PRESET_COLORS[(teamNumber - 1) % PRESET_COLORS.length]);
		team.setMembers("");
		team.setNameFontSize(0);
		team.setNameBold(false);
		return team;
	}

	PlayerTeamInfo getTeamInfoForPlayer(String playerName)
	{
		if (playerName == null)
		{
			return null;
		}

		return playerTeams.get(normalizeName(playerName));
	}

	private List<TeamData> createDefaultTeams()
	{
		List<TeamData> defaults = new ArrayList<>();
		defaults.add(createDefaultTeam(1));
		defaults.add(createDefaultTeam(2));
		return defaults;
	}

	private void rebuildLookup()
	{
		Map<String, PlayerTeamInfo> lookup = new HashMap<>();

		for (TeamData team : teams)
		{
			if (team.getMembers() == null)
			{
				continue;
			}

			PlayerTeamInfo info = new PlayerTeamInfo(
				team.getColor(),
				team.getName(),
				team.getNameFontSize(),
				team.isNameBold()
			);

			for (String line : team.getMembers().split("\\R"))
			{
				String trimmed = line.trim();
				if (trimmed.isEmpty())
				{
					continue;
				}

				String key = normalizeName(trimmed);
				// First team in the list wins when a player appears on multiple teams.
				lookup.putIfAbsent(key, info);
			}
		}

		playerTeams = lookup;
	}

	private static String normalizeName(String name)
	{
		return Text.standardize(name);
	}

	static int resolveFontSize(int fontSize)
	{
		if (fontSize <= 0)
		{
			return DEFAULT_NAME_FONT_SIZE;
		}
		return Math.max(MIN_NAME_FONT_SIZE, Math.min(MAX_NAME_FONT_SIZE, fontSize));
	}

	static boolean isDefaultNameFont(int fontSize, boolean bold)
	{
		return !bold && fontSize <= 0;
	}

	private static void normalizeTeamName(TeamData team)
	{
		if (team.getName() == null)
		{
			team.setName("");
		}
	}

	private static void normalizeNameFont(TeamData team)
	{
		int size = team.getNameFontSize();
		if (size == LEGACY_NAME_FONT_SIZE)
		{
			team.setNameFontSize(0);
			return;
		}

		if (!team.isNameBold() && size == DEFAULT_NAME_FONT_SIZE)
		{
			team.setNameFontSize(0);
		}
	}
}
