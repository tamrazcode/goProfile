package org.tamraz.goprofile

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ProfilePlaceholderExpansion(private val plugin: GoProfile) : PlaceholderExpansion() {

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
                val statusDisplay = statusConfig.getString("statuses.$rawStatus.display")
                if (statusDisplay != null) {
                    plugin.parseMiniMessage(statusDisplay).toString()
                } else {
                    plugin.parseMiniMessage(rawStatus).toString()
                }
            }
            "id" -> plugin.database.getPlayerId(player)?.toString() ?: "N/A"
            "gender" -> plugin.database.getGender(player) ?: "Not set"
            else -> null
        }
    }
}