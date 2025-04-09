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

        // Регистрируем команды и TabCompleter
        val profilePluginCommand = ProfilePluginCommand(this)
        getCommand("profile")?.setExecutor(ProfileCommand(this))
        getCommand("profileplugin")?.setExecutor(profilePluginCommand)
        getCommand("profileplugin")?.tabCompleter = profilePluginCommand
        getCommand("setprofiletitle")?.setExecutor(SetProfileTitleCommand(this))
        val likeCommand = RatingCommand(this, true)
        getCommand("like")?.setExecutor(likeCommand)
        val dislikeCommand = RatingCommand(this, false)
        getCommand("dislike")?.setExecutor(dislikeCommand)
        val unlikeCommand = UnratingCommand(this, true)
        getCommand("unlike")?.setExecutor(unlikeCommand)
        getCommand("unlike")?.tabCompleter = unlikeCommand
        val undislikeCommand = UnratingCommand(this, false)
        getCommand("undislike")?.setExecutor(undislikeCommand)
        getCommand("undislike")?.tabCompleter = undislikeCommand

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
        val language = config.getString("language", "en_us") // По умолчанию en_us
        val messagesFileName = if (language == "ru_ru") "messages_ru.yml" else "messages_en.yml"
        val messagesFile = File(dataFolder, messagesFileName)
        if (!messagesFile.exists()) {
            saveResource(messagesFileName, false)
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun reloadMessages() {
        val language = config.getString("language", "en_us")
        val messagesFileName = if (language == "ru_ru") "messages_ru.yml" else "messages_en.yml"
        val messagesFile = File(dataFolder, messagesFileName)
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun getMessage(key: String, player: org.bukkit.OfflinePlayer? = null, vararg args: Any): String {
        val message = messages.getString(key, "Message not found: $key") ?: "Message not found: $key"
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