package com.bingoteamhelper;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamData
{
	private String name;
	private int colorRgb;
	private String members;

	Color getColor()
	{
		return new Color(colorRgb);
	}

	void setColor(Color color)
	{
		this.colorRgb = color.getRGB();
	}
}
