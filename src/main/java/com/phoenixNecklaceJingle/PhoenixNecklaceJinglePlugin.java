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
        ensureEmptyWavExists();
    }

    @Override
    protected void shutDown() throws Exception
    { }

    private File resolveSoundFile()
    {
        File rlDir = RuneLite.RUNELITE_DIR;
        return new File(rlDir, DEFAULT_SUBPATH);
    }

    /**
     * Ensure a valid (header-only) PCM 16-bit WAV exists at the subpath.
     * Writes a RIFF/WAVE header with 0 data-byte payload.
     */
    private void ensureEmptyWavExists()
    {
        File out = resolveSoundFile();
        try
        {
            File parent = out.getParentFile();
            if (parent != null && !parent.exists())
            {
                parent.mkdirs();
            }

            if (!out.exists())
            {
                writeEmptyPcmWav(out, /*sampleRate*/44100, /*channels*/1, /*bits*/16);
                log.info("Created empty WAV: {}", out.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            log.warn("Failed to create empty WAV at {}", out.getAbsolutePath(), e);
        }
    }

    /**
     * Writes a minimal valid WAV (RIFF) header with zero data bytes.
     */
    private static void writeEmptyPcmWav(File file, int sampleRate, int channels, int bitsPerSample) throws IOException
    {
        int dataSize = 0; // empty/silent
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);
        int riffChunkSize = 36 + dataSize; // 4 + (8 + Subchunk1) + (8 + Subchunk2)

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file)))
        {
            // RIFF header
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(riffChunkSize));
            dos.writeBytes("WAVE");

            // fmt  subchunk
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));                   // Subchunk1Size for PCM
            dos.writeShort(Short.reverseBytes((short) 1));              // AudioFormat = 1 (PCM)
            dos.writeShort(Short.reverseBytes((short) channels));       // NumChannels
            dos.writeInt(Integer.reverseBytes(sampleRate));             // SampleRate
            dos.writeInt(Integer.reverseBytes(byteRate));               // ByteRate
            dos.writeShort(Short.reverseBytes((short) blockAlign));     // BlockAlign
            dos.writeShort(Short.reverseBytes((short) bitsPerSample));  // BitsPerSample

            // data subchunk
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));           // Subchunk2Size (0)
            // No data bytes written (empty)
        }
    }

    private static float volumePercentToDb(int volPercent)
    {
        int v = Math.max(0, Math.min(100, volPercent));
        return -30.0f + (v / 100.0f) * 30.0f;
    }

    private void playCustomSound()
    {
        ensureEmptyWavExists();
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