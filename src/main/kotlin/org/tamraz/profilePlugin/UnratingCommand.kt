package com.tamraz.profileplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class UnratingCommand(private val plugin: ProfilePlugin, private val isLike: Boolean) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Эта команда только для игроков!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.getMessage(if (isLike) "unlike.usage" else "undislike.usage"))
            return true
        }

        val isAdminCommand = args[0].equals("admin", ignoreCase = true) && args.size >= 2
        if (isAdminCommand && !sender.hasPermission("profileplugin.admin")) {
            sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
            return true
        }

        val targetName = if (isAdminCommand) args[1] else args[0]
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

        if (isAdminCommand) {
            plugin.database.resetRatings(target, if (isLike) "LIKE" else "DISLIKE")
            sender.sendMessage(plugin.getMessage(if (isLike) "unlike.admin-success" else "undislike.admin-success", null, targetName))
        } else {
            val senderPlayer = sender as Player
            val success = plugin.database.removeRating(senderPlayer, target, if (isLike) "LIKE" else "DISLIKE")
            if (success) {
                sender.sendMessage(plugin.getMessage(if (isLike) "unlike.success" else "undislike.success", sender, targetName))
            } else {
                sender.sendMessage(plugin.getMessage(if (isLike) "unlike.not-rated" else "undislike.not-rated", sender, targetName))
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) {
            val suggestions = mutableListOf<String>()
            // Добавляем имена игроков
            suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
            // Если у игрока есть права, добавляем "admin"
            if (sender.hasPermission("profileplugin.admin")) {
                suggestions.add("admin")
            }
            return suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}