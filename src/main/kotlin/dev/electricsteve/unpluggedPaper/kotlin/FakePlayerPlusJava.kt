package dev.electricsteve.unpluggedPaper.kotlin

import com.coderxi.plugin.fakeplayer.api.FakePlayerPlusPluginApi.Companion.api
import com.coderxi.plugin.fakeplayer.api.entity.FakePlayer
import com.coderxi.plugin.fakeplayer.api.manager.FakePlayerManager
import com.coderxi.plugin.fakeplayer.api.model.PlayerTextures
import kotlinx.coroutines.Dispatchers
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture

object FakePlayerPlusJava {
    @JvmStatic
    fun spawn(
        manager: FakePlayerManager,
        name: String,
        sender: CommandSender,
        location: Location?
    ): CompletableFuture<FakePlayer?> =
        FoliaCoroutines.run(Dispatchers.Default) {
            manager.spawn(name, sender, location)
        }

    @JvmStatic
    fun getPlayerTexturesByName(
        name: String?,
        cache: Boolean
    ): CompletableFuture<PlayerTextures?> =
        FoliaCoroutines.run(Dispatchers.Default) {
            api.getPlayerTexturesByName(name, cache)
        }

    @JvmStatic
    fun saveSkin(
        fakePlayerManager: FakePlayerManager,
        fakePlayer: FakePlayer
    ): CompletableFuture<Unit> =
        FoliaCoroutines.run(Dispatchers.Default) {
            fakePlayerManager.saveSkin(fakePlayer)
        }

    @JvmStatic
    fun setTextures(
        plugin: Plugin,
        fakePlayer: FakePlayer,
        textures: PlayerTextures
    ): CompletableFuture<Unit> = FoliaCoroutines.run(FoliaCoroutines.on(plugin, fakePlayer.player)) {
        fakePlayer.nms.setTextures(textures.value, textures.signature)
    }
}