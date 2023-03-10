package me.mandu.wildplus

import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

class WildPlus() : JavaPlugin() {
    companion object {
        val miniMessage: MiniMessage = MiniMessage.miniMessage()
        var server: Server? = null
        var logger: Logger? = null
        var instance: WildPlus? = null
    }

    private var eventListener: EventListener? = null

    override fun onEnable() {
//        miniMessage = MiniMessage.builder()
//            .tags(
//                TagResolver.builder()
//                .resolver(StandardTags.defaults())
//                .build()
//            )
//            .strict(false)
//            .build()
        WildPlus.instance = this
        WildPlus.server = server
        WildPlus.logger = getLogger()

        eventListener = EventListener()
        eventListener?.let {
            it.populateSleeping(server.onlinePlayers.toTypedArray())
            server.pluginManager.registerEvents(it, this)
        }
    }
    override fun onDisable() {
        eventListener?.let {
            it.populateSleeping(arrayOf<Player>())
            HandlerList.unregisterAll(it)
        }
        eventListener = null
        WildPlus.logger = null
        WildPlus.server = null
        WildPlus.instance = null
    }
}