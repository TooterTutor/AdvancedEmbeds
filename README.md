## TooterWorks plugin directory

Use `/directory browse` for a private Components V2 plugin browser. Administrators can publish, update, and reload a persistent catalog, starting with ModularPacks. See [DIRECTORY.md](DIRECTORY.md) for setup, metadata, commands, and validation.

# EmbedBot - Professional Discord Embed Creator

<div align="center">

**A powerful, modern Discord bot for creating beautiful embeds with an intuitive interface**

[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.java.net/)
[![JDA 6.3.0](https://img.shields.io/badge/JDA-6.3.0-blue.svg)](https://github.com/discord-jda/JDA)
[![Components V2](https://img.shields.io/badge/Discord-Components%20V2-blurple.svg)](https://discord.com/)
[![License](https://img.shields.io/badge/License-Educational-green.svg)](LICENSE)

[Features](#features) • [Installation](#installation) • [Usage](#usage) • [Commands](#discord-commands) • [Architecture](#architecture) • [Documentation](#documentation)

</div>

---

## 🎯 What is EmbedBot?

EmbedBot is a sophisticated Discord bot that makes creating and managing rich Discord embeds effortless. Whether you're building announcement channels, documentation, server info displays, or any other rich content, EmbedBot provides a professional, user-friendly interface directly within Discord.

Unlike simple embed generators, EmbedBot offers:
- **In-Discord editing**: Build embeds using Discord's modern Components V2 interface
- **Multi-embed sessions**: Create and manage multiple embeds simultaneously
- **Live preview**: See your embeds update in real-time as you edit
- **Persistent configuration**: All settings auto-save and survive restarts
- **Webhook integration**: Publish to multiple destinations with labeled webhooks
- **Optional GUI**: Beautiful JavaFX interface for desktop management

## ✨ Features

### 🎨 Advanced Embed Builder
- **Full Customization**: Title, description, color, author, footer, timestamp, fields, images
- **Components V2 Interface**: Modern Discord UI with containers, sections, separators, and rich text
- **Multi-Embed Sessions**: Work on up to 10 embeds at once with easy navigation
- **Live Preview**: Real-time embed preview updates as you edit
- **Smart Defaults**: Sensible starting values for quick embed creation
- **Copy Function**: Clone existing embeds from messages or URLs
- **Field Management**: Add, edit, remove, and organize embed fields
- **Modal-Based Editing**: Clean, focused editing experience for each property

### 🖥️ JavaFX GUI (Optional)
- **Material Design Theme**: Catppuccin Mocha dark theme with professional aesthetics
- **Token Management**: Secure password-masked token input
- **Real-Time Logs**: Live console output viewer with auto-scroll
- **Lifecycle Controls**: Start/stop bot with one click
- **Status Indicators**: Visual feedback for bot state
- **Cross-Platform**: Works on Linux, Windows, and macOS

### 🔗 Webhook & Publishing
- **Multi-Webhook Support**: Configure multiple webhook targets per server
- **Label-Based Management**: Easy-to-remember webhook labels
- **Flexible Publishing**: Send to channels or webhooks
- **Automatic Saving**: Webhook configurations persist across restarts
- **Channel Controls**: Dedicated builder and publish channels per guild

### ⚡ Modern Architecture
- **Session-Based**: Each user gets their own editing session with 30-minute TTL
- **Thread-Safe**: Concurrent collections for multi-user safety
- **Component Namespacing**: Clean `eb:v2:` prefix prevents ID conflicts
- **Automatic Persistence**: Config auto-saves to JSON
- **JDA 6.3.0**: Latest Discord API with full gateway intent support
- **Java 17+**: Modern Java with records, pattern matching, and sealed classes

## 📋 Prerequisites

| Requirement | Version | Notes |
|------------|---------|-------|
| **Java** | 17+ | [Download OpenJDK](https://openjdk.java.net/) |
| **Discord Bot** | N/A | [Create bot](https://discord.com/developers/applications) |
| **Permissions** | Required | Send Messages, Embed Links, Use Slash Commands |
| **Intents** | Required | GUILD_MEMBERS, GUILD_MESSAGES |

## 📦 Installation

### Option 1: Pre-built JAR (Recommended)

1. **Download** the latest `embedbot-1.1-SNAPSHOT.jar` from releases
2. **Verify Java version**:
   ```bash
   java -version
   # Should show version 17 or higher
   ```
3. **Run the bot**:
   ```bash
   java -jar embedbot-1.1-SNAPSHOT.jar
   ```

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/EmbedBot.git
cd EmbedBot

# Build with Maven
mvn clean package

# Run the compiled JAR
java -jar target/embedbot-1.1-SNAPSHOT.jar
```

## 🚀 Usage

### GUI Mode (Default)

**Best for**: Desktop users, development, visual feedback

```bash
java -jar embedbot-1.1-SNAPSHOT.jar
```

**GUI Features**:
1. **Token Input**: Paste your Discord bot token (masked for security)
2. **Start/Stop**: Control bot lifecycle with buttons
3. **Console**: View real-time logs with color-coded messages
4. **Status Bar**: See connection status at a glance

### Headless Mode (CLI)

**Best for**: Linux servers, Docker containers, automation

```bash
# Option 1: Environment variable
export DISCORD_TOKEN="your_bot_token_here"
java -jar embedbot-1.1-SNAPSHOT.jar --no-gui

# Option 2: .env file (recommended)
echo "DISCORD_TOKEN=your_bot_token_here" > .env
java -jar embedbot-1.1-SNAPSHOT.jar --headless

# Option 3: Docker-friendly
docker run -e DISCORD_TOKEN="token" embedbot --no-gui
```

### Development Mode (Fast Commands)

**For testing**: Commands update instantly instead of waiting up to 1 hour

```bash
export GUILD_ID="your_server_id"
export DISCORD_TOKEN="your_token"
java -jar embedbot-1.1-SNAPSHOT.jar
```

## 📚 Discord Commands

### Core Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/embed new` | Start a new embed editing session | `/embed new` |
| `/embed edit` | Resume your active session | `/embed edit` |
| `/embed cancel` | Cancel and delete your session | `/embed cancel` |
| `/embed publish` | Send embeds to publish channel or webhook | `/embed publish` |
| `/embed help` | Show comprehensive help message | `/embed help` |

### Advanced Commands

| Command | Description | Example |
|---------|-------------|---------|
| `/embed copy [message_id]` | Clone an embed from a message | `/embed copy 123456789` |
| `/embed copy [url]` | Clone an embed from a message URL | `/embed copy https://discord.com/...` |
| `/embed set-builder-channel [channel]` | Set where embed UI appears | `/embed set-builder-channel #embeds` |
| `/embed set-publish-channel [channel]` | Set default publish destination | `/embed set-publish-channel #announcements` |

### Webhook Management

| Command | Description | Example |
|---------|-------------|---------|
| `/embed webhook-add [label] [url]` | Add a named webhook target | `/embed webhook-add updates https://...` |
| `/embed webhook-remove [label]` | Remove a webhook by label | `/embed webhook-remove updates` |
| `/embed webhook-list` | List all configured webhooks | `/embed webhook-list` |

## 🎨 Embed Building Interface

When you run `/embed new` or `/embed edit`, you'll see a modern two-message interface:

### Control Panel (Bottom Message)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
## 🎨 Embed Builder (Embed 1/3)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 Mode Selector
┌─────────────────────────┐
│ ✏️ Main  📋 Fields      │
│ 🔀 Reorder  ⚙️ Misc     │
└─────────────────────────┘

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✏️ Main Editing
Edit title, description, color, and images

[📌 Title/URL] [📄 Description]
[🎨 Color] [🖼️ Images]
[👤 Author] [🔖 Footer]
[⏱️ Timestamp]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Navigation
[⬅️ Prev] [➡️ Next] [✅ Done]
```

### Live Preview (Top Message)

Shows your embed updating in real-time as you edit. Supports all Discord embed features including custom colors, images, fields, timestamps, and more.

### Editing Modes

#### ✏️ Main Mode
**Purpose**: Edit core embed properties

**Actions**:
- **📌 Title/URL**: Set title text and optional hyperlink
- **📄 Description**: Set main embed description (supports Markdown)
- **🎨 Color**: Set embed accent color (hex format: `#FF5733`)
- **🖼️ Images**: Set thumbnail (small image) and main image (large)
- **👤 Author**: Set author section (name, URL, icon)
- **🔖 Footer**: Set footer section (text, icon)
- **⏱️ Timestamp**: Toggle current timestamp on/off

#### 📋 Fields Mode *(Coming Soon)*
**Purpose**: Manage embed fields

**Planned Actions**:
- Add new fields (name/value pairs)
- Edit existing fields
- Remove fields
- Reorder fields
- Toggle inline layout

#### 🔀 Reorder Mode
**Purpose**: Manage multiple embeds in session

**Actions**:
- **➕ Add Embed**: Create a new blank embed (max 10)
- **🗑️ Delete**: Remove current embed from session
- **⬅️ Move Left**: Move embed earlier in sequence
- **➡️ Move Right**: Move embed later in sequence
- **Smart Buttons**: Disabled when at boundaries (first/last)

#### ⚙️ Misc Mode
**Purpose**: Additional options and settings

**Actions**:
- **📤 Export**: Copy embed as JSON
- **📥 Import**: Load embed from JSON
- **🔄 Reset**: Clear current embed
- **📋 Duplicate**: Clone current embed

### Modal Editing

All text inputs use Discord modals for a clean editing experience:

```
┌─────────────────────────────────┐
│   Edit Embed Title              │
├─────────────────────────────────┤
│                                 │
│  Title Text                     │
│  ┌─────────────────────────┐    │
│  │ My Awesome Embed        │    │
│  └─────────────────────────┘    │
│                                 │
│  URL (optional)                 │
│  ┌─────────────────────────┐    │
│  │ https://example.com     │    │
│  └─────────────────────────┘    │
│                                 │
│       [Cancel]  [Submit]        │
└─────────────────────────────────┘
```

## ⚙️ Configuration

### Automatic Persistence

**All settings are automatically saved** and persist across bot restarts.

#### `.env` File (Bot Token)
```env
# Required: Your Discord bot token
DISCORD_TOKEN=your_bot_token_here

# Optional: Server ID for instant command updates (dev only)
GUILD_ID=1234567890
```

#### `embedbot-config.json` (Guild Settings)
Auto-generated JSON file storing per-server configuration:

```json
{
  "1234567890": {
    "builderChannel": 9876543210,
    "publishChannel": 1111222233,
    "webhooks": [
      {
        "label": "updates",
        "url": "https://discord.com/api/webhooks/..."
      },
      {
        "label": "announcements",
        "url": "https://discord.com/api/webhooks/..."
      }
    ]
  }
}
```

**What's Saved**:
- ✅ Builder channels (where embed UI appears)
- ✅ Publish channels (default destination)
- ✅ Webhook targets (labeled URLs)
- ✅ Per-guild settings isolated by server ID

**When It Saves**:
- Immediately after `/embed set-builder-channel`
- Immediately after `/embed set-publish-channel`
- Immediately after `/embed webhook-add` or `/embed webhook-remove`

### Creating a Discord Bot

1. **Go to Discord Developer Portal**: https://discord.com/developers/applications
2. **Create New Application**: Click "New Application", give it a name
3. **Create Bot User**: Navigate to "Bot" tab, click "Add Bot"
4. **Get Token**: Click "Reset Token", copy the token (save securely!)
5. **Enable Intents**: 
   - Enable `SERVER MEMBERS INTENT`
   - Enable `MESSAGE CONTENT INTENT`
6. **Invite Bot**: Use OAuth2 URL Generator with scopes:
   - `bot`
   - `applications.commands`
   - Permissions: Send Messages, Embed Links, Use Slash Commands

## 🏗️ Architecture

### Project Structure

```
src/main/java/io/github/tootertutor/embedbot/
├── Bot.java                    # JDA bootstrap, lifecycle management
├── Launcher.java               # Entry point (GUI/CLI routing)
├── EmbedController.java        # Discord event dispatcher
├── Util.java                   # Helper methods
├── api/
│   └── WebhookClient.java      # HTTP webhook posting
├── config/
│   ├── ConfigPersistence.java  # JSON save/load
│   ├── GuildConfig.java        # Per-guild settings store
│   └── model/
│       └── WebhookTarget.java  # Webhook data model
├── gui/
│   └── BotGUI.java             # JavaFX desktop interface
├── interaction/
│   └── EmbedInteractionHandler.java  # Component V2 event router
├── model/
│   ├── EmbedDraft.java         # Mutable embed builder wrapper
│   └── MenuMode.java           # UI mode enum (MAIN, FIELDS, etc.)
├── session/
│   ├── EditingSession.java     # User session state
│   └── SessionStore.java       # Thread-safe session manager
├── ui/
│   └── EmbedUiService.java     # Component V2 UI renderer
└── util/
    └── EmbedMapper.java        # Embed conversion utilities
```

### Design Patterns

#### Session-Based Architecture
- Each user gets isolated `EditingSession` with 30-minute TTL
- Automatic cleanup of expired sessions
- Thread-safe `ConcurrentHashMap` for multi-user support
- Session stores: drafts, selected index, mode, message IDs

#### Component Namespacing
- All component IDs prefixed with `eb:v2:`
- Format: `eb:v2:action:param1:param2`
- Examples: `eb:v2:mode:main`, `eb:v2:title`, `eb:v2:nav:prev`
- Prevents conflicts with other bots

#### Two-Message UI
- **Control Message**: Interactive buttons, selects, navigation
- **Preview Message**: Live embed preview, updates automatically
- Reduces clutter, improves UX

#### Modal-Based Editing
- Labels wrap `TextInput` for better UX
- Multi-field modals for related properties (e.g., title + URL)
- Validation before submission
- Clear error messages

#### Configuration Persistence
- Immediate save on config change
- Atomic writes with Gson
- No manual save commands needed
- Graceful degradation on file errors

### Key Components Explained

#### `Bot.java`
- Bootstraps JDA with gateway intents
- Registers slash commands (global or guild-specific)
- Manages bot lifecycle (start/stop)
- Resolves token from environment/properties/.env

#### `SessionStore.java`
- Thread-safe session storage with `ConcurrentHashMap`
- Automatic expiration after TTL (30 minutes)
- Cleanup thread for memory management
- Per-user session isolation

#### `EmbedUiService.java`
- Renders Components V2 layouts
- Generates mode-specific button rows
- Creates containers, sections, separators
- Handles emoji, colors, formatting

#### `EmbedInteractionHandler.java`
- Routes slash commands, buttons, selects, modals
- Validates user permissions
- Updates sessions and UI
- Handles publish logic

#### `ConfigPersistence.java`
- Loads/saves `embedbot-config.json`
- Atomic file writes
- Error handling and logging
- Gson serialization

## 🎨 GUI Theming

The JavaFX GUI uses the **Catppuccin Mocha** color palette for a modern, soothing aesthetic:

| Element | Color | Hex |
|---------|-------|-----|
| Background | Base | `#1e1e2e` |
| Surface | Surface0 | `#313244` |
| Text | Text | `#cdd6f4` |
| Primary | Blue | `#89b4fa` |
| Success | Green | `#a6e3a1` |
| Error | Red | `#f38ba8` |
| Warning | Yellow | `#f9e2af` |

Styles are defined in `src/main/resources/styles.css` with CSS classes for reusable theming.

## 📖 Documentation

- **[COMPONENTS_V2.md](COMPONENTS_V2.md)**: Deep dive into Discord Components V2 implementation
- **[DiscordComponents.md](DiscordComponents.md)**: Legacy component reference

## 🛠️ Development

### Building

```bash
# Clean build
mvn clean package

# Skip tests (faster)
mvn clean package -DskipTests

# With dependencies (shaded JAR)
mvn clean package shade:shade
```

### Running Tests

```bash
mvn test
```

### IDE Setup

**IntelliJ IDEA**:
1. File → Open → Select `pom.xml`
2. Trust project, wait for Maven import
3. Run → Edit Configurations → Add Application
4. Main class: `io.github.tootertutor.embedbot.Launcher`

**Eclipse**:
1. File → Import → Maven → Existing Maven Projects
2. Select project directory
3. Run As → Java Application → Select `Launcher`

**VS Code**:
1. Open folder in VS Code
2. Install "Extension Pack for Java"
3. F5 to run/debug

### Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| JDA | 6.3.0 | Discord API wrapper |
| JavaFX | 21.0.1 | GUI framework |
| Logback | 1.5.6 | Logging |
| Gson | 2.10.1 | JSON serialization |
| java-dotenv | 5.2.2 | .env file parsing |

## ❓ Troubleshooting

### Common Issues

#### "NoClassDefFoundError" when running JAR
**Problem**: Missing dependencies
**Solution**: Use the shaded JAR from `target/embedbot-1.1-SNAPSHOT.jar` which includes all dependencies

#### GUI doesn't launch
**Problem**: JavaFX not found
**Solution**: 
- Ensure Java 17+ is installed
- JavaFX is bundled in the shaded JAR
- Try headless mode: `java -jar embedbot.jar --no-gui`

#### Bot doesn't respond to commands
**Problem**: Commands not registered or permissions missing
**Solutions**:
1. Check bot has these permissions: Send Messages, Embed Links, Use Slash Commands
2. Set `GUILD_ID` for instant updates (dev only)
3. Wait up to 1 hour for global command propagation
4. Check console logs for errors
5. Verify intents are enabled in Developer Portal

#### Token errors
**Problem**: Invalid or missing token
**Solutions**:
1. Ensure token has no extra spaces or quotes
2. Use **Bot** token, not User token
3. Check token is set in `.env` or environment
4. Verify token in Discord Developer Portal

#### "Interaction already acknowledged"
**Problem**: Double-acknowledgment bug (fixed in latest version)
**Solution**: Update to latest version where `event.editComponents()` is used

#### Session expired
**Problem**: Sessions expire after 30 minutes of inactivity
**Solution**: Start a new session with `/embed new` or `/embed edit`

### Debug Mode

Enable verbose logging:

```bash
# Set log level to DEBUG
export LOG_LEVEL=DEBUG
java -jar embedbot-1.1-SNAPSHOT.jar
```

Check `logs/` directory for detailed logs.

## 📝 License

This project is provided **as-is for educational and personal use**. 

Feel free to:
- ✅ Use for personal Discord servers
- ✅ Learn from the code
- ✅ Fork and modify for your needs

Please:
- ❌ Don't sell or redistribute commercially
- ❌ Don't claim as your own work
- ❌ Don't use for malicious purposes

## 🙏 Credits

Built with love using these amazing libraries:

- **[JDA](https://github.com/discord-jda/JDA)** - Powerful Java wrapper for Discord API
- **[JavaFX](https://openjfx.io/)** - Modern, rich GUI framework
- **[Catppuccin](https://github.com/catppuccin/catppuccin)** - Soothing pastel theme
- **[java-dotenv](https://github.com/cdimascio/dotenv-java)** - Environment variable management
- **[Gson](https://github.com/google/gson)** - JSON serialization library
- **[Logback](https://logback.qos.ch/)** - Reliable logging framework

Special thanks to:
- Discord API team for Components V2
- JDA maintainers for excellent documentation
- Catppuccin theme creators for the beautiful color palette

---

<div align="center">

**Made with 🍪 and 🥛 by TooterTutor**

[Report Bug](https://github.com/yourusername/EmbedBot/issues) • [Request Feature](https://github.com/yourusername/EmbedBot/issues) • [Documentation](https://github.com/yourusername/EmbedBot/wiki)

</div>
