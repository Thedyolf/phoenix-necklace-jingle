package com.phoenixNecklaceJingle;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PhoenixNecklaceJingleTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PhoenixNecklaceJinglePlugin.class);
		RuneLite.main(args);
	}
}