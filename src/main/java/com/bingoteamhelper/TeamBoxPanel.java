package com.bingoteamhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ColorJButton;
import net.runelite.client.ui.components.FlatTextField;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;

class TeamBoxPanel extends JPanel
{
	private final TeamData teamData;
	private final Consumer<Void> onChange;
	private final ColorPickerManager colorPickerManager;
	private final FlatTextField nameField;
	private final JTextArea membersArea;

	TeamBoxPanel(TeamData teamData, ColorPickerManager colorPickerManager, Consumer<Void> onChange)
	{
		this.teamData = teamData;
		this.colorPickerManager = colorPickerManager;
		this.onChange = onChange;

		setLayout(new DynamicGridLayout(0, 1, 0, 4));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(6, 6, 6, 6)
		));
		setMinimumSize(new Dimension(PluginPanel.PANEL_WIDTH, 0));

		nameField = new FlatTextField();
		nameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		nameField.setHoverBackgroundColor(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		nameField.setText(teamData.getName());
		nameField.getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				String name = nameField.getText().trim();
				if (!name.isEmpty())
				{
					teamData.setName(name);
				}
				else
				{
					nameField.setText(teamData.getName());
				}
				notifyChange();
			}
		});

		ColorJButton colorButton = new ColorJButton("", teamData.getColor());
		colorButton.setFocusable(false);
		colorButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				RuneliteColorPicker colorPicker = colorPickerManager.create(
					TeamBoxPanel.this,
					colorButton.getColor(),
					teamData.getName(),
					true
				);
				colorPicker.setLocationRelativeTo(colorButton);
				colorPicker.setOnColorChange(c ->
				{
					colorButton.setColor(c);
					teamData.setColor(c);
				});
				colorPicker.setOnClose(c ->
				{
					teamData.setColor(c);
					notifyChange();
				});
				colorPicker.setVisible(true);
			}
		});

		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.add(nameField, BorderLayout.CENTER);
		header.add(colorButton, BorderLayout.EAST);

		membersArea = new JTextArea(teamData.getMembers() == null ? "" : teamData.getMembers());
		membersArea.setLineWrap(false);
		membersArea.setRows(3);
		membersArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		membersArea.setForeground(Color.WHITE);
		membersArea.setCaretColor(Color.WHITE);
		membersArea.setBorder(new EmptyBorder(2, 2, 2, 2));
		membersArea.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				teamData.setMembers(membersArea.getText());
				notifyChange();
			}
		});

		JScrollPane membersScroll = new JScrollPane(membersArea);
		membersScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		membersScroll.setPreferredSize(new Dimension(0, 72));

		add(header);
		add(membersScroll);
	}

	TeamData getTeamData()
	{
		String name = nameField.getText().trim();
		if (!name.isEmpty())
		{
			teamData.setName(name);
		}
		teamData.setMembers(membersArea.getText());
		return teamData;
	}

	private void notifyChange()
	{
		onChange.accept(null);
	}
}
