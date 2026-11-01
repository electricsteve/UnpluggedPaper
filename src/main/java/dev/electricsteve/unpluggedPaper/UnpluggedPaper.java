package dev.electricsteve.unpluggedPaper;

import static dev.electricsteve.unpluggedPaper.commands.UnplugCommand.DEFAULT_FAKE_PLAYER_SETTINGS;

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi;
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import de.exlll.configlib.YamlConfigurations;
import dev.electricsteve.unpluggedPaper.api.PlayerSpawnOptions;
import dev.electricsteve.unpluggedPaper.commands.UnplugCommand;
import dev.electricsteve.unpluggedPaper.config.UnpluggedConfiguration;
import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;
import dev.electricsteve.unpluggedPaper.kotlin.FakePlayerPlusJava;
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
        Bukkit.getScheduler().runTaskLater(this, _ -> Bukkit.getAsyncScheduler().runNow(this, _ -> addBackUnpluggedPlayers()), 1); // Schedule for 1 tick later (and run async immediately), which should only happen once the server has loaded
    }

    public void savePlayersConfiguration() {
        playersConfigurationStore.save(this.unpluggedPlayersConfig, playersFile);
    }

    private void addBackUnpluggedPlayers() {
        long methodStartTime = System.nanoTime();
        List<UnpluggedPlayers.PlayerEntry> unpluggedPlayers = this.unpluggedPlayersConfig.getUnpluggedPlayers();
        for (UnpluggedPlayers.PlayerEntry playerEntry : unpluggedPlayers) {
            long endTime = playerEntry.getStartTime() + playerEntry.getTime();
            long currentTimeMillis = System.currentTimeMillis();
            if (playerEntry.getStatus() == UnpluggedPlayers.UnpluggedStatus.ACTIVE) {
                String name = playerEntry.getName();
                UUID uuid = playerEntry.getUuid();
                if (endTime <= currentTimeMillis) {
                    playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.EXPIRED);
                    this.broadcastPlugBack(Component.text(name), " AFK session expired", playerEntry.getTime());
                    this.savePlayersConfiguration();
                    return;
                }
                long durationLeftMillis = endTime - currentTimeMillis;
                long durationHadMillis = currentTimeMillis - playerEntry.getStartTime();

                OfflinePlayer offlinePlayer = this.getServer().getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline() || offlinePlayer.isBanned()) {
                    debugLog("Not spawning player {} because they were either banned or a player with this UUID is already online.", uuid);
                    playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.TERMINATED);
                    broadcastPlugBack(Component.text(playerEntry.getName()), " AFK session terminated", durationHadMillis);
                    savePlayersConfiguration();
                    continue;
                }
                Location location = offlinePlayer.getLocation();
                if (location == null) return;
                double health = 20.0; // TODO: fix this, currently waiting on response from paper devs (https://discord.com/channels/289587909051416579/925530366192779286/1566422373270421515)
                int foodLevel = 20;

                // Player profile textures
                PlayerProfile playerProfile = offlinePlayer.getPlayerProfile();
                playerProfile.complete();
                Set<ProfileProperty> profileProperties = playerProfile.getProperties();
                ProfileProperty textureProperty = profileProperties.stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);
                PlayerTextures playerTextures = textureProperty != null && textureProperty.getSignature() != null ?
                    new PlayerTextures(textureProperty.getValue(), textureProperty.getSignature()) : null;

                // TODO: merge this with the one in `UnplugCommand`
                FakePlayerManager fpm = this.fppComponent.getFpm();
                FakePlayerPlusJava.getFromRepository(fpm, name).thenAccept(fakePlayer -> {
                    if (fakePlayer != null) {
                        UUID existingUuid = fakePlayer.getUuid();
                        if (!existingUuid.equals(uuid)) {
                            try {
                                FakePlayerPlusJava.deleteFromRepository(fpm, existingUuid).get();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    PlayerSpawnOptions options = PlayerSpawnOptions.builder()
                        .location(location)
                        .uuid(uuid)
                        .playerTextures(playerTextures)
                        .sleepingIgnored(false)
                        .setClientLoaded(true)
                        .health(health)
                        .foodLevel(foodLevel)
                        .fakePlayerSettings(DEFAULT_FAKE_PLAYER_SETTINGS.apply(location.getWorld()))
                        .enableAdvancements(true)
                        .durationMillis(durationLeftMillis)
                        .durationOverCallback(_ -> {
                            this.getPlayerEntry(uuid).ifPresent(entry -> entry.setStatus(UnpluggedPlayers.UnpluggedStatus.EXPIRED));
                            this.broadcastPlugBack(Component.text(name), " AFK session expired", System.currentTimeMillis() - playerEntry.getStartTime());
                            this.savePlayersConfiguration();
                        })
                        .hidePlayer(this.configuration.getUnplugged().unpluggedHidePlayer())
                        .hidePlayerOps(this.configuration.getUnplugged().unpluggedHideFromOps())
                        .build();
                    UnpluggedPaperApiImpl.spawnPlayerWithUuidAndSkin(name, this, options);
                });
            }
        }
        long methodDuration = System.nanoTime() - methodStartTime;
        debugLog("Adding back unplugged players took {}ns ({}μs or {}ms)", methodDuration, methodDuration / 10e3, methodDuration / 10e6);
    }

    public Optional<UnpluggedPlayers.PlayerEntry> getPlayerEntry(UUID uuid) {
        return this.unpluggedPlayersConfig.getUnpluggedPlayers().stream().filter(playerEntry -> playerEntry.getUuid().equals(uuid)).findFirst();
    }

    public void updatePlayerEntry(UUID uuid, String name, UnpluggedPlayers.UnpluggedStatus status, long time, long startTime, String reason) {
        Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = getPlayerEntry(uuid);
        if (optionalPlayerEntry.isPresent()) {
            UnpluggedPlayers.PlayerEntry playerEntry = optionalPlayerEntry.get();
            playerEntry.setName(name);
            playerEntry.setStatus(status);
            playerEntry.setTime(time);
            playerEntry.setStartTime(startTime);
            playerEntry.setReason(reason);
        } else {
            UnpluggedPlayers.PlayerEntry playerEntry = new UnpluggedPlayers.PlayerEntry(uuid, name, status, time, startTime, reason);
            this.unpluggedPlayersConfig.getUnpluggedPlayers().add(playerEntry);
        }
        savePlayersConfiguration();
    }

    private boolean isFakePlayer(UUID uuid) {
        return this.fppComponent.getFpm().isFake(uuid, false);
    }

    @EventHandler
    private void onPlayerKick(PlayerKickEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (event.getCause().equals(PlayerKickEvent.Cause.DUPLICATE_LOGIN) && isFakePlayer(uuid)) {
            // A fake player is kicked because a real player with the same UUID logged in
            Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = getPlayerEntry(uuid);
            if (optionalPlayerEntry.isPresent()) {
                UnpluggedPlayers.PlayerEntry playerEntry = optionalPlayerEntry.get();
                if (!playerEntry.getStatus().equals(UnpluggedPlayers.UnpluggedStatus.ACTIVE)) return;
                playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.INTERRUPTED);
                broadcastPlugBack(event.getPlayer().displayName(), " has returned", System.currentTimeMillis() - playerEntry.getStartTime());
                savePlayersConfiguration();
            }
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (isFakePlayer(uuid)) {
            Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = getPlayerEntry(uuid);
            if (optionalPlayerEntry.isPresent()) {
                UnpluggedPlayers.PlayerEntry playerEntry = optionalPlayerEntry.get();
                if (!playerEntry.getStatus().equals(UnpluggedPlayers.UnpluggedStatus.ACTIVE)) return;
                if (this.getConfiguration().getUnplugged().resetHealthUponDeath()) {
                    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
                    player.setHealth(attribute != null ? attribute.getValue() : 20);
                    event.setCancelled(true);
                }
                playerEntry.setStatus(UnpluggedPlayers.UnpluggedStatus.TERMINATED);
                broadcastPlugBack(player.displayName(), " AFK session terminated", System.currentTimeMillis() - playerEntry.getStartTime());
                savePlayersConfiguration();
            }
        }
    }

    @EventHandler
    private void onDamage(EntityDamageEvent event) {
        if (this.getConfiguration().getUnplugged().unpluggedDisableDamage()
            && event.getEntity() instanceof Player player && isFakePlayer(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        // TODO: this shouldn't be here, move this to something proper when doing the api
        Player player = event.getPlayer();
        if (!isFakePlayer(player.getUniqueId()) && this.getConfiguration().getUnplugged().unpluggedHidePlayer() && (this.getConfiguration().getUnplugged().unpluggedHideFromOps() || !player.isOp())) this.getServer().getOnlinePlayers().stream()
            .filter(otherPlayer -> isFakePlayer(otherPlayer.getUniqueId()))
            .forEach(fakePlayer -> player.hidePlayer(this, fakePlayer));
        Optional<UnpluggedPlayers.PlayerEntry> optionalPlayerEntry = getPlayerEntry(player.getUniqueId());
        if (optionalPlayerEntry.isPresent() && !isFakePlayer(player.getUniqueId())) {
            UnpluggedPlayers.PlayerEntry playerEntry = optionalPlayerEntry.get();
            UnpluggedPlayers.UnpluggedStatus status = playerEntry.getStatus();
            if (status.equals(UnpluggedPlayers.UnpluggedStatus.INACTIVE)) return;
            if (status.equals(UnpluggedPlayers.UnpluggedStatus.ACTIVE)) {
                getComponentLogger().error("Illegal state: on real player join, it's Unplugged entry was ACTIVE");
                return;
            }
            playerEntry.reset();
        }
    }

    @Override
    public void onDisable() {
        // Just to be sure
        savePlayersConfiguration();
    }

    public void broadcastPlugBack(Component playerComponent, String reasonString, long durationMillis) {
        Component c = playerComponent.color(NamedTextColor.YELLOW).append(Component.text(reasonString, NamedTextColor.YELLOW));
        @SuppressWarnings("SpellCheckingInspection") // Don't spell check format string
        String durationString = DurationFormatUtils.formatDuration(durationMillis, "[d'd'H'h'm'm's's']", true);
        if (configuration.getMessages().displayDuration()) c =
            c.append(Component.text(" (Gone for: ", NamedTextColor.GRAY))
                .append(Component.text(durationString, NamedTextColor.GREEN)).append(Component.text(")", NamedTextColor.GRAY));
        broadcast(c);
    }

    public void broadcast(Component component) {
        if (!configuration.getMessages().broadcastMessages() || configuration.getUnplugged().unpluggedHidePlayer()) return;
        this.getServer().broadcast(component);
    }
}
