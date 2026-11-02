package dev.electricsteve.unpluggedPaper.config;

import java.util.List;
import java.util.UUID;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
public class UnpluggedPlayers {
    private List<PlayerEntry> unpluggedPlayers = List.of();

    @Getter
    @Setter
    @AllArgsConstructor
    @Configuration
    public static class PlayerEntry {
        private UUID uuid;
        private String name;
        private UnpluggedStatus status;
        private long time;
        private long startTime;
        private String reason;

        // Have to have a no-args constructor for configlib to not error
        /// DO NOT CALL THIS
        public PlayerEntry() {
            this.uuid = UUID.randomUUID();
        }

        public void reset() {
            status = UnpluggedStatus.INACTIVE;
            time = -1L;
            startTime = -1L;
            reason = "";
        }
    }

    public enum UnpluggedStatus {
        /// When a session is active
        ACTIVE,
        /// When no session is active (appears after a server stop with one of the other non-active versions)
        INACTIVE,
        /// When the session has reached its end time
        EXPIRED,
        /// When stopped by admin
        INTERRUPTED,
        /// When the real player joined
        RETURNED,
        /// When the player couldn't be spawned from config (banned or duplicate UUID) or it was killed
        TERMINATED,
    }
}
