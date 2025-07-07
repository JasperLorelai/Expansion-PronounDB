package eu.jasperlorelai.pronoundb;

import java.util.*;
import java.net.URI;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.io.InputStreamReader;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.player.PlayerJoinEvent;

import me.clip.placeholderapi.expansion.Taskable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PronounDBExpansion extends PlaceholderExpansion implements Taskable, Listener {

	private static final String IDENTIFIER = "pronoundb";
	private static final String AUTHOR = "JasperLorelai";
	private static final String VERSION = "v0.0.1";

	private static final String NOT_FOUND = "N/A";
	private static final String API_URL = "https://pronoundb.org/api/v2/lookup?platform=minecraft&ids=";
	private static final String USER_AGENT = "PronounDBExpansion/" + VERSION + " (https://github.com/JasperLorelai/Expansion-PronounDB)";

	private static BukkitTask REPEATING_TASK;

	private static final Set<UUID> QUEUE = new LinkedHashSet<>();
	private static final Cache<UUID, String> PRONOUNS = CacheBuilder.newBuilder()
			.expireAfterWrite(12, TimeUnit.HOURS)
			.build();

	private static final int ID_LIMIT = 50;
	private static final int TASK_INTERVAL = 5 * 60 * 20;
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	@NotNull
	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@NotNull
	@Override
	public String getAuthor() {
		return AUTHOR;
	}

	@NotNull
	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public void start() {
		Bukkit.getOnlinePlayers()
				.stream()
				.map(Entity::getUniqueId)
				.forEach(QUEUE::add);

		REPEATING_TASK = Bukkit.getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), this::fetchQueued, 0, TASK_INTERVAL);
	}

	@Override
	public void stop() {
		QUEUE.clear();
		REPEATING_TASK.cancel();
		PRONOUNS.invalidateAll();
	}

	@EventHandler
	private void onJoin(PlayerJoinEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		if (PRONOUNS.getIfPresent(uuid) != null) return;
		QUEUE.add(uuid);
	}

	@Nullable
	@Override
	public String onRequest(OfflinePlayer player, @NotNull String params) {
		if (player == null) return null;
		UUID uuid = player.getUniqueId();

		String pronouns = PRONOUNS.getIfPresent(uuid);
		if (pronouns != null) return pronouns;

		QUEUE.add(uuid);
		return NOT_FOUND;
	}

	private void fetchQueued() {
		// Consume the first 50 elements max.
		List<UUID> uuids = new ArrayList<>();
		Iterator<UUID> iterator = QUEUE.iterator();
		for (int i = 0; iterator.hasNext() && i < ID_LIMIT; i++) {
			uuids.add(iterator.next());
			iterator.remove();
		}
		if (uuids.isEmpty()) return;

		String ids = uuids.stream()
				.map(UUID::toString)
				.collect(Collectors.joining(","));

		HttpRequest request = HttpRequest.newBuilder()
				.header("User-Agent", USER_AGENT)
				.uri(URI.create(API_URL + ids))
				.build();

		HttpResponse<InputStream> response;
		try {
			response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) return;
		} catch (IOException | InterruptedException e) {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			return;
		}

		JsonObject json = new JsonParser().parse(new InputStreamReader(response.body())).getAsJsonObject();
		for (UUID uuid : uuids) {
			if (!json.has(uuid.toString())) {
				PRONOUNS.put(uuid, NOT_FOUND);
				continue;
			}

			JsonArray array = json.getAsJsonObject(uuid.toString())
					.getAsJsonObject("sets")
					.getAsJsonArray("en");
			List<String> pronouns = new ArrayList<>();
			for (JsonElement pronoun : array) {
				if (!pronoun.isJsonPrimitive()) continue;
				pronouns.add(pronoun.getAsString());
			}

			PRONOUNS.put(uuid, String.join("/", pronouns));
		}
	}

}
