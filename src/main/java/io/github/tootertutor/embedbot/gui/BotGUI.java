package io.github.tootertutor.embedbot.gui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.tootertutor.embedbot.Bot;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Modern JavaFX-based GUI for configuring and controlling the EmbedBot.
 * <p>
 * Features a clean, Material Design-inspired interface with token input,
 * start/stop controls, and real-time log viewing.
 * </p>
 */
public class BotGUI extends Application {
    private static final Logger log = LoggerFactory.getLogger(BotGUI.class);
    private static final Path BASE_DIR = resolveBaseDir();
    private static final Path ENV_PATH = BASE_DIR.resolve(".env");

    private PasswordField tokenField;
    private Button startButton;
    private Button stopButton;
    private TextArea logArea;
    private Label statusLabel;

    private volatile boolean botRunning = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EmbedBot Configuration");

        // Main container
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #1e1e2e;");

        // Header
        Label titleLabel = new Label("Discord EmbedBot");
        titleLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        Label subtitleLabel = new Label("Components V2 • Modern Java 17+");
        subtitleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #a6adc8;");

        VBox headerBox = new VBox(5, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // Token input section
        VBox tokenSection = createTokenSection();

        // Control buttons section
        HBox buttonSection = createButtonSection();

        // Status section
        HBox statusSection = createStatusSection();

        // Log viewer section
        VBox logSection = createLogSection();

        // Assemble layout
        root.getChildren().addAll(
                headerBox,
                new Separator(),
                tokenSection,
                buttonSection,
                statusSection,
                new Separator(),
                logSection);

        Scene scene = new Scene(root, 800, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (botRunning) {
                try {
                    Bot.stop();
                } catch (Exception ex) {
                    log.error("Error stopping bot on exit", ex);
                }
            }
            Platform.exit();
            System.exit(0);
        });

        // Redirect logging to text area
        redirectLoggingToTextArea();

        primaryStage.show();
        log.info("EmbedBot GUI initialized");
    }

    private VBox createTokenSection() {
        Label tokenLabel = new Label("Discord Bot Token");
        tokenLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        tokenField = new PasswordField();
        tokenField.setPromptText("Enter your Discord bot token...");
        tokenField.setPrefHeight(40);
        tokenField.setStyle(
                "-fx-background-color: #313244; " +
                        "-fx-text-fill: #cdd6f4; " +
                        "-fx-prompt-text-fill: #6c7086; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-font-size: 13px; " +
                        "-fx-padding: 0 15px;");

        // Try to load token from environment or .env file next to the jar
        String envToken = System.getenv("DISCORD_TOKEN");
        if (envToken == null || envToken.isEmpty()) {
            Dotenv dotenv = Dotenv.configure()
                    .directory(BASE_DIR.toString())
                    .ignoreIfMissing()
                    .load();
            envToken = dotenv.get("DISCORD_TOKEN");
        }
        if (envToken != null && !envToken.isEmpty()) {
            tokenField.setText(envToken);
        }

        VBox section = new VBox(10, tokenLabel, tokenField);
        return section;
    }

    private HBox createButtonSection() {
        startButton = new Button("Start Bot");
        startButton.setPrefWidth(150);
        startButton.setPrefHeight(45);
        startButton.setStyle(
                "-fx-background-color: #a6e3a1; " +
                        "-fx-text-fill: #1e1e2e; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand;");
        startButton.setOnAction(e -> handleStartBot());

        stopButton = new Button("Stop Bot");
        stopButton.setPrefWidth(150);
        stopButton.setPrefHeight(45);
        stopButton.setStyle(
                "-fx-background-color: #f38ba8; " +
                        "-fx-text-fill: #1e1e2e; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-cursor: hand; " +
                        "-fx-opacity: 0.5;");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> handleStopBot());

        HBox buttonBox = new HBox(15, startButton, stopButton);
        buttonBox.setAlignment(Pos.CENTER);
        return buttonBox;
    }

    private HBox createStatusSection() {
        Label statusLabelText = new Label("Status:");
        statusLabelText.setStyle("-fx-font-size: 13px; -fx-text-fill: #a6adc8;");

        statusLabel = new Label("Not Running");
        statusLabel.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-text-fill: #f38ba8; " +
                        "-fx-font-weight: bold;");

        HBox statusBox = new HBox(10, statusLabelText, statusLabel);
        statusBox.setAlignment(Pos.CENTER);
        return statusBox;
    }

    private VBox createLogSection() {
        Label logLabel = new Label("Console Output");
        logLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(15);
        logArea.setStyle(
                "-fx-control-inner-background: #181825; " +
                        "-fx-text-fill: #cdd6f4; " +
                        "-fx-font-family: 'Monospaced'; " +
                        "-fx-font-size: 12px; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-color: #313244; " +
                        "-fx-border-width: 1px;");

        VBox logBox = new VBox(10, logLabel, logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return logBox;
    }

    private void handleStartBot() {
        String token = tokenField.getText().trim();

        if (token.isEmpty()) {
            showError("Token Required", "Please enter a Discord bot token.");
            return;
        }

        // Validate token format (basic check)
        if (token.length() < 50) {
            showError("Invalid Token", "The token appears to be invalid (too short).");
            return;
        }

        startButton.setDisable(true);
        statusLabel.setText("Starting...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #f9e2af; -fx-font-weight: bold;");

        // Start bot in background thread
        new Thread(() -> {
            try {
                // Persist token to .env next to the jar and set system property for this run
                persistToken(token);
                System.setProperty("DISCORD_TOKEN", token);

                Bot.start();

                Platform.runLater(() -> {
                    botRunning = true;
                    startButton.setDisable(true);
                    stopButton.setDisable(false);
                    stopButton.setStyle(
                            "-fx-background-color: #f38ba8; " +
                                    "-fx-text-fill: #1e1e2e; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 8px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-opacity: 1.0;");
                    tokenField.setDisable(true);
                    statusLabel.setText("Running");
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
                    log.info("Bot started successfully");
                });
            } catch (Exception ex) {
                log.error("Failed to start bot", ex);
                Platform.runLater(() -> {
                    showError("Startup Failed", "Failed to start bot: " + ex.getMessage());
                    startButton.setDisable(false);
                    statusLabel.setText("Error");
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #f38ba8; -fx-font-weight: bold;");
                });
            }
        }).start();
    }

    private void handleStopBot() {
        stopButton.setDisable(true);
        statusLabel.setText("Stopping...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #f9e2af; -fx-font-weight: bold;");

        new Thread(() -> {
            try {
                Bot.stop();

                Platform.runLater(() -> {
                    botRunning = false;
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                    stopButton.setStyle(
                            "-fx-background-color: #f38ba8; " +
                                    "-fx-text-fill: #1e1e2e; " +
                                    "-fx-font-size: 14px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-background-radius: 8px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-opacity: 0.5;");
                    tokenField.setDisable(false);
                    statusLabel.setText("Not Running");
                    statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #f38ba8; -fx-font-weight: bold;");
                    log.info("Bot stopped");
                });
            } catch (Exception ex) {
                log.error("Error stopping bot", ex);
                Platform.runLater(() -> {
                    showError("Stop Failed", "Failed to stop bot: " + ex.getMessage());
                    stopButton.setDisable(false);
                });
            }
        }).start();
    }

    private void redirectLoggingToTextArea() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                buffer.append((char) b);
                if (b == '\n') {
                    String text = buffer.toString();
                    Platform.runLater(() -> {
                        logArea.appendText(text);
                        // Auto-scroll to bottom
                        logArea.setScrollTop(Double.MAX_VALUE);
                    });
                    buffer.setLength(0);
                }
            }
        });

        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Resolve base directory of running JAR (or classpath root in IDE) for .env
     * placement.
     */
    private static Path resolveBaseDir() {
        try {
            var uri = BotGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path base = Paths.get(uri).toAbsolutePath();
            Path dir = Files.isRegularFile(base) ? base.getParent() : base;
            return dir == null ? Paths.get("").toAbsolutePath() : dir;
        } catch (Exception e) {
            return Paths.get("").toAbsolutePath();
        }
    }

    /**
     * Writes DISCORD_TOKEN to .env beside the jar for future runs.
     */
    private void persistToken(String token) {
        try {
            if (ENV_PATH.getParent() != null) {
                Files.createDirectories(ENV_PATH.getParent());
            }
            String content = "DISCORD_TOKEN=" + token + System.lineSeparator();
            Files.writeString(ENV_PATH, content);
            log.info("Saved DISCORD_TOKEN to {}", ENV_PATH.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to write token to .env at {}", ENV_PATH.toAbsolutePath(), e);
        }
    }
}
