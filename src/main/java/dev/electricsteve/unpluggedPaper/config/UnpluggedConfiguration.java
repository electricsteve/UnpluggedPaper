package dev.electricsteve.unpluggedPaper.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
public class UnpluggedConfiguration {
    private Main main = new Main(
        true,
        false,
        true,
        false
    );

    // TODO: Implement unimplemented options

    public record Main(
        @Comment("Toggles the entire Unplug feature.")
        boolean unpluggedPaperEnabled,
        @Comment("Enables debugging output.")
        boolean debugMode,
        @Comment("[DOES NOTHING ATM] Enables reduced output for various information commands")
        boolean reducedListDebugInfo,
        @Comment("[DOES NOTHING ATM] Enables advanced Admin options, such as 'set'.")
        boolean advancedAdminOptions
    ) {}

    private Unplugged unplugged = new Unplugged(
        129600,
        false,
        false,
        false,
        false
    );

    public record Unplugged(
        @Comment("Set the default timeout (in minutes). The default is for 90 days.")
        long defaultUnpluggedTimeout,
        @Comment("Resets the AFK bots Health when killed. (set health to max, cancel death, leave)")
        boolean resetHealthUponDeath,
        @Comment("Prevents the AFK bot from taking damage.")
        boolean unpluggedDisableDamage,
        @Comment("Makes the bot invisible to others. (still shows on server list players)")
        boolean unpluggedHidePlayer,
        @Comment("Makes the bot invisible to to Operators as well.")
        boolean unpluggedHideFromOps
    ) {}

    private Messages messages = new Messages(
        false,
        false,
        false,
        false
    );

    public record Messages(
        @Comment("Enables the broadcasting of Unplugged status messages.")
        boolean broadcastMessages,
        @Comment("[DOES NOTHING ATM] Enables the disabling of the default `player has joined` messages while bots are spawned, where possible.")
        boolean hideUnpluggedJoin,
        @Comment("Enables the duration display of Unplugged status messages.")
        boolean displayDuration,
        @Comment("[DOES NOTHING ATM] Enables the Feedback display of the reason why an Unplugged session ended")
        boolean displayReturnFeedback
    ) {}
}
