package com.tamraz.profileplugin

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class ProfilePlaceholderExpansion(private val plugin: ProfilePlugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "profile"
    }

    override fun getAuthor(): String {
        return "tamrazcode"
    }

    override fun getVersion(): String {
        return plugin.description.version
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null

        return when (params.lowercase()) {
            "like" -> plugin.database.getLikes(player).toString()
            "dislike" -> plugin.database.getDislikes(player).toString()
            else -> null
        }
    }
}