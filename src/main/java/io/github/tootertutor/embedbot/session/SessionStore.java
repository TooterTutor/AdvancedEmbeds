package io.github.tootertutor.embedbot.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe session registry with TTL-based cleanup.
 */
public class SessionStore {
    private record SessionKey(long guildId, long userId, long channelId) {
    }

    private final ConcurrentHashMap<SessionKey, EditingSession> sessions = new ConcurrentHashMap<>();
    private final Duration ttl;

    public SessionStore(Duration ttl) {
        this.ttl = ttl;
    }

    public EditingSession getOrCreate(long guildId, long userId, long channelId) {
        return sessions.computeIfAbsent(new SessionKey(guildId, userId, channelId),
                key -> new EditingSession(userId, guildId, channelId));
    }

    public Optional<EditingSession> get(long guildId, long userId, long channelId) {
        return Optional.ofNullable(sessions.get(new SessionKey(guildId, userId, channelId)));
    }

    public void remove(long guildId, long userId, long channelId) {
        sessions.remove(new SessionKey(guildId, userId, channelId));
    }

    public int sweepExpired() {
        Instant cutoff = Instant.now().minus(ttl);
        int[] removed = new int[] { 0 };
        sessions.forEach((key, session) -> {
            if (session.lastTouched().isBefore(cutoff)) {
                sessions.remove(key);
                removed[0] += 1;
            }
        });
        return removed[0];
    }

    public Map<Long, EditingSession> activeSessionsForGuild(long guildId) {
        return sessions.entrySet().stream()
                .filter(e -> e.getKey().guildId == guildId)
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().userId, Map.Entry::getValue));
    }
}
