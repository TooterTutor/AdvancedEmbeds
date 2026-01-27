package io.github.tootertutor.embedbot.api;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.tootertutor.embedbot.config.model.WebhookTarget;
import net.dv8tion.jda.api.entities.MessageEmbed;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Lightweight HTTP client for sending embeds to Discord webhooks.
 */
public class WebhookClient {
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final Gson gson;

    public WebhookClient(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public WebhookResult send(WebhookTarget target, List<MessageEmbed> embeds) throws IOException {
        JsonObject payload = new JsonObject();
        JsonArray embedsArray = new JsonArray();

        for (MessageEmbed embed : embeds) {
            embedsArray.add(gson.toJsonTree(embed.toData().toMap()));
        }

        payload.add("embeds", embedsArray);

        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = new Request.Builder()
                .url(target.url() + "?wait=true")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            return new WebhookResult(response.isSuccessful(), response.code(), responseBody);
        }
    }

    public record WebhookResult(boolean success, int code, String body) {
    }
}
