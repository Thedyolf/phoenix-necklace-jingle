package com.phoenixNecklaceJingle;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.RuneLite;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@PluginDescriptor(
	name = "Phoenix Necklace Jingle"
)
public class PhoenixNecklaceJinglePlugin extends Plugin
{
    @Inject private Client client;
    @Inject private PhoenixNecklaceJingleConfig config;
    @Inject private AudioPlayer audioPlayer;

    private static final String DEFAULT_SUBPATH = "PhoenixNecklaceJingle/custom.wav";

    @Override
    protected void startUp() throws Exception
    {
        // Create the empty WAV at startup if it's missing
        ensureDefaultSoundExists();
    }

    @Override
    protected void shutDown() throws Exception
    { }

    private File resolveSoundFile()
    {
        File rlDir = RuneLite.RUNELITE_DIR;
        return new File(rlDir, DEFAULT_SUBPATH);
    }

    private void ensureDefaultSoundExists()
    {
        File out = resolveSoundFile();
        if (out.exists()) return;

        out.getParentFile().mkdirs();
        try (InputStream in = getClass().getResourceAsStream("/custom.wav"))
        {
            if (in != null)
            {
                Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            else
            {
                log.warn("Bundled default sound missing at /custom.wav");
            }
        }
        catch (IOException e)
        {
            log.warn("Could not write default sound to {}", out, e);
        }
    }
    private static float volumePercentToDb(int volPercent)
    {
        int v = Math.max(0, Math.min(100, volPercent));
        return -30.0f + (v / 100.0f) * 30.0f;
    }

    private void playCustomSound()
    {
        ensureDefaultSoundExists();
        File soundFile = resolveSoundFile();
        if (!soundFile.exists())
        {
            log.warn("Custom sound not found at {}", soundFile.getAbsolutePath());
            return;
        }

        try
        {
            float gainDb = volumePercentToDb(config.volume());
            audioPlayer.play(soundFile, gainDb);
        }
        catch (Exception e)
        {
            log.warn("Unable to play custom sound: {}", soundFile.getAbsolutePath(), e);
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage event)
    {
        if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
        {
            String message = Text.standardize(event.getMessageNode().getValue());
            if (message.contains("your phoenix necklace heals you, but is destroyed in the process."))
            {
                if (this.config.enableCustomSoundsVolume())
                {
                    playCustomSound(); // will be silent if the empty WAV hasn't been replaced
                }
                else
                {
                    client.playSoundEffect(this.config.soundID(), this.config.volume());
                }
            }
        }
    }

    @Provides
    PhoenixNecklaceJingleConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PhoenixNecklaceJingleConfig.class);
    }
}