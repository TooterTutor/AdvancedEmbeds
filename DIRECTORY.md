# TooterWorks plugin directory

The directory is a Components V2 feature built on the existing JDA 6.3 / Maven stack. It runs alongside `/embed` and starts with ModularPacks. No webhook, database, message collector, or privileged message-content intent is required for the directory.

## Setup

1. Build with `mvn verify`, then run the bot as described in the README. Keep the same bot application/token across restarts. The existing JavaFX launcher requires the project's JavaFX-compatible Java runtime (Java 21 recommended); compilation still targets Java 17.
2. On first startup, the bot copies the bundled `plugin-directory.example.json` to `plugin-directory.json` in the working directory. Keep this file on persistent storage. To choose an explicit location, launch with `java -Dtooterworks.catalog=/path/to/plugin-directory.json -jar target/embedbot-1.1-SNAPSHOT.jar`.
3. Review the generated metadata and links. Add your real Discord support forum URL as a link if desired; no channel IDs or invite URLs are guessed. The bundled links point to the ModularPacks GitHub releases, README, source, and issues.
4. Use `/directory browse` to preview privately. An administrator can publish with `/directory publish channel:#plugins`.

Commands register alongside `/embed`, using the existing `GUILD_ID` behavior. Set that environment variable to your test server ID for fast registration; global commands can take time to appear. Ensure the installation has the `bot` and `applications.commands` scopes. Server integration settings can further limit command access.

## Commands

| Command | Access | Behavior |
| --- | --- | --- |
| `/directory browse` | Members | Open a private plugin browser |
| `/directory publish channel:` | Administrator | Create a public TooterWorks landing message |
| `/directory update channel: message:` | Administrator | Refresh a bot-owned directory landing message in place |
| `/directory reload` | Administrator | Validate the on-disk catalog and swap the live snapshot |

The public **Browse plugins** button opens a private browser for the person clicking. Members can choose a plugin, follow its links, and navigate Previous / Next / Home. Menus show at most 25 plugins per page. A single-plugin catalog works with navigation disabled at the boundaries.

Publish deliberately creates a new message each time. To update, enable Discord Developer Mode, copy the original directory message ID, and supply its channel and ID to `/directory update`. Updates reject messages belonging to another bot or lacking the directory landing button. The bot needs View Channel and Send Messages; updates also require Read Message History. The administrator must be able to view the destination. Failures are reported privately and logged.

## Metadata and adding plugins

Edit `plugin-directory.json` on the bot host, then run `/directory reload`. This file is operator-managed; slash commands do not accept arbitrary file paths or remote JSON. One catalog is shared across all servers served by this bot instance. Administrators in any of those servers can trigger a reload of that same local file.

The schema is an object with a `plugins` array. Each entry has:

- `id`: stable unique lowercase slug, 1–40 characters (`a-z`, `0-9`, `-`). Keep it stable when renaming a plugin.
- `name`: display name, 1–80 characters.
- `summary`: select-menu description, 1–100 characters.
- `description`: Markdown details, 1–1800 characters.
- `compatibility`: supported platform/version guidance, 1–200 characters.
- `links`: 0–5 objects with `label` (1–80 characters) and absolute `https` `url` (1–512 characters, no URL credentials). Discord channel URLs can be used for support.

Copy an entry from `src/main/resources/plugin-directory.example.json` for another plugin. Array order controls directory order; 1–250 entries are supported. Unknown fields are ignored for future extensions. Text and menus are validated against conservative Discord limits, and reload builds complete JDA payloads before accepting changes. Allowed mentions are disabled for all directory messages.

A bad reload leaves the previous immutable snapshot active and preserves the file for repair. Fix the file and retry. Invalid files at startup fail startup rather than silently replacing your metadata. Reload immediately affects browse buttons and future interactions, including published messages; private details already on screen refresh on the next interaction. Use `/directory update` after reload to refresh the public landing count. A removed plugin produces a helpful selection prompt; old page numbers are clamped to valid pages.

## Persistence and recovery

Interactions route through the versioned `tw:dir:v1:` custom IDs. No in-memory editing session or stored message registration is needed. Existing landing buttons work after a restart because the handler reloads the same catalog and understands the same IDs. Keep the custom-ID version compatible when changing this feature. Discord's own ephemeral-message lifetime still applies; use the public button or `/directory browse` to open a fresh browser when a private message expires.

Back up the catalog and record published channel/message IDs if you want to update them later. If a message is deleted, publish a replacement. The bot never automatically publishes or edits Discord messages at startup or on reload.

## Validation

Run `mvn verify`. Tests exercise catalog loading/validation/immutability, pagination, deleted entries, serialized payload limits, restart routing without a session, event isolation, and administrator enforcement. CI runs this on Java 21. Tests use mocked Discord events; they do not connect to Discord.

Before deploying in TooterWorks, perform these live checks in a test channel:

1. Publish as an administrator; verify a member can click Browse, select ModularPacks, and open each link.
2. Browse as two users and confirm selections stay private and independent.
3. Restart the bot; click the original public button again.
4. Add enough test entries to cross 25; reload and check page boundaries, then restore the real catalog.
5. Change metadata and reload; verify new selections use it, then update the original landing message by ID.
6. Try publish/update/reload as a non-administrator, and update with an unrelated message ID. Verify denial and no message changes.
7. Try invalid JSON, then restore it; verify failed reload retains the working directory. Test missing channel/history permissions and a deleted message ID for private error feedback.

Live Discord appearance, permission behavior, and restart interactions need this smoke test; a successful build alone does not verify them.
