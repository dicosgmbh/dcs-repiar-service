package de.dicos.repairservice;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public record Config(int port, String contextPath, String user, String password, boolean isTest)
{

	public static Config load(Optional<String> profile)
	{
		Properties props = new Properties();
		Stream.concat(Stream.of("application.properties"),
			profile.map(p -> "application-" + p + ".properties").stream())
			.forEach(name -> readInto(props, name));

		return new Config(
			Integer.parseInt(props.getProperty("server.port")),
			props.getProperty("server.context-path"),
			props.getProperty("security.user.name"),
			props.getProperty("security.user.password"),
			// Boolean.parseBoolean(props.getProperty("isTest"))
			false);
	}

	private static void readInto(Properties props, String resource)
	{
		try (InputStream in = Config.class.getClassLoader().getResourceAsStream(resource)) {
			if (in != null) {
				props.load(in);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
