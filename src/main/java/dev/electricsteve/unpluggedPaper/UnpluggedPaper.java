package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import de.exlll.configlib.YamlConfigurations;
import dev.electricsteve.unpluggedPaper.commands.UnplugCommand;
import dev.electricsteve.unpluggedPaper.config.UnpluggedConfiguration;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import lombok.Getter;
import revxrsal.commands.bukkit.BukkitLamp;

@Getter
public final class UnpluggedPaper extends JavaPlugin implements Listener {
    private static final String REQUIRED_FPP_VERSION = "2.2.50";

    public static Logger LOGGER = LogManager.getLogger("UnpluggedPaper");
    private static boolean debug = Boolean.getBoolean("unplugged.debug"); // Set by command-line argument

    private final Path configFile = new File(getDataFolder(), "config.yml").toPath();
    private final Path playersFile = new File(getDataFolder(), "unplugged_players.yml").toPath();
    private YamlConfigurationStore<UnpluggedPlayers> playersConfigurationStore;
    private final YamlConfigurationProperties configurationProperties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
        .header(
            """
            UnpluggedPaper Configuration
            """
        )
        .footer("Author: ElectricSteve")
        .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
        .build();
    private final YamlConfigurationProperties playersConfigProperties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
        .header(
            """
            Unplugged players
            This file contains data for (unplugged) players, so they can be respawned on server restart.
            """
        )
        .footer("Author: ElectricSteve")
        .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
        .build();

    private UnpluggedFppComponent fppComponent;
    private UnpluggedConfiguration configuration;
    private UnpluggedPlayers unpluggedPlayersConfig;

    /// Prints a debug log if debug logging is enabled. Only works after configuration loading (or if in IDE).
    /// @param key message
    /// @param args parameters
    private static void debugLog(String key, Object... args) {
        if (debug) {
            LOGGER.info(String.format("[DEBUG] %s", key), args);
        }
    }

    @Override
    public void onEnable() {
        String fppVersion = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("FakePlayerPlus")).getPluginMeta().getVersion();
        if (!fppVersion.equals(REQUIRED_FPP_VERSION)) {
            getComponentLogger().error("FakePlayerPlus version " + REQUIRED_FPP_VERSION + " is required, but version {} is installed. Disabling plugin.", fppVersion);
            return;
        }

        // Config
        this.configuration = YamlConfigurations.update(
            configFile,
            UnpluggedConfiguration.class,
            configurationProperties
        );
        playersConfigurationStore = new YamlConfigurationStore<>(UnpluggedPlayers.class, playersConfigProperties);
        this.unpluggedPlayersConfig = playersConfigurationStore.update(playersFile);

        if (this.configuration.getMain().debugMode()) debug = true;
        if (!this.configuration.getMain().unpluggedPaperEnabled()) {
            debugLog("unpluggedPaperEnabled set to false, exiting");
            return;
        }

        // Initialize fpp component and commands
        this.fppComponent = new UnpluggedFppComponent();
        FakePlayerPlusPluginApi.Companion.getApi().registerComponent(fppComponent);
        var lamp = BukkitLamp.builder(this).build();
        lamp.register(new UnplugCommand(this));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(this, this);

        // Add back players that were unplugged when the server turned off
        addBackUnpluggedPlayers();
    }

    public void savePlayersConfiguration() {
        playersConfigurationStore.save(this.unpluggedPlayersConfig, playersFile);
    }

    private void addBackUnpluggedPlayers() {
        List<UnpluggedPlayers.PlayerEntry> unpluggedPlayers = this.unpluggedPlayersConfig.getUnpluggedPlayers();
        for (UnpluggedPlayers.PlayerEntry playerEntry : unpluggedPlayers) {
            if (playerEntry.getStatus() == UnpluggedPlayers.UnpluggedStatus.ACTIVE
                && playerEntry.getStartTime() + playerEntry.getTime() > System.currentTimeMillis()) {
                
            }
        }
    }

    private Optional<UnpluggedPlayers.PlayerEntry> getPlayerEntry(UUID uuid) {
        return this.unpluggedPlayersConfig.getUnpluggedPlayers().stream().filter(playerEntry -> playerEntry.getUuid().equals(uuid)).findFirst();
    }

    @EventHandler
    private void onPlayerKick(PlayerKickEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (event.getCause().equals(PlayerKickEvent.Cause.DUPLICATE_LOGIN) && this.fppComponent.getFpm().isFake(uuid, false)) {
            // A fake player is kicked because a real player with the same UUID logged in
            Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = getPlayerEntry(uuid);
            if (optionalPlayerEntry.isPresent()) {
                UnpluggedPlayers.PlayerEntry playerEntry = optionalPlayerEntry.get();
                if (!playerEntry.getStatus().equals(UnpluggedPlayers.UnpluggedStatus.ACTIVE)) return;
                playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.INTERRUPTED);
                savePlayersConfiguration();
            }
        }
    }

    @Override
    public void onDisable() {
        List<UnpluggedPlayers.PlayerEntry> unpluggedPlayers = this.unpluggedPlayersConfig.getUnpluggedPlayers();
        for (UnpluggedPlayers.PlayerEntry playerEntry : unpluggedPlayers) {
            UnpluggedPlayers.UnpluggedStatus status = playerEntry.getStatus();
            if (!status.equals(UnpluggedPlayers.UnpluggedStatus.ACTIVE) && !status.equals(UnpluggedPlayers.UnpluggedStatus.INACTIVE)) {
                playerEntry.reset();
            }
        }
    }

}
