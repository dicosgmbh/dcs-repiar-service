package de.dicos.repairservice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class RepairServiceServerTest
{

	private static final HttpClient client = HttpClient.newHttpClient();

	private static Config config;

	private static HttpServer server;

	@BeforeAll
	static void startServer()
		throws Exception
	{
		Config loaded = Config.load(Optional.of("test"));
		config = new Config(0, loaded.contextPath(), loaded.user(), loaded.password(), true);
		server = RepairServiceServer.start(config);
	}

	@AfterAll
	static void stopServer()
	{
		server.stop(0);
	}

	@Test
	void okIsPublic()
		throws Exception
	{
		HttpResponse<String> response = get("/rest/ok", Optional.empty());

		assertEquals(200, response.statusCode());
		assertEquals("OK", response.body());
	}

	@Test
	void okRequiresExactPath()
		throws Exception
	{
		assertEquals(404, get("/rest/okay", Optional.empty()).statusCode());
	}

	@Test
	void healthRequiresAuth()
		throws Exception
	{
		assertEquals(401, get("/actuator/health", Optional.empty()).statusCode());
		assertEquals(401, get("/actuator/health", Optional.of(config.user() + ":wrong")).statusCode());
	}

	@Test
	void healthWithAuth()
		throws Exception
	{
		HttpResponse<String> response = get("/actuator/health", Optional.of(config.user() + ":" + config.password()));

		assertEquals(200, response.statusCode());
		assertEquals("{\"status\":\"UP\"}", response.body());
	}

	@Test
	void unknownPathRequiresAuth()
		throws Exception
	{
		assertEquals(401, get("/rest/unknown", Optional.empty()).statusCode());
	}

	private static HttpResponse<String> get(String path, Optional<String> credentials)
		throws Exception
	{
		URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + config.contextPath() + path);
		HttpRequest.Builder request = HttpRequest.newBuilder(uri).GET();
		credentials.map(c -> Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_8)))
			.ifPresent(c -> request.header("Authorization", "Basic " + c));
		return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}
}

