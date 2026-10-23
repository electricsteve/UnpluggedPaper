package dev.electricsteve.unpluggedPaper.api;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings;
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerSpawnOptions {
    /// The location to spawn the player at.
    /// If not set, defaults to the world's spawn location.
    @Nullable
    private Location location;
    /// The UUID to spawn the player with.
    /// If not set, defaults to a UUID generated according to `UUID.nameUUIDFromBytes("${plugin.name}:$name".toByteArray())`,
    /// with the plugin name being the name of the FakePlayerPlus plugin.
    @Nullable
    private UUID uuid;
    /// The skin to spawn the player with.
    /// If not set, defaults to a random default skin.
    @Nullable
    private PlayerTextures playerTextures;
    /// The health to spawn the player at.
    /// If not set, defaults to full health (20.0)
    @Builder.Default
    private double health = -1.0;
    /// The food level to spawn the player at.
    /// If not set, defaults to full food level (20)
    @Builder.Default
    private int foodLevel = -1;
    /// Whether the player should be ignored when checking for sleeping players.
    /// If not set, defaults to true.
    @Builder.Default
    private boolean sleepingIgnored = true;
    /// Whether the client should be marked as loaded immediately after joining.
    /// This removes the delay before the player can be hit.
    @Builder.Default
    private boolean setClientLoaded = false;
    /// The fake player settings to use when spawning the player.
    /// If not set, defaults to the default settings.
    @Nullable
    private FakePlayerSettings fakePlayerSettings;
    /// Whether advancements should be enabled for the player.
    /// If not set, defaults to false.
    @Builder.Default
    private boolean enableAdvancements = false;
}
