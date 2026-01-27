package io.github.tootertutor.embedbot.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.tootertutor.embedbot.model.EmbedDraft;
import io.github.tootertutor.embedbot.model.MenuMode;

/**
 * In-memory state for a user's active embed editing session.
 */
public class EditingSession {
    private final long userId;
    private final long guildId;
    private final long channelId;

    private final List<EmbedDraft> drafts = new ArrayList<>();
    private int selected = 0;
    private Instant lastTouched = Instant.now();

    private String previewMessageId;
    private String controlMessageId;
    private MenuMode menuMode = MenuMode.MAIN;

    public EditingSession(long userId, long guildId, long channelId) {
        this.userId = userId;
        this.guildId = guildId;
        this.channelId = channelId;
        EmbedDraft draft = new EmbedDraft();
        draft.setDefaults();
        drafts.add(draft);
    }

    public long userId() {
        return userId;
    }

    public long guildId() {
        return guildId;
    }

    public long channelId() {
        return channelId;
    }

    public List<EmbedDraft> drafts() {
        return drafts;
    }

    public EmbedDraft current() {
        return drafts.get(selected);
    }

    public int size() {
        return drafts.size();
    }

    public int selectedIndex() {
        return selected;
    }

    public void select(int index) {
        if (index < 0) {
            index = 0;
        }
        if (index >= drafts.size()) {
            index = drafts.size() - 1;
        }
        selected = index;
        touch();
    }

    public void addNew() {
        EmbedDraft draft = new EmbedDraft();
        draft.setDefaults();
        drafts.add(draft);
        selected = drafts.size() - 1;
        touch();
    }

    public void deleteSelected() {
        if (drafts.isEmpty()) {
            return;
        }
        drafts.remove(selected);
        if (drafts.isEmpty()) {
            addNew();
            selected = 0;
        } else if (selected >= drafts.size()) {
            selected = drafts.size() - 1;
        }
        touch();
    }

    public void moveSelectedLeft() {
        if (selected <= 0) {
            return;
        }
        EmbedDraft draft = drafts.remove(selected);
        drafts.add(--selected, draft);
        touch();
    }

    public void moveSelectedRight() {
        if (selected >= drafts.size() - 1) {
            return;
        }
        EmbedDraft draft = drafts.remove(selected);
        drafts.add(++selected, draft);
        touch();
    }

    public void touch() {
        lastTouched = Instant.now();
    }

    public Instant lastTouched() {
        return lastTouched;
    }

    public String previewMessageId() {
        return previewMessageId;
    }

    public void previewMessageId(String id) {
        this.previewMessageId = id;
    }

    public String controlMessageId() {
        return controlMessageId;
    }

    public void controlMessageId(String id) {
        this.controlMessageId = id;
    }

    public MenuMode menuMode() {
        return menuMode;
    }

    public void menuMode(MenuMode menuMode) {
        this.menuMode = menuMode;
    }
}
