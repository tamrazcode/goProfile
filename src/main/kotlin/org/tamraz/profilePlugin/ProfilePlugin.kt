package com.tamraz.profileplugin

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Bukkit
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import org.bukkit.ChatColor
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

class ProfilePlugin : JavaPlugin() {

    lateinit var database: Database
        private set

    lateinit var messages: YamlConfiguration
        private set

    private val activeProfiles = mutableMapOf<org.bukkit.entity.Player, ProfileGUI>()

    override fun onEnable() {
        saveDefaultConfig()
        saveDefaultMessages()
        database = Database(this)
        getCommand("profile")?.setExecutor(ProfileCommand(this))
        getCommand("profileplugin")?.setExecutor(ProfilePluginCommand(this))
        getCommand("setprofiletitle")?.setExecutor(SetProfileTitleCommand(this))
        getCommand("like")?.setExecutor(RatingCommand(this, true))
        getCommand("dislike")?.setExecutor(RatingCommand(this, false))
        getCommand("unlike")?.setExecutor(UnratingCommand(this, true))
        getCommand("undislike")?.setExecutor(UnratingCommand(this, false))
        Bukkit.getPluginManager().registerEvents(InventoryClickListener(this), this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ProfilePlaceholderExpansion(this).register()
        }

        startGuiUpdateTask()

        logger.info("ProfilePlugin успешно запущен!")
    }

    override fun onDisable() {
        database.close()
        activeProfiles.clear()
        logger.info("ProfilePlugin отключен!")
    }

    private fun startGuiUpdateTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (activeProfiles.isEmpty()) return

                // Асинхронно обновляем данные
                object : BukkitRunnable() {
                    override fun run() {
                        val updates = mutableMapOf<org.bukkit.entity.Player, Map<Int, ItemStack>>()
                        for ((player, gui) in activeProfiles.entries.toList()) {
                            if (!player.isOnline || player.openInventory.topInventory.holder !is ProfileInventoryHolder) {
                                activeProfiles.remove(player)
                                continue
                            }
                            val updatedItems = gui.getUpdatedItems()
                            updates[player] = updatedItems
                        }

                        // Синхронно обновляем инвентарь
                        object : BukkitRunnable() {
                            override fun run() {
                                for ((player, items) in updates) {
                                    val inventory = player.openInventory.topInventory
                                    for ((slot, item) in items) {
                                        inventory.setItem(slot, item)
                                    }
                                }
                            }
                        }.runTask(this@ProfilePlugin)
                    }
                }.runTaskAsynchronously(this@ProfilePlugin)
            }
        }.runTaskTimer(this, 0L, 100L) // Каждые 5 секунд (100 тиков)
    }

    fun addActiveProfile(player: org.bukkit.entity.Player, gui: ProfileGUI) {
        activeProfiles[player] = gui
    }

    fun removeActiveProfile(player: org.bukkit.entity.Player) {
        activeProfiles.remove(player)
    }

    fun setPlaceholders(player: org.bukkit.OfflinePlayer, text: String): String {
        return PlaceholderAPI.setPlaceholders(player, text)
    }

    private fun saveDefaultMessages() {
        val messagesFile = File(dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false)
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun reloadMessages() {
        val messagesFile = File(dataFolder, "messages.yml")
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun getMessage(key: String, player: org.bukkit.OfflinePlayer? = null, vararg args: Any): String {
        val message = messages.getString(key, "Сообщение не найдено: $key") ?: "Сообщение не найдено: $key"
        val formatted = String.format(message, *args)
        val withPlaceholders = if (player != null) setPlaceholders(player, formatted) else formatted
        return translateColors(withPlaceholders)
    }

    fun translateColors(text: String): String {
        var result = text
        val hexPattern = Regex("&#([A-Fa-f0-9]{6})")

        result = hexPattern.replace(result) { match ->
            val hex = match.groupValues[1]
            val chars = hex.toCharArray()
            "§x§${chars[0]}§${chars[1]}§${chars[2]}§${chars[3]}§${chars[4]}§${chars[5]}"
        }

        result = ChatColor.translateAlternateColorCodes('&', result)
        return result
    }
}