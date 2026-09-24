package com.bingoteamhelper;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

public class BingoTeamHelperPanel extends PluginPanel
{
	private static final String CONFIG_GROUP = "bingo-team-helper";
	private static final String SHOW_TAGS_KEY = "showTags";

	private final TeamService teamService;
	private final ColorPickerManager colorPickerManager;
	private final BingoTeamHelperConfig config;
	private final ConfigManager configManager;

	private final JPanel teamsContainer = new JPanel(new DynamicGridLayout(0, 1, 0, 8));
	private final List<TeamBoxPanel> teamBoxes = new ArrayList<>();
	private TeamBoxPanel dragSource;
	private AWTEventListener dragReleaseListener;

	@Inject
	BingoTeamHelperPanel(
		TeamService teamService,
		ColorPickerManager colorPickerManager,
		BingoTeamHelperConfig config,
		ConfigManager configManager
	)
	{
		this.teamService = teamService;
		this.colorPickerManager = colorPickerManager;
		this.config = config;
		this.configManager = configManager;

		teamsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel actions = buildActionsPanel();
		JPanel showTagsRow = buildBooleanOptionRow("Show tags", config.showTags(), selected ->
			configManager.setConfiguration(CONFIG_GROUP, SHOW_TAGS_KEY, Boolean.toString(selected))
		);

		add(actions);
		add(teamsContainer);
		add(showTagsRow);

		rebuildTeams();
	}

	private JPanel buildActionsPanel()
	{
		JPanel actions = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		actions.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton addTeamButton = new JButton("+ Add Team");
		addTeamButton.setFocusable(false);
		addTeamButton.addActionListener(e -> addTeam());

		JButton importButton = new JButton("Import");
		importButton.setFocusable(false);
		importButton.addActionListener(e -> importTeams());

		JButton exportButton = new JButton("Export");
		exportButton.setFocusable(false);
		exportButton.addActionListener(e -> exportTeams());

		actions.add(addTeamButton);
		actions.add(importButton);
		actions.add(exportButton);
		return actions;
	}

	private static JPanel buildBooleanOptionRow(String label, boolean selected, Consumer<Boolean> onChange)
	{
		JPanel item = new JPanel(new BorderLayout());
		item.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
		item.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel configEntryName = new JLabel(label);
		configEntryName.setForeground(Color.WHITE);
		item.add(configEntryName, BorderLayout.CENTER);

		JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected(selected);
		checkbox.setFocusable(false);
		checkbox.addActionListener(e -> onChange.accept(checkbox.isSelected()));
		item.add(checkbox, BorderLayout.EAST);

		return item;
	}

	void rebuildTeams()
	{
		teamsContainer.removeAll();
		teamBoxes.clear();

		for (TeamData team : teamService.getTeams())
		{
			TeamBoxPanel box = new TeamBoxPanel(
				team,
				colorPickerManager,
				ignored -> saveTeams(),
				reorderListener()
			);
			teamBoxes.add(box);
			teamsContainer.add(box);
		}

		teamsContainer.revalidate();
		teamsContainer.repaint();
	}

	private void addTeam()
	{
		List<TeamData> teams = collectTeams();
		teams.add(teamService.createDefaultTeam(teams.size() + 1));
		teamService.saveTeams(teams);
		rebuildTeams();
	}

	private void saveTeams()
	{
		teamService.saveTeams(collectTeams());
	}

	private TeamBoxPanel.ReorderListener reorderListener()
	{
		return new TeamBoxPanel.ReorderListener()
		{
			@Override
			public void beginReorder(TeamBoxPanel source)
			{
				beginDrag(source);
			}

			@Override
			public void completeReorder(java.awt.Point pointInTeamsContainer)
			{
				BingoTeamHelperPanel.this.completeReorder(pointInTeamsContainer);
			}
		};
	}

	private void beginDrag(TeamBoxPanel source)
	{
		endDragListener();
		dragSource = source;

		dragReleaseListener = event ->
		{
			if (dragSource == null || !(event instanceof MouseEvent))
			{
				return;
			}

			MouseEvent mouseEvent = (MouseEvent) event;
			if (mouseEvent.getID() != MouseEvent.MOUSE_RELEASED)
			{
				return;
			}

			Point pointInTeamsContainer = SwingUtilities.convertPoint(
				mouseEvent.getComponent(),
				mouseEvent.getPoint(),
				teamsContainer
			);
			completeReorder(pointInTeamsContainer);
		};

		Toolkit.getDefaultToolkit().addAWTEventListener(dragReleaseListener, AWTEvent.MOUSE_EVENT_MASK);
	}

	private void completeReorder(Point pointInTeamsContainer)
	{
		if (dragSource == null)
		{
			endDragListener();
			return;
		}

		TeamBoxPanel source = dragSource;
		dragSource = null;
		endDragListener();
		reorderTeam(source, findTeamIndexAtY(pointInTeamsContainer.y));
	}

	private void endDragListener()
	{
		if (dragReleaseListener != null)
		{
			Toolkit.getDefaultToolkit().removeAWTEventListener(dragReleaseListener);
			dragReleaseListener = null;
		}
	}

	private int findTeamIndexAtY(int y)
	{
		for (int i = 0; i < teamBoxes.size(); i++)
		{
			TeamBoxPanel box = teamBoxes.get(i);
			int midY = box.getY() + box.getHeight() / 2;
			if (y < midY)
			{
				return i;
			}
		}
		return Math.max(0, teamBoxes.size() - 1);
	}

	private void reorderTeam(TeamBoxPanel source, int targetIndex)
	{
		int fromIndex = teamBoxes.indexOf(source);
		if (fromIndex < 0 || fromIndex == targetIndex)
		{
			return;
		}

		TeamBoxPanel box = teamBoxes.remove(fromIndex);
		teamBoxes.add(targetIndex, box);

		teamsContainer.removeAll();
		for (TeamBoxPanel teamBox : teamBoxes)
		{
			teamsContainer.add(teamBox);
		}

		teamsContainer.revalidate();
		teamsContainer.repaint();
		saveTeams();
	}

	private void exportTeams()
	{
		saveTeams();
		String json = teamService.exportTeamsJson();

		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(json), null);

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Export Teams");
		fileChooser.setSelectedFile(new java.io.File("bingo-teams.json"));
		fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));

		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			java.io.File file = fileChooser.getSelectedFile();
			if (!file.getName().endsWith(".json"))
			{
				file = new java.io.File(file.getAbsolutePath() + ".json");
			}

			try
			{
				Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
				JOptionPane.showMessageDialog(
					this,
					"Teams exported to file and copied to clipboard.",
					"Export Successful",
					JOptionPane.INFORMATION_MESSAGE
				);
			}
			catch (IOException ex)
			{
				JOptionPane.showMessageDialog(
					this,
					"Could not write file, but teams were copied to clipboard.",
					"Export Partially Failed",
					JOptionPane.WARNING_MESSAGE
				);
			}
		}
		else
		{
			JOptionPane.showMessageDialog(
				this,
				"Teams copied to clipboard.",
				"Export Successful",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
	}

	private void importTeams()
	{
		String[] options = {"Clipboard", "File", "Cancel"};
		int choice = JOptionPane.showOptionDialog(
			this,
			"Import teams from clipboard or file?",
			"Import Teams",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			options,
			options[0]
		);

		if (choice == 2 || choice == JOptionPane.CLOSED_OPTION)
		{
			return;
		}

		String json;
		if (choice == 0)
		{
			json = readClipboard();
			if (json == null)
			{
				JOptionPane.showMessageDialog(
					this,
					"Clipboard does not contain text.",
					"Import Failed",
					JOptionPane.ERROR_MESSAGE
				);
				return;
			}
		}
		else
		{
			json = readImportFile();
			if (json == null)
			{
				return;
			}
		}

		if (teamService.importTeamsJson(json))
		{
			rebuildTeams();
			JOptionPane.showMessageDialog(
				this,
				"Teams imported successfully.",
				"Import Successful",
				JOptionPane.INFORMATION_MESSAGE
			);
		}
		else
		{
			JOptionPane.showMessageDialog(
				this,
				"Could not import teams. Check the file or clipboard contents.",
				"Import Failed",
				JOptionPane.ERROR_MESSAGE
			);
		}
	}

	private String readClipboard()
	{
		try
		{
			if (!Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor))
			{
				return null;
			}

			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		}
		catch (UnsupportedFlavorException | IOException ex)
		{
			return null;
		}
	}

	private String readImportFile()
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Import Teams");
		fileChooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));

		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return null;
		}

		try
		{
			return Files.readString(fileChooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
		}
		catch (IOException ex)
		{
			JOptionPane.showMessageDialog(
				this,
				"Could not read the selected file.",
				"Import Failed",
				JOptionPane.ERROR_MESSAGE
			);
			return null;
		}
	}

	private List<TeamData> collectTeams()
	{
		List<TeamData> teams = new ArrayList<>();
		for (TeamBoxPanel box : teamBoxes)
		{
			teams.add(box.getTeamData());
		}
		return teams;
	}
}
