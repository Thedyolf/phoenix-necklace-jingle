package com.phoenixNecklaceJingle;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("phoenixNecklaceJingle")
public interface PhoenixNecklaceJingleConfig extends Config
{
    @ConfigSection(
            name = "Custom Jingle",
            description = "Use a custom jingle when your Phoenix Necklace breaks.",
            position = 1
    )
    String CUSTOM_JINGLE = "customJingle";

	@Range(min = 1, max = 100)
	@ConfigItem(
			keyName = "volume",
			name = "Volume",
			description = "Sound effect volume",
			position = 2
	)
	default int volume() {
		return 100;
	}

    @ConfigItem(
            keyName = "soundID",
            name = "Sound ID",
            description = "The sound ID you wish to play. <br>\" + " +
                    "Sound List: https://oldschool.runescape.wiki/w/List_of_in-game_sound_IDs\"",
            position = 1
    )
    default int soundID()
    {
        return 3924;
    }
    @ConfigItem(
            keyName = "enableCustomSound",
            name = "Enable Custom Sound",
            description = "Use a custom sound to play rather than an in-game sound ID",
            position = 3,
            section = CUSTOM_JINGLE
    )
    default boolean enableCustomSoundsVolume() { return false; }



}
