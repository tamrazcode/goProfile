package com.tamraz.profileplugin

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Bukkit
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import org.bukkit.ChatColor

class ProfilePlugin : JavaPlugin() {

    lateinit var database: Database
        private set

    lateinit var messages: YamlConfiguration
        private set

    override fun onEnable() {
        saveDefaultConfig()
        saveDefaultMessages()
        database = Database(this)
        getCommand("profile")?.setExecutor(ProfileCommand(this))
        getCommand("profileplugin")?.setExecutor(ProfilePluginCommand(this))
        getCommand("setprofiletitle")?.setExecutor(SetProfileTitleCommand(this))
        Bukkit.getPluginManager().registerEvents(InventoryClickListener(this), this)
        logger.info("ProfilePlugin успешно запущен!")
    }

    override fun onDisable() {
        database.close()
        logger.info("ProfilePlugin отключен!")
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
        var result = ChatColor.translateAlternateColorCodes('&', text)
        val pattern = Regex("&#([A-Fa-f0-9]{6})")
        result = pattern.replace(result) { match ->
            val hex = match.groupValues[1]
            "§x" + hex.chunked(2).joinToString("") { "§${it.lowercase()}" }
        }
        return result
    }
}