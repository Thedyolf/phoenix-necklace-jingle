package com.phoenixNecklaceJingle;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.sound.sampled.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.RuneLite;
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
	@Inject
	private Client client;

	@Inject
	private PhoenixNecklaceJingleConfig config;

	@Override
	protected void startUp() throws Exception
	{	}

	@Override
	protected void shutDown() throws Exception
	{	}

    private static final String DEFAULT_SUBPATH = "PhoenixNecklaceJingle/custom.wav";

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

    private void playCustomSound()
    {
        ensureDefaultSoundExists();
        File soundFile = resolveSoundFile();
        if (!soundFile.exists())
        {
            log.warn("Custom sound not found at {}", soundFile.getAbsolutePath());
            return;
        }

        byte[] audioBytes;
        AudioFormat format;

        try (InputStream fis = new BufferedInputStream(Files.newInputStream(soundFile.toPath()));
             AudioInputStream ais = AudioSystem.getAudioInputStream(fis))
        {
            format = ais.getFormat();

            int frameLen = (int) ais.getFrameLength();
            int frameSize = format.getFrameSize();
            if (frameLen != AudioSystem.NOT_SPECIFIED && frameSize > 0)
            {
                int numBytes = frameLen * frameSize;
                audioBytes = new byte[numBytes];
                int read = 0, r;
                while (read < numBytes && (r = ais.read(audioBytes, read, numBytes - read)) != -1) read += r;
            }
            else
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
                byte[] buf = new byte[8192];
                int r;
                while ((r = ais.read(buf)) != -1) bos.write(buf, 0, r);
                audioBytes = bos.toByteArray();
            }
        }
        catch (UnsupportedAudioFileException e)
        {
            log.warn("Unsupported audio file (use PCM 16-bit WAV): {}", soundFile.getAbsolutePath(), e);
            return;
        }
        catch (IOException e)
        {
            log.warn("Unable to load custom sound: {}", soundFile.getAbsolutePath(), e);
            return;
        }

        try
        {
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info))
            {
                log.warn("Audio line not supported for format: {}", format);
                return;
            }

            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP)
                {
                    clip.close();
                }
            });

            clip.open(format, audioBytes, 0, audioBytes.length);

            try
            {
                // Example: config.volume() returns 0..100
                int vol = config.volume(); // adjust to your config type/range
                if (vol < 0) vol = 0;
                if (vol > 100) vol = 100;

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
                {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float min = gain.getMinimum(); // typically around -80.0 dB
                    float max = gain.getMaximum(); // typically around 6.0 dB
                    float dB = min + (max - min) * (vol / 100.0f);
                    gain.setValue(dB);
                }
            }
            catch (IllegalArgumentException ignored) {}

            clip.setFramePosition(0);
            clip.start();
        }
        catch (LineUnavailableException e)
        {
            log.warn("Failed to open audio line for custom sound.", e);
        }
    }

	@Subscribe
	private void onChatMessage (ChatMessage event) {
		if (event.getType() == ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) {
			String message = Text.standardize(event.getMessageNode().getValue());
			if (message.contains("your phoenix necklace heals you, but is destroyed in the process.")) {
                if (this.config.enableCustomSoundsVolume()){
                    playCustomSound();
                }
                else {
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
