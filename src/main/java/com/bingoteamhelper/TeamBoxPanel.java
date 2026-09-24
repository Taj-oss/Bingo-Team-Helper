package com.bingoteamhelper;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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
	interface ReorderListener
	{
		void beginReorder(TeamBoxPanel source);

		void completeReorder(java.awt.Point pointInTeamsContainer);
	}

	private final TeamData teamData;
	private final Consumer<Void> onChange;
	private final ReorderListener reorderListener;
	private final ColorPickerManager colorPickerManager;
	private final FlatTextField nameField;
	private final JTextField fontSizeField;
	private final JToggleButton boldButton;
	private final JTextArea membersArea;

	TeamBoxPanel(
		TeamData teamData,
		ColorPickerManager colorPickerManager,
		Consumer<Void> onChange,
		ReorderListener reorderListener
	)
	{
		this.teamData = teamData;
		this.colorPickerManager = colorPickerManager;
		this.onChange = onChange;
		this.reorderListener = reorderListener;

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
		nameField.setText(teamData.getName() != null ? teamData.getName() : "");
		nameField.getTextField().addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				teamData.setName(nameField.getText().trim());
				notifyChange();
			}
		});

		int displayFontSize = teamData.getNameFontSize() <= 0
			? TeamService.DEFAULT_NAME_FONT_SIZE
			: TeamService.resolveFontSize(teamData.getNameFontSize());
		fontSizeField = new JTextField(Integer.toString(displayFontSize), 2);
		fontSizeField.setFocusable(true);
		fontSizeField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fontSizeField.setForeground(Color.WHITE);
		fontSizeField.setCaretColor(Color.WHITE);
		fontSizeField.setBorder(new CompoundBorder(
			new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(2, 3, 2, 3)
		));
		fontSizeField.setToolTipText("Name font size (px). Default " + TeamService.DEFAULT_NAME_FONT_SIZE + " matches Player Indicators.");
		fontSizeField.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				syncFontSizeFromField(true);
			}
		});

		boldButton = new JToggleButton("B");
		boldButton.setFocusable(false);
		boldButton.setSelected(teamData.isNameBold());
		boldButton.setToolTipText("Bold name tags");
		boldButton.setPreferredSize(new Dimension(22, 22));
		boldButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		boldButton.setForeground(Color.WHITE);
		boldButton.addActionListener(e ->
		{
			teamData.setNameBold(boldButton.isSelected());
			updateBoldButtonStyle();
			notifyChange();
		});
		updateBoldButtonStyle();

		ColorJButton colorButton = new ColorJButton("", teamData.getColor());
		colorButton.setFocusable(false);
		colorButton.setPreferredSize(new Dimension(22, 22));
		colorButton.setMinimumSize(new Dimension(22, 22));
		colorButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				String pickerTitle = teamData.getName();
				if (pickerTitle == null || pickerTitle.isEmpty())
				{
					pickerTitle = "Team color";
				}
				RuneliteColorPicker colorPicker = colorPickerManager.create(
					TeamBoxPanel.this,
					colorButton.getColor(),
					pickerTitle,
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

		JLabel dragHandle = new JLabel("::");
		dragHandle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		dragHandle.setToolTipText("Drag to reorder. Top team has priority for duplicate members.");
		dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		dragHandle.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (reorderListener != null)
				{
					reorderListener.beginReorder(TeamBoxPanel.this);
				}
			}
		});

		JPanel header = new JPanel(new BorderLayout(4, 0));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		JPanel controls = new JPanel(new BorderLayout(2, 0));
		controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		controls.add(fontSizeField, BorderLayout.WEST);
		controls.add(boldButton, BorderLayout.CENTER);
		controls.add(colorButton, BorderLayout.EAST);

		header.add(dragHandle, BorderLayout.WEST);
		header.add(nameField, BorderLayout.CENTER);
		header.add(controls, BorderLayout.EAST);

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
		teamData.setName(nameField.getText().trim());
		teamData.setMembers(membersArea.getText());
		syncFontSizeFromField(false);
		teamData.setNameBold(boldButton.isSelected());
		return teamData;
	}

	private void syncFontSizeFromField(boolean notify)
	{
		try
		{
			int size = Integer.parseInt(fontSizeField.getText().trim());
			size = TeamService.resolveFontSize(size);
			int previous = teamData.getNameFontSize();
			int stored = size == TeamService.DEFAULT_NAME_FONT_SIZE && !boldButton.isSelected() ? 0 : size;
			teamData.setNameFontSize(stored);

			String normalized = Integer.toString(size);
			if (!normalized.equals(fontSizeField.getText()))
			{
				fontSizeField.setText(normalized);
			}

			if (notify && stored != previous)
			{
				notifyChange();
			}
		}
		catch (NumberFormatException ex)
		{
			int fallback = teamData.getNameFontSize() <= 0
				? TeamService.DEFAULT_NAME_FONT_SIZE
				: TeamService.resolveFontSize(teamData.getNameFontSize());
			String fallbackText = Integer.toString(fallback);
			if (!fallbackText.equals(fontSizeField.getText()))
			{
				fontSizeField.setText(fallbackText);
			}
		}
	}

	private void updateBoldButtonStyle()
	{
		if (boldButton.isSelected())
		{
			boldButton.setBorder(new MatteBorder(1, 1, 1, 1, ColorScheme.BRAND_ORANGE));
		}
		else
		{
			boldButton.setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		}
	}

	private void notifyChange()
	{
		onChange.accept(null);
	}
}
