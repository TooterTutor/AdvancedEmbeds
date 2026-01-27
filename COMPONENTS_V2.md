# Discord Components V2 Implementation Guide

<div align="center">

**Building Rich, Interactive Discord Interfaces with Components V2**

[Overview](#overview) • [Components](#core-components) • [Implementation](#implementation-details) • [Benefits](#benefits) • [Migration Guide](#migration-from-legacy-components)

</div>

---

## 📖 Overview

EmbedBot leverages Discord's **Components V2 API** to deliver a modern, visually rich interface that feels native to Discord. Components V2 introduces a hierarchical layout system with advanced styling capabilities, transforming simple button-and-menu interactions into polished, card-like experiences.

### What is Components V2?

Components V2 is Discord's next-generation message component framework that enables:
- **Hierarchical Layouts**: Nest components for complex UI structures
- **Rich Text Display**: Markdown-formatted text within message components
- **Visual Separators**: Customizable dividers for better organization
- **Styled Containers**: Accent colors, borders, and spoiler support
- **Accessory Elements**: Icons and thumbnails integrated into sections

### Why Use Components V2?

| Legacy Components | Components V2 |
|------------------|---------------|
| Flat ActionRow layout | Hierarchical Container/Section structure |
| Text-only labels | Rich Markdown TextDisplay |
| No visual separators | Configurable Separator spacing |
| Uniform styling | Custom accent colors per container |
| Basic organization | Professional card-like layouts |

---

## 🔄 Migration from Legacy Components

### Before (Legacy Components)

```java
// ❌ Old approach: Flat, limited structure
List<ActionRow> rows = new ArrayList<>();
rows.add(ActionRow.of(
    Button.primary("title", "Title"),
    Button.primary("desc", "Description")
));
rows.add(ActionRow.of(
    Button.primary("color", "Color"),
    Button.primary("image", "Image")
));
event.editMessageComponents(rows).queue();
```

**Limitations**:
- No contextual information display
- Flat hierarchy, hard to visually group
- No spacing control between sections
- Plain text labels only

### After (Components V2)

```java
// ✅ New approach: Rich, hierarchical structure
List<LayoutComponent> components = new ArrayList<>();

// Header with status info
components.add(Section.of(
    null,
    TextDisplay.of("## 🎨 Embed Builder (Embed 1/3)\n*Edit title, description, color, and images*")
));

components.add(Separator.createDivider(Separator.Spacing.SMALL));

// Styled action container
Container actions = Container.of(
    ActionRow.of(
        Button.primary("eb:v2:title", "Title").withEmoji(Emoji.fromUnicode("📌")),
        Button.primary("eb:v2:desc", "Description").withEmoji(Emoji.fromUnicode("📄"))
    ),
    ActionRow.of(
        Button.primary("eb:v2:color", "Color").withEmoji(Emoji.fromUnicode("🎨")),
        Button.primary("eb:v2:images", "Images").withEmoji(Emoji.fromUnicode("🖼️"))
    )
).withAccentColor(Color.decode("#5865F2"));

components.add(actions);

event.editComponents(components).useComponentsV2().queue();
```

**Advantages**:
- ✅ Rich header with Markdown formatting
- ✅ Visual separators for clear sections
- ✅ Grouped actions in styled container
- ✅ Emoji integration for better UX
- ✅ Discord Blurple accent color

---

## 🧩 Core Components

### 1. Container

**Purpose**: Groups multiple components with optional styling

**Features**:
- Accent color border
- Spoiler support (click to reveal)
- Can contain any LayoutComponent

**Usage**:
```java
Container.of(
    ActionRow.of(Button.primary("id1", "Button 1")),
    ActionRow.of(Button.secondary("id2", "Button 2"))
)
.withAccentColor(Color.decode("#5865F2"))  // Discord Blurple
.withSpoiler(false);
```

**When to Use**:
- Grouping related actions (e.g., all embed editing buttons)
- Adding visual emphasis with accent colors
- Creating card-like sections

---

### 2. Section

**Purpose**: Organizes content with optional accessory element (icon/thumbnail)

**Features**:
- Supports accessory on right side
- Can contain TextDisplay, ActionRows, Containers
- Good for headers and informational blocks

**Usage**:
```java
// Without accessory
Section.of(
    null,
    TextDisplay.of("## 🎯 Quick Actions\n*Frequently used commands*")
);

// With accessory (future feature)
Section.of(
    SectionAccessory.icon("https://example.com/icon.png"),
    TextDisplay.of("**Status:** Online")
);
```

**When to Use**:
- Headers with descriptive text
- Status displays
- Informational blocks
- Future: Thumbnail previews with text

---

### 3. TextDisplay

**Purpose**: Rich text formatting within message components

**Features**:
- Full Markdown support (`**bold**`, `*italic*`, etc.)
- Multi-line text with `\n`
- Emoji support
- Inline code blocks

**Usage**:
```java
// Simple status
TextDisplay.of("📝 **Current:** Embed 1/3 | **Mode:** MAIN");

// Multi-line with formatting
TextDisplay.of("""
    ## 🎨 Embed Builder
    
    **Status:** Editing
    **Progress:** 2/5 fields completed
    """);

// Inline code
TextDisplay.of("Set color using hex format: `#FF5733`");
```

**When to Use**:
- Status headers
- Instructions and help text
- Mode descriptions
- Dynamic information display

---

### 4. Separator

**Purpose**: Visual dividers to organize content sections

**Features**:
- Three spacing sizes: `SMALL`, `MEDIUM`, `LARGE`
- Visible dividers or invisible spacing
- Clean section breaks

**Usage**:
```java
// Visible divider with small spacing
Separator.createDivider(Separator.Spacing.SMALL);

// Invisible spacing (padding)
Separator.createInvisible(Separator.Spacing.MEDIUM);

// Large divider for major sections
Separator.createDivider(Separator.Spacing.LARGE);
```

**When to Use**:
- Between major UI sections
- After headers before action buttons
- Creating breathing room in dense interfaces
- Visual hierarchy enhancement

**Spacing Guide**:
| Spacing | Visual Gap | Use Case |
|---------|-----------|----------|
| `SMALL` | ~8px | Between related groups |
| `MEDIUM` | ~16px | Between sections |
| `LARGE` | ~24px | Between major UI areas |


---

## 📋 Mode-Specific Layouts

### ✏️ Main Mode

**Purpose**: Edit core embed properties (title, description, color, images, author, footer, timestamp)

**Layout**:
```java
Container.of(
    Section.of(null, TextDisplay.of("✏️ **Main Editing**\n*Edit title, description, color, and images*")),
    ActionRow.of(
        Button.primary("eb:v2:title", "Title/URL").withEmoji(Emoji.fromUnicode("📌")),
        Button.primary("eb:v2:desc", "Description").withEmoji(Emoji.fromUnicode("📄"))
    ),
    ActionRow.of(
        Button.primary("eb:v2:color", "Color").withEmoji(Emoji.fromUnicode("🎨")),
        Button.primary("eb:v2:images", "Images").withEmoji(Emoji.fromUnicode("🖼️"))
    ),
    ActionRow.of(
        Button.primary("eb:v2:author", "Author").withEmoji(Emoji.fromUnicode("👤")),
        Button.primary("eb:v2:footer", "Footer").withEmoji(Emoji.fromUnicode("🔖")),
        Button.primary("eb:v2:timestamp", "Timestamp").withEmoji(Emoji.fromUnicode("⏱️"))
    )
).withAccentColor(Color.decode("#5865F2"));
```

**Buttons**:
| Button | Emoji | Action | Modal Fields |
|--------|-------|--------|--------------|
| Title/URL | 📌 | Edit title and optional hyperlink | Title text, URL (optional) |
| Description | 📄 | Edit main embed description | Description (4000 chars) |
| Color | 🎨 | Set embed accent color | Hex color (#RRGGBB) |
| Images | 🖼️ | Set thumbnail and main image | Thumbnail URL, Image URL |
| Author | 👤 | Set author section | Name, URL, Icon URL |
| Footer | 🔖 | Set footer section | Text, Icon URL |
| Timestamp | ⏱️ | Toggle current timestamp | (No modal, toggles on/off) |

---

### 🔀 Reorder Mode

**Purpose**: Manage multiple embeds in session (add, delete, reorder)

**Layout**:
```java
Container.of(
    Section.of(null, TextDisplay.of("🔀 **Manage Embeds**\n*Add, remove, or reorder embeds*")),
    ActionRow.of(
        Button.success("eb:v2:add", "Add Embed").withEmoji(Emoji.fromUnicode("➕")),
        Button.danger("eb:v2:delete", "Delete").withEmoji(Emoji.fromUnicode("🗑️"))
                .withDisabled(session.size() == 1) // Can't delete last embed
    ),
    ActionRow.of(
        Button.secondary("eb:v2:nav:left", "Move Left").withEmoji(Emoji.fromUnicode("⬅️"))
                .withDisabled(session.selectedIndex() == 0), // At first position
        Button.secondary("eb:v2:nav:right", "Move Right").withEmoji(Emoji.fromUnicode("➡️"))
                .withDisabled(session.selectedIndex() == session.size() - 1) // At last position
    )
).withAccentColor(Color.decode("#5865F2"));
```

**Buttons**:
| Button | Emoji | Action | Smart Behavior |
|--------|-------|--------|----------------|
| Add Embed | ➕ | Create new blank embed | Disabled if 10 embeds exist (Discord limit) |
| Delete | 🗑️ | Remove current embed | Disabled if only 1 embed (can't delete last) |
| Move Left | ⬅️ | Swap with previous embed | Disabled when at first position |
| Move Right | ➡️ | Swap with next embed | Disabled when at last position |

---

### 📋 Fields Mode *(Coming Soon)*

**Purpose**: Manage embed fields (name/value pairs with inline option)

**Planned Layout**:
```java
Container.of(
    Section.of(null, TextDisplay.of("📋 **Field Management**\n*Add, edit, and organize embed fields*")),
    ActionRow.of(
        Button.success("eb:v2:field:add", "Add Field").withEmoji(Emoji.fromUnicode("➕")),
        Button.primary("eb:v2:field:edit", "Edit Field").withEmoji(Emoji.fromUnicode("✏️"))
    ),
    ActionRow.of(
        Button.danger("eb:v2:field:remove", "Remove Field").withEmoji(Emoji.fromUnicode("🗑️")),
        Button.secondary("eb:v2:field:clear", "Clear All").withEmoji(Emoji.fromUnicode("🧹"))
    )
).withAccentColor(Color.decode("#5865F2"));
```

**Planned Features**:
- Add fields with name, value, inline option
- Edit existing fields (select from dropdown)
- Remove individual fields
- Clear all fields at once
- Field limit: 25 per embed (Discord max)

---

### ⚙️ Misc Mode *(Coming Soon)*

**Purpose**: Advanced options (export, import, reset, duplicate)

**Planned Layout**:
```java
Container.of(
    Section.of(null, TextDisplay.of("⚙️ **Miscellaneous**\n*Additional options and settings*")),
    ActionRow.of(
        Button.primary("eb:v2:export", "Export JSON").withEmoji(Emoji.fromUnicode("📤")),
        Button.primary("eb:v2:import", "Import JSON").withEmoji(Emoji.fromUnicode("📥"))
    ),
    ActionRow.of(
        Button.secondary("eb:v2:duplicate", "Duplicate").withEmoji(Emoji.fromUnicode("📋")),
        Button.danger("eb:v2:reset", "Reset").withEmoji(Emoji.fromUnicode("🔄"))
    )
).withAccentColor(Color.decode("#5865F2"));
```

**Planned Features**:
- Export current embed as JSON
- Import embed from JSON
- Duplicate current embed
- Reset embed to blank state
- Template system (future)

---

## 🎯 Design Principles

### 1. Visual Hierarchy

**Principle**: Use Components V2 to create clear information architecture

**Implementation**:
- **Headers**: Section + TextDisplay with Markdown headers (`##`)
- **Grouping**: Container with accent color for related actions
- **Separation**: Separator between distinct UI areas
- **Emphasis**: Emoji for quick visual scanning

**Result**: Users can instantly understand the interface structure without reading everything.

---

### 2. Emoji Integration

**Principle**: All buttons use emojis for better UX and visual appeal

**Implementation**:
```java
Button.primary("eb:v2:title", "Title")
    .withEmoji(Emoji.fromUnicode("📌"));
```

**Emoji Mapping**:
| Action | Emoji | Semantic Meaning |
|--------|-------|------------------|
| Title | 📌 | Pin/Important content |
| Description | 📄 | Document/Text |
| Color | 🎨 | Palette/Customization |
| Images | 🖼️ | Picture/Visual |
| Author | 👤 | Person/Identity |
| Footer | 🔖 | Bookmark/Note |
| Timestamp | ⏱️ | Time/Date |
| Add | ➕ | Create/Increase |
| Delete | 🗑️ | Remove/Trash |
| Move Left | ⬅️ | Navigate backward |
| Move Right | ➡️ | Navigate forward |
| Fields | 📋 | List/Form |
| Settings | ⚙️ | Configuration |
| Export | 📤 | Send out |
| Import | 📥 | Bring in |

**Result**: Users recognize actions faster, improved accessibility, consistent visual language.

---

### 3. Accent Colors

**Principle**: Use Discord Blurple (#5865F2) to make action containers visually distinct

**Implementation**:
```java
Container.of(actionRows)
    .withAccentColor(Color.decode("#5865F2"));
```

**Color Usage**:
| Color | Hex | Use Case |
|-------|-----|----------|
| **Discord Blurple** | `#5865F2` | Action containers (primary) |
| **Success Green** | `#3BA55C` | Confirmation actions (future) |
| **Danger Red** | `#ED4245` | Destructive actions (future) |
| **Warning Yellow** | `#FEE75C` | Caution prompts (future) |

**Result**: Visual consistency with Discord's brand, better user engagement.

---

### 4. Smart Button States

**Principle**: Disable buttons when actions aren't available to prevent errors

**Implementation**:
```java
Button.secondary("eb:v2:nav:left", "Move Left")
    .withEmoji(Emoji.fromUnicode("⬅️"))
    .withDisabled(session.selectedIndex() == 0); // First embed
```

**Smart Behaviors**:
| Button | Disabled When | Reason |
|--------|---------------|--------|
| Move Left | `selectedIndex == 0` | Already at first position |
| Move Right | `selectedIndex == size - 1` | Already at last position |
| Delete | `size == 1` | Can't delete last embed |
| Add Embed | `size == 10` | Discord's 10-embed limit |

**Result**: Prevents invalid actions, clearer interface state, better UX.

---

## 💻 Implementation Details

### Code Location

All Components V2 logic is centralized in the UI service layer:

**File**: [src/main/java/io/github/tootertutor/embedbot/ui/EmbedUiService.java](src/main/java/io/github/tootertutor/embedbot/ui/EmbedUiService.java)

**Key Methods**:

| Method | Responsibility | Returns |
|--------|----------------|---------|
| `buildComponentsV2(EditingSession)` | Assembles complete Components V2 layout | `List<LayoutComponent>` |
| `buildHeader(EditingSession)` | Creates header Section with TextDisplay | `Section` |
| `buildModeSelector(EditingSession)` | Creates mode selection StringSelectMenu | `ActionRow` |
| `buildActionContainer(EditingSession)` | Creates mode-specific button Container | `Container` |
| `buildNavigation(EditingSession)` | Creates Prev/Next/Done buttons | `ActionRow` |

---

### Building the Interface

**Step-by-step construction**:

```java
public List<LayoutComponent> buildComponentsV2(EditingSession session) {
    List<LayoutComponent> components = new ArrayList<>();
    
    // 1. Header: Title and mode description
    components.add(buildHeader(session));
    
    // 2. Separator for visual break
    components.add(Separator.createDivider(Separator.Spacing.SMALL));
    
    // 3. Mode selector dropdown
    components.add(buildModeSelector(session));
    
    // 4. Separator before actions
    components.add(Separator.createDivider(Separator.Spacing.SMALL));
    
    // 5. Mode-specific action buttons in styled container
    components.add(buildActionContainer(session));
    
    // 6. Separator before navigation
    components.add(Separator.createDivider(Separator.Spacing.SMALL));
    
    // 7. Navigation controls
    components.add(Section.of(null, buildNavigation(session)));
    
    return components;
}
```

---

### Updating the Interface

**Critical**: Use `event.editComponents()` to both acknowledge interaction AND update UI:

```java
// ✅ CORRECT: Single acknowledgment
public void updateInterface(ButtonInteractionEvent event, EditingSession session) {
    event.editComponents(buildComponentsV2(session))
        .useComponentsV2()  // REQUIRED for Components V2
        .queue();
}

// ❌ WRONG: Double acknowledgment (throws exception)
public void updateInterfaceWRONG(ButtonInteractionEvent event, EditingSession session) {
    event.deferEdit().queue();  // Acknowledges interaction
    event.getMessage()
        .editMessageComponents(buildComponentsV2(session))  // Tries to acknowledge again!
        .queue();  // IllegalStateException!
}
```

**Why this matters**:
- Discord interactions must be acknowledged exactly once
- `event.editComponents()` both acknowledges AND updates in one call
- Separate defer + edit causes `IllegalStateException`

---

### Component Nesting Rules

**Discord's nesting hierarchy**:

```
Message
└─ LayoutComponent
    ├─ Container
    │   ├─ Section
    │   │   └─ (TextDisplay, ActionRow, etc.)
    │   ├─ ActionRow
    │   │   └─ (Button, StringSelectMenu, etc.)
    │   └─ Separator
    ├─ Section (standalone)
    ├─ ActionRow (standalone)
    └─ Separator (standalone)
```

**Nesting limits**:
- ✅ `Container` can contain: Section, ActionRow, Separator, Container
- ✅ `Section` can contain: TextDisplay, ActionRow, Container
- ❌ `ActionRow` cannot contain other ActionRows
- ❌ Maximum 5 ActionRows per message
- ❌ Maximum 5 buttons per ActionRow

---

## ✨ Benefits

### 1. Better Visual Hierarchy
- **Sections** group related content
- **Separators** create clear boundaries
- **Containers** add visual emphasis with accent colors
- **TextDisplay** provides rich contextual information

**Result**: Users understand the interface structure instantly.

---

### 2. Rich Information Display
- Markdown formatting in TextDisplay
- Dynamic status updates (embed count, mode)
- Inline help text with *italic descriptions*
- Headers with `##` for visual weight

**Result**: Less confusion, better discoverability of features.

---

### 3. Professional Appearance
- Discord Blurple accent matches platform aesthetic
- Emoji integration feels native
- Card-like containers appear polished
- Spacing creates breathing room

**Result**: Users trust the bot more, higher engagement.

---

### 4. Improved UX
- Smart button states prevent errors
- Visual scanning with emojis
- Clear mode descriptions
- Intuitive navigation

**Result**: Lower learning curve, fewer support questions.

---

### 5. Future-Proof Architecture
- Built on Discord's latest component system
- Room for new features (Fields, Misc modes)
- Extensible design patterns
- Easy to add new buttons/modes

**Result**: Sustainable codebase, easier maintenance.

---

## ⚙️ Compatibility

### Requirements

| Requirement | Version | Notes |
|-------------|---------|-------|
| **JDA** | 6.3.0+ | Components V2 support added |
| **Discord API** | v10+ | Components V2 available |
| **Java** | 17+ | Modern syntax (records, text blocks) |

### Platform Support

- ✅ **Discord Desktop**: Full support, best experience
- ✅ **Discord Web**: Full support
- ✅ **Discord Mobile**: Full support with adaptive layout
- ✅ **All Discord text channels**: Works everywhere

### Backward Compatibility

- ✅ Legacy slash commands still work
- ✅ Existing sessions unaffected
- ✅ Can mix Components V2 and legacy components
- ✅ Modals use Components V2 patterns (Label-wrapped TextInput)

**Migration**: No breaking changes, existing code works as-is.

---

## 🧪 Testing Guide

### Manual Testing Steps

1. **Start bot**:
   ```bash
   java -jar embedbot-1.1-SNAPSHOT.jar
   ```

2. **Create session** in Discord:
   ```
   /embed new
   ```

3. **Verify Components V2**:
   - ✅ Header shows "## 🎨 Embed Builder"
   - ✅ Separators create visible dividers
   - ✅ Action container has blue accent border
   - ✅ Buttons show emojis before labels
   - ✅ Mode descriptions are italic

4. **Test interactions**:
   - Click mode selector → Layout updates
   - Click "Title" → Modal appears
   - Submit modal → Preview updates
   - Switch to Reorder mode → Different buttons appear

5. **Test smart states**:
   - At first embed → "Move Left" disabled
   - At last embed → "Move Right" disabled
   - Only one embed → "Delete" disabled

### Visual Inspection Checklist

- [ ] Header uses Markdown (`##` and `*italic*`)
- [ ] Separators create visual breaks
- [ ] Container has Discord Blurple accent (#5865F2)
- [ ] All buttons have emoji + label
- [ ] Mode descriptions change per mode
- [ ] Smart buttons disable appropriately
- [ ] Layout is clean and organized

### Error Scenarios

| Scenario | Expected Behavior |
|----------|-------------------|
| Click button twice quickly | Only first click processed |
| Network lag during edit | Graceful retry or timeout |
| Invalid modal input | Clear error message |
| Session expires | "Session not found" message |
| Max embeds reached | "Add Embed" button disabled |

---

## 📚 References

### Official Documentation

- **[Discord Components V2](https://discord.com/developers/docs/interactions/message-components#component-object-component-structure-v2)** - Official Discord API docs
- **[JDA Components V2 Example](https://github.com/discord-jda/JDA/blob/master/src/examples/java/ComponentsV2Example.java)** - Reference implementation
- **[JDA Changelog](https://github.com/discord-jda/JDA/releases/tag/v6.3.0)** - Components V2 release notes

### Related EmbedBot Docs

- **[README.md](README.md)** - Main project documentation
### Community Resources

- **[JDA Discord Server](https://discord.gg/0hMr4ce0tIl3SLv5)** - Get help with JDA
- **[Discord Developers](https://discord.gg/discord-developers)** - Official Discord API community

---

## 🔧 Troubleshooting

### Common Issues

#### "Interaction already acknowledged"

**Problem**: `IllegalStateException` when updating components

**Cause**: Calling both `deferEdit()` and `editComponents()` (double acknowledgment)

**Solution**:
```java
// ✅ Use this
event.editComponents(components).useComponentsV2().queue();

// ❌ Not this
event.deferEdit().queue();
event.getMessage().editMessageComponents(components).queue();
```

---

#### "Unknown component type"

**Problem**: Components don't render correctly

**Cause**: Forgot `.useComponentsV2()` when editing message

**Solution**:
```java
event.editComponents(components)
    .useComponentsV2()  // ← REQUIRED!
    .queue();
```

---

#### Accent color not showing

**Problem**: Container doesn't have colored border

**Cause**: Color format invalid or not supported by client

**Solution**:
```java
// ✅ Use Color.decode() for hex colors
Container.of(...)
    .withAccentColor(Color.decode("#5865F2"));

// ❌ Not raw Color constructor
Container.of(...)
    .withAccentColor(new Color(88, 101, 242)); // May not work
```

---

#### Emojis not displaying

**Problem**: Buttons show `?` instead of emoji

**Cause**: Invalid Unicode or custom emoji ID

**Solution**:
```java
// ✅ Unicode emojis work everywhere
.withEmoji(Emoji.fromUnicode("📌"));

// ⚠️ Custom emojis need correct ID
.withEmoji(Emoji.fromCustom("customName", 123456789L, false));
```

---

## 📝 Best Practices

### 1. Always use `.useComponentsV2()`

When editing messages with Components V2 layouts:
```java
message.editComponents(buildComponentsV2(session))
    .useComponentsV2()
    .queue();
```

### 2. Limit nesting depth

**Recommended**: 2-3 levels max
```java
// ✅ Good: Clear hierarchy
Container.of(
    Section.of(null, TextDisplay.of("...")),
    ActionRow.of(button1, button2)
)
```

**Avoid**: Deep nesting
```java
// ❌ Bad: Too complex
Container.of(
    Container.of(
        Container.of(...)
    )
)
```

### 3. Use Separators strategically

- After headers
- Between major UI sections
- Not after every component (too cluttered)

### 4. Keep TextDisplay concise

- 1-2 lines for headers
- 3-4 lines max for descriptions
- Use Markdown for emphasis, not decoration

### 5. Consistent emoji usage

- Use same emoji for same action across modes
- Choose semantically meaningful emojis
- Don't overuse (not every button needs one)

---

<div align="center">

**Components V2 makes Discord bots feel professional and native**

Built with 💜 by tootertutor

[Back to README](README.md) • [Report Issue](https://github.com/yourusername/EmbedBot/issues)

</div>
