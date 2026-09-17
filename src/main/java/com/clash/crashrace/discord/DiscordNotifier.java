package com.clash.crashrace.discord;

import com.clash.crashrace.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Posts event notifications to a Discord channel via the REST API, authenticated as a bot
 * (Authorization: Bot &lt;token&gt;) rather than a channel webhook. Only ever sends outgoing
 * messages - no gateway connection is opened, so no extra dependency (e.g. JDA) is needed,
 * just the JDK's built-in HTTP client.
 *
 * Every send is fire-and-forget and asynchronous, so it is safe to call from the hang-watcher
 * thread as well as the main thread - useful to get a detection out to Discord immediately,
 * before waiting for the main thread to recover.
 */
public final class DiscordNotifier {

    private static final String API_BASE = "https://discord.com/api/v10";

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DiscordNotifier(JavaPlugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void send(String message) {
        if (!config.discordEnabled()) {
            return;
        }
        String token = config.discordBotToken();
        String channelId = config.discordChannelId();
        if (token.isBlank() || channelId.isBlank()) {
            return;
        }

        String body = "{\"content\":" + jsonString(message) + "}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/channels/" + channelId + "/messages"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bot " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        plugin.getLogger().warning("[CrashRace] Discord通知の送信に失敗しました: " + error.getMessage());
                    } else if (response.statusCode() >= 300) {
                        plugin.getLogger().warning("[CrashRace] Discord通知がHTTP " + response.statusCode()
                                + "で拒否されました: " + response.body());
                    }
                });
    }

    private String jsonString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
