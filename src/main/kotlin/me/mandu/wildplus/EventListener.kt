package me.mandu.wildplus

import io.papermc.paper.event.player.PlayerDeepSleepEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*

class EventListener : Listener {
    //////////
    // Death location logger
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val playerLoc = event.player.location
        val msg = WildPlus.miniMessage.deserialize(
            "<aqua>당신의 사망 지점(WXYZ) : <light_purple><death_world/> <death_x/> <death_y/> <death_z/>",
            Placeholder.unparsed("death_world", playerLoc.world.name),
            Placeholder.unparsed("death_x", "%.1f".format(playerLoc.x)),
            Placeholder.unparsed("death_y", "%.1f".format(playerLoc.y)),
            Placeholder.unparsed("death_z", "%.1f".format(playerLoc.z)),
        )
        event.player.sendMessage(msg)
    }

    //////////
    // Time-skipping bed
    class TimeSkipper() : BukkitRunnable() {
        companion object {
            var current: TimeSkipper? = null
            var task: BukkitTask? = null
            val playersInBed = mutableMapOf<UUID, UUID>()
            fun checkAuto() {
                checkAndDisable()
                checkAndEnable()
            }
            fun checkAndEnable() {
                if (playersInBed.isNotEmpty()) {
                    if (current == null) {
                        val inst = TimeSkipper()
                        current = inst
                        task = inst.runTaskTimer(WildPlus.instance!!, 1L, 1L)
                    }
                }
            }
            fun checkAndDisable() {
                if (playersInBed.isEmpty()) {
                    task?.cancel()
                    current = null
                    task = null
                }
            }
        }
        override fun run() {
            val skippedWid = mutableSetOf<UUID>()
            for (entry in playersInBed) {
                val world = WildPlus.server?.getWorld(entry.value)
                world?.let {
                    if (!skippedWid.contains(entry.value)) {
                        it.time = it.time + 100L
                        skippedWid.add(entry.value)
                    }
                    if (it.time % 2000L < 100L) {
                        WildPlus.server?.getPlayer(entry.key)?.sendMessage(
                            Component.text("현재 시각: ").color(NamedTextColor.AQUA)
                                .append(Component.text("%02d 시".format((it.time / 1000L + 6) % 24)).color(NamedTextColor.LIGHT_PURPLE))
                        )
                    }
                }
            }
        }
    }
    fun populateSleeping(players: Array<Player>) {
        TimeSkipper.playersInBed.clear()
        for (player in players) {
            if (player.isSleeping) {
                TimeSkipper.playersInBed.put(player.uniqueId, player.world.uid)
            }
        }
        WildPlus.server?.consoleSender?.sendMessage(
            Component.text("Current # of Players sleeping: ")
                .append(Component.text(TimeSkipper.playersInBed.size.toString()))
        )
        TimeSkipper.checkAuto()
    }
    @EventHandler
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        val enterResult = event.bedEnterResult
        if (enterResult == PlayerBedEnterEvent.BedEnterResult.OK) {
            TimeSkipper.playersInBed.put(event.player.uniqueId, event.player.world.uid)
        }
//        } else if (enterResult == PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW) {
//            event.player.sleep(event.bed.location, true)
//            TimeSkipper.playersInBed.put(event.player.uniqueId, event.player.world.uid)
//        }
        TimeSkipper.checkAuto()
    }
    @EventHandler
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        TimeSkipper.playersInBed.remove(event.player.uniqueId)
        TimeSkipper.checkAuto()
    }
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (TimeSkipper.playersInBed.contains(event.player.uniqueId)) {
            TimeSkipper.playersInBed.remove(event.player.uniqueId)
            TimeSkipper.checkAuto()
        }
    }
    @EventHandler
    fun onPlayerDeepSleep(event: PlayerDeepSleepEvent) {
        if (TimeSkipper.playersInBed.contains(event.player.uniqueId)) {
            event.isCancelled = true
        }
    }
}