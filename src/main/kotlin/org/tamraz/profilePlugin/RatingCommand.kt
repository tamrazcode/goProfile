package com.tamraz.profileplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RatingCommand(private val plugin: ProfilePlugin, private val isLike: Boolean) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Эта команда только для игроков!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.getMessage(if (isLike) "like.usage" else "dislike.usage"))
            return true
        }

        val targetName = args[0]
        val onlinePlayer = Bukkit.getPlayerExact(targetName)
        val target = if (onlinePlayer != null) {
            onlinePlayer
        } else {
            val offlinePlayer = Bukkit.getOfflinePlayerIfCached(targetName)
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                offlinePlayer
            } else {
                sender.sendMessage(plugin.getMessage("profile.player-not-found", sender, targetName))
                return true
            }
        }

        // Проверяем, не пытается ли игрок поставить оценку самому себе
        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage(plugin.getMessage(if (isLike) "like.self" else "dislike.self"))
            return true
        }

        // Пробуем поставить лайк или дизлайк
        val success = plugin.database.setRating(sender, target, if (isLike) "LIKE" else "DISLIKE")
        if (success) {
            sender.sendMessage(plugin.getMessage(if (isLike) "like.success" else "dislike.success", sender, targetName))
        } else {
            sender.sendMessage(plugin.getMessage(if (isLike) "like.already-rated" else "dislike.already-rated", sender, targetName))
        }

        return true
    }
}