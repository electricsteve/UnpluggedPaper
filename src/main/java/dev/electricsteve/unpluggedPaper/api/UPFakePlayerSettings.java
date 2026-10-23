package dev.electricsteve.unpluggedPaper.api;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayerSettings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class UPFakePlayerSettings implements FakePlayerSettings {
    private boolean collidable;
    private boolean pickupItems;
    private boolean invulnerable;
    private boolean infiniteFoodLevel;
    private boolean autoReplenish;
    private boolean autoFish;
    private int simulationDistance;
    private boolean xpNoCooldown;
    private boolean autoEquipTool;
    private boolean keepInventory;
    private InteractedAction interactedAction;
    private InteractedAction shiftInteractedAction;
    private DeathAction deathAction;
    private KeepingMode keepingMode;

    // Blame stupid lombok https://github.com/projectlombok/lombok/issues/2464

    @Override
    public boolean getCollidable() {
        return this.collidable;
    }

    @Override
    public boolean getPickupItems() {
        return this.pickupItems;
    }

    @Override
    public boolean getInvulnerable() {
        return this.invulnerable;
    }

    @Override
    public boolean getInfiniteFoodLevel() {
        return this.infiniteFoodLevel;
    }

    @Override
    public boolean getAutoReplenish() {
        return this.autoReplenish;
    }

    @Override
    public boolean getAutoFish() {
        return this.autoFish;
    }

    @Override
    public boolean getXpNoCooldown() {
        return this.xpNoCooldown;
    }

    @Override
    public boolean getAutoEquipTool() {
        return this.autoEquipTool;
    }

    @Override
    public boolean getKeepInventory() {
        return this.keepInventory;
    }
}
