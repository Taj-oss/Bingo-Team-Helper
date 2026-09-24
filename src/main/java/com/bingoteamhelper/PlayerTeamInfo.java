package com.bingoteamhelper;

import java.awt.Color;
import lombok.Value;

@Value
class PlayerTeamInfo
{
	Color color;
	String teamName;
	int fontSize;
	boolean bold;
}
