package org.tamraz.goprofile

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

class ProfilePlaceholderExpansion(private val plugin: GoProfile) : PlaceholderExpansion() {

    private val statusConfig: YamlConfiguration by lazy {
        val statusFile = File(plugin.dataFolder, "status.yml")
        if (!statusFile.exists()) {
            plugin.saveResource("status.yml", false)
        }
        YamlConfiguration.loadConfiguration(statusFile)
    }

    private val legacySerializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .build()

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
                val rawStatus = plugin.database.getStatus(player)
                if (rawStatus == null) {
                    legacySerializer.serialize(plugin.getMessage("placeholder.status.none", player))
                } else {
                    val statusDisplay = statusConfig.getString("statuses.$rawStatus.display")
                    if (statusDisplay != null) {
                        legacySerializer.serialize(plugin.parseMiniMessage(statusDisplay))
                    } else {
                        legacySerializer.serialize(plugin.parseMiniMessage(rawStatus))
                    }
                }
            }
            "id" -> {
                val playerId = plugin.database.getPlayerId(player)
                if (playerId == null) {
                    legacySerializer.serialize(plugin.getMessage("placeholder.id.none", player))
                } else {
                    playerId.toString()
                }
            }
            "gender" -> {
                val gender = plugin.database.getGender(player)
                if (gender == null) {
                    legacySerializer.serialize(plugin.getMessage("placeholder.gender.none", player))
                } else {
                    val genderDisplayKey = "placeholder.gender.$gender"
                    legacySerializer.serialize(plugin.getMessage(genderDisplayKey, player))
                }
            }
            else -> null
        }
    }
}