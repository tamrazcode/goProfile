package com.tamraz.profileplugin

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ProfilePlaceholderExpansion(private val plugin: ProfilePlugin) : PlaceholderExpansion() {

    private val statusConfig: YamlConfiguration by lazy {
        val statusFile = File(plugin.dataFolder, "status.yml")
        if (!statusFile.exists()) {
            plugin.saveResource("status.yml", false)
        }
        YamlConfiguration.loadConfiguration(statusFile)
    }

    override fun getIdentifier(): String {
        return "profile"
    }

    override fun getAuthor(): String {
        return "tamraz"
    }

    override fun getVersion(): String {
        return plugin.description.version
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        return when (params.lowercase()) {
            "like" -> plugin.database.getLikes(player).toString()
            "dislike" -> plugin.database.getDislikes(player).toString()
            "status" -> {
                val rawStatus = plugin.database.getStatus(player) ?: return "None"
                // Проверяем, является ли статус идентификатором готового статуса
                val statusDisplay = statusConfig.getString("statuses.$rawStatus.display")
                if (statusDisplay != null) {
                    // Если это готовый статус, возвращаем его отображаемый текст
                    plugin.translateColors(statusDisplay)
                } else {
                    // Если это кастомный статус, возвращаем его как есть
                    plugin.translateColors(rawStatus)
                }
            }
            else -> null
        }
    }
}