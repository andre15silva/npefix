package fr.inria.spirals.npefix.config;

import java.io.IOException;
import java.util.Properties;

public class Config  {
	public static Config CONFIG = new Config();
	private Properties properties = new Properties();
	private Config() {
		try {
			properties.load(getClass().getResource("/config.ini").openStream());
		} catch (IOException e) {
			throw new RuntimeException("Unable to open the configuration.", e);
		}
	}

	public int getTimeoutIteration() {
		return Integer.parseInt(properties.getProperty("iteration.timeout"));
	}

	public int getNbIteration() {
		return Integer.parseInt(properties.getProperty("iteration.count"));
	}

	public int getServerPort() {
		return Integer.parseInt(properties.getProperty("server.port"));
	}
}
