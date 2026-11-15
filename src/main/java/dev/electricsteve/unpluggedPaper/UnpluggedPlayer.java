package dev.electricsteve.unpluggedPaper;

import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer;

import dev.electricsteve.unpluggedPaper.config.UnpluggedPlayers;

public record UnpluggedPlayer(UnpluggedPlayers.PlayerEntry playerEntry, FakePlayer fakePlayer) {
}
