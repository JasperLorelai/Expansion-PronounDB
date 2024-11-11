package eu.jasperlorelai.pronoundb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import me.clip.placeholderapi.expansion.Cleanable;
import me.clip.placeholderapi.expansion.Taskable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PronounDBExpansion extends PlaceholderExpansion implements Taskable, Listener, Cleanable {

	private static final Pronouns pronouns = new Pronouns();

	private static final String IDENTIFIER = "pronoundb";
	private static final String AUTHOR = "JasperLorelai";
	private static final String VERSION = "v0.0.1";

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
		pronouns.cacheOnline();
	}

	@Override
	public void stop() {
		pronouns.clear();
	}

	@Override
	public void cleanup(Player player) {
		pronouns.voidCache(player);
	}

	@EventHandler
	private void onJoin(PlayerJoinEvent event) {
		pronouns.pronouns(event.getPlayer().getUniqueId());
	}

	@Nullable
	@Override
	public String onRequest(OfflinePlayer player, @NotNull String params) {
		return player == null ? "" : pronouns.pronouns(player.getUniqueId());
	}

}
