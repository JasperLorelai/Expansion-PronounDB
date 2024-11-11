package eu.jasperlorelai.pronoundb;

import java.util.*;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

public class Pronouns {

	private final Map<UUID, String> PRONOUNS = new HashMap<>();

	private static final String USER_AGENT = "https://github.com/JasperLorelai/Expansion-PronounDB";
	private static final String PRONOUNDB_API = "https://pronoundb.org/api/v2/lookup?platform=minecraft&ids=";

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	public void cacheOnline() {
		List<UUID> uuids = Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).toList();
		StringBuilder builder = new StringBuilder();
		uuids.forEach(uuid -> {
			if (PRONOUNS.containsKey(uuid)) return;
			builder.append(uuid).append(",");
		});
		request(builder.toString(), json -> {
			uuids.forEach(uuid -> parseFor(json, uuid));
			return null;
		});
	}

	public void clear() {
		PRONOUNS.clear();
	}

	public void voidCache(Player player) {
		PRONOUNS.remove(player.getUniqueId());
	}

	public String pronouns(UUID uuid) {
		String pronouns = PRONOUNS.get(uuid);
		if (pronouns != null) return pronouns;
		return request(uuid.toString(), json -> parseFor(json, uuid));
	}

	private String request(String ids, Function<JsonObject, String> function) {
		HttpRequest request = HttpRequest.newBuilder()
				.header("User-Agent", USER_AGENT)
				.uri(URI.create(PRONOUNDB_API + ids))
				.build();

		try {
			HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) return "";

			JsonObject json = new JsonParser().parse(new InputStreamReader(response.body())).getAsJsonObject();
			return function.apply(json);
		} catch (IOException | InterruptedException e) {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			return "";
		}
	}

	private String parseFor(JsonObject json, UUID uuid) {
		if (!json.has(uuid.toString())) return "";

		JsonArray pronounsList = json.getAsJsonObject(uuid.toString())
				.getAsJsonObject("sets")
				.getAsJsonArray("en");
		List<String> pronounStrings = new ArrayList<>();
		for (JsonElement pronoun : pronounsList) {
			if (!pronoun.isJsonPrimitive()) continue;
			pronounStrings.add(pronoun.getAsString());
		}

		String pronouns = String.join("/", pronounStrings);
		PRONOUNS.put(uuid, pronouns);
		return pronouns;
	}

}
