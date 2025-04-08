package com.tamraz.profileplugin

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.NamespacedKey
import org.bukkit.event.player.PlayerQuitEvent

class InventoryClickListener(private val plugin: ProfilePlugin) : Listener {

    // Храним время последнего использования команды: <игрок, <слот, время>>
    private val cooldowns = mutableMapOf<org.bukkit.entity.Player, MutableMap<Int, Long>>()

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is ProfileInventoryHolder) return

        // Отменяем клик по умолчанию для всех типов кликов (включая Shift)
        event.isCancelled = true

        // Проверяем, что клик был по предмету в инвентаре GUI (не в инвентаре игрока)
        if (event.clickedInventory?.holder !is ProfileInventoryHolder) return

        // Проверяем, есть ли у предмета команда
        val item = event.currentItem ?: return
        val meta = item.itemMeta ?: return
        val pdc = meta.persistentDataContainer

        // Получаем игрока, который кликнул
        val player = event.whoClicked as? org.bukkit.entity.Player ?: return

        // Получаем target из ProfileInventoryHolder
        val holder = event.inventory.holder as ProfileInventoryHolder
        val target = holder.getTarget() ?: return

        // Проверяем параметр sound
        val soundName = pdc.get(NamespacedKey(plugin, "profile_sound"), org.bukkit.persistence.PersistentDataType.STRING)
        if (soundName != null) {
            try {
                val sound = Sound.valueOf(soundName)
                player.playSound(player.location, sound, 1.0f, 1.0f)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Неверный звук: $soundName")
            }
        }

        // Проверяем параметр close
        val shouldClose = pdc.get(NamespacedKey(plugin, "profile_close"), org.bukkit.persistence.PersistentDataType.BYTE) == 1.toByte()
        if (shouldClose) {
            player.closeInventory()
        }

        // Проверяем, есть ли команда
        val command = pdc.get(NamespacedKey(plugin, "profile_command"), org.bukkit.persistence.PersistentDataType.STRING) ?: return

        // Проверяем cooldown
        val cooldownSeconds = pdc.get(NamespacedKey(plugin, "profile_cooldown"), org.bukkit.persistence.PersistentDataType.INTEGER) ?: 0
        if (cooldownSeconds > 0) {
            val slot = event.slot
            val playerCooldowns = cooldowns.getOrPut(player) { mutableMapOf() }
            val lastUsed = playerCooldowns.getOrDefault(slot, 0L)
            val currentTime = System.currentTimeMillis() / 1000

            if (currentTime - lastUsed < cooldownSeconds) {
                val remaining = cooldownSeconds - (currentTime - lastUsed)
                player.sendMessage(plugin.getMessage("inventory.cooldown", player, remaining))
                return
            }

            // Обновляем время последнего использования
            playerCooldowns[slot] = currentTime
        }

        // Проверяем формат команды
        when {
            command.startsWith("[console]") -> {
                val actualCommand = command.substring("[console]".length).trim()
                val formattedCommand = actualCommand.replace("{player}", target.name ?: "Unknown")
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand)
            }
            command.startsWith("[player]") -> {
                val actualCommand = command.substring("[player]".length).trim()
                val formattedCommand = actualCommand.replace("{player}", target.name ?: "Unknown")
                Bukkit.dispatchCommand(player, formattedCommand)
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        cooldowns.remove(event.player)
    }
}