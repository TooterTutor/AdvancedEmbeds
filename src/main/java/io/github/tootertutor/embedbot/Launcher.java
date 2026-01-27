package io.github.tootertutor.embedbot;

import io.github.tootertutor.embedbot.gui.BotGUI;

/**
 * Main launcher that provides both CLI and GUI modes.
 * <p>
 * Usage:
 * <ul>
 * <li>{@code java -jar embedbot.jar} - Launches GUI mode</li>
 * <li>{@code java -jar embedbot.jar --no-gui} - Launches CLI mode</li>
 * </ul>
 * </p>
 */
public final class Launcher {

    private Launcher() {
        // entrypoint only
    }

    public static void main(String[] args) throws Exception {
        boolean useGui = true;

        // Check for --no-gui flag
        for (String arg : args) {
            if ("--no-gui".equals(arg) || "--headless".equals(arg)) {
                useGui = false;
                break;
            }
        }

        if (useGui) {
            // Launch JavaFX GUI
            BotGUI.main(args);
        } else {
            // Launch CLI mode (traditional bot startup)
            Bot.main(args);
        }
    }
}
