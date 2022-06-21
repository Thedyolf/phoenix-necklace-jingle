package com.phoenixNecklaceJingle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("phoenixNecklaceJingle")
public interface PhoenixNecklaceJingleConfig extends Config
{
	@Range(min = 1, max = 50)
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Sound effect volume",
			position = 1
	)
	default int volume() {
		return 20;
	}

}
