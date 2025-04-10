package org.tamraz.goprofile

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GoProfileCommand(private val plugin: GoProfile) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.getMessage("plugin.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("profileplugin.admin")) {
                    sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
                    return true
                }
                plugin.reloadConfig()
                plugin.reloadMessages()
                plugin.reloadStatusConfig() // Добавляем перезагрузку status.yml
                sender.sendMessage(plugin.getMessage("reload.success"))
            }

            "profile" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.getMessage("profile.not-player"))
                    return true
                }

                if (args.size == 1) {
                    // Открываем профиль самого игрока
                    val gui = ProfileGUI(plugin, sender)
                    gui.open(sender)
                    return true
                }

                when (args[1].lowercase()) {
                    "status" -> {
                        if (args.size < 3) {
                            sender.sendMessage(plugin.getMessage("profile.status.usage"))
                            return true
                        }

                        when (args[2].lowercase()) {
                            "set" -> {
                                if (args.size < 4) {
                                    sender.sendMessage(plugin.getMessage("profile.status.set-usage"))
                                    return true
                                }
                                val statusText = args.drop(3).joinToString(" ")
                                if (statusText.length > 32) {
                                    sender.sendMessage(plugin.getMessage("profile.status.too-long"))
                                    return true
                                }
                                plugin.database.setStatus(sender, statusText)
                                sender.sendMessage(plugin.getMessage("profile.status.set-success", sender, statusText))
                            }
                            "clear" -> {
                                plugin.database.setStatus(sender, null)
                                sender.sendMessage(plugin.getMessage("profile.status.cleared"))
                            }
                            else -> {
                                // Проверяем, является ли аргумент идентификатором готового статуса
                                val statusId = args[2].lowercase()
                                val statusDisplay = plugin.statusConfig.getString("statuses.$statusId.display")
                                if (statusDisplay == null) {
                                    sender.sendMessage(plugin.getMessage("profile.status.invalid-id", sender, statusId))
                                    return true
                                }
                                plugin.database.setStatus(sender, statusId)
                                sender.sendMessage(plugin.getMessage("profile.status.set-success", sender, plugin.translateColors(statusDisplay)))
                            }
                        }
                    }
                    else -> {
                        // Открываем профиль другого игрока
                        val targetName = args[1]
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

                        val gui = ProfileGUI(plugin, target)
                        gui.open(sender)
                    }
                }
            }

            "setprofiletitle" -> {
                if (!sender.hasPermission("profileplugin.admin")) {
                    sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
                    return true
                }

                if (args.size < 3) {
                    sender.sendMessage(plugin.getMessage("setprofiletitle.usage"))
                    return true
                }

                val targetName = args[1]
                val onlinePlayer = Bukkit.getPlayerExact(targetName)
                val target = if (onlinePlayer != null) {
                    onlinePlayer
                } else {
                    val offlinePlayer = Bukkit.getOfflinePlayerIfCached(targetName)
                    if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                        offlinePlayer
                    } else {
                        sender.sendMessage(plugin.getMessage("setprofiletitle.player-not-found", null, targetName))
                        return true
                    }
                }

                val title = args.drop(2).joinToString(" ")
                plugin.database.setTitle(target, title)
                sender.sendMessage(plugin.getMessage("setprofiletitle.success", null, targetName, title))
            }

            "like" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.getMessage("profile.not-player"))
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage(plugin.getMessage("like.usage"))
                    return true
                }

                val targetName = args[1]
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

                if (sender.uniqueId == target.uniqueId) {
                    sender.sendMessage(plugin.getMessage("like.self"))
                    return true
                }

                val success = plugin.database.setRating(sender, target, "LIKE")
                if (success) {
                    sender.sendMessage(plugin.getMessage("like.success", sender, targetName))
                } else {
                    sender.sendMessage(plugin.getMessage("like.already-rated", sender, targetName))
                }
            }

            "dislike" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.getMessage("profile.not-player"))
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage(plugin.getMessage("dislike.usage"))
                    return true
                }

                val targetName = args[1]
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

                if (sender.uniqueId == target.uniqueId) {
                    sender.sendMessage(plugin.getMessage("dislike.self"))
                    return true
                }

                val success = plugin.database.setRating(sender, target, "DISLIKE")
                if (success) {
                    sender.sendMessage(plugin.getMessage("dislike.success", sender, targetName))
                } else {
                    sender.sendMessage(plugin.getMessage("dislike.already-rated", sender, targetName))
                }
            }

            "unlike" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.getMessage("profile.not-player"))
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage(plugin.getMessage("unlike.usage"))
                    return true
                }

                val isAdminCommand = args[1].equals("admin", ignoreCase = true) && args.size >= 3
                if (isAdminCommand && !sender.hasPermission("profileplugin.admin")) {
                    sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
                    return true
                }

                val targetName = if (isAdminCommand) args[2] else args[1]
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
                    plugin.database.resetRatings(target, "LIKE")
                    sender.sendMessage(plugin.getMessage("unlike.admin-success", null, targetName))
                } else {
                    val senderPlayer = sender as Player
                    val success = plugin.database.removeRating(senderPlayer, target, "LIKE")
                    if (success) {
                        sender.sendMessage(plugin.getMessage("unlike.success", sender, targetName))
                    } else {
                        sender.sendMessage(plugin.getMessage("unlike.not-rated", sender, targetName))
                    }
                }
            }

            "undislike" -> {
                if (sender !is Player) {
                    sender.sendMessage(plugin.getMessage("profile.not-player"))
                    return true
                }

                if (args.size < 2) {
                    sender.sendMessage(plugin.getMessage("undislike.usage"))
                    return true
                }

                val isAdminCommand = args[1].equals("admin", ignoreCase = true) && args.size >= 3
                if (isAdminCommand && !sender.hasPermission("profileplugin.admin")) {
                    sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
                    return true
                }

                val targetName = if (isAdminCommand) args[2] else args[1]
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
                    plugin.database.resetRatings(target, "DISLIKE")
                    sender.sendMessage(plugin.getMessage("undislike.admin-success", null, targetName))
                } else {
                    val senderPlayer = sender as Player
                    val success = plugin.database.removeRating(senderPlayer, target, "DISLIKE")
                    if (success) {
                        sender.sendMessage(plugin.getMessage("undislike.success", sender, targetName))
                    } else {
                        sender.sendMessage(plugin.getMessage("undislike.not-rated", sender, targetName))
                    }
                }
            }

            else -> {
                sender.sendMessage(plugin.getMessage("plugin.usage"))
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
            val suggestions = mutableListOf("profile", "setprofiletitle", "like", "dislike", "unlike", "undislike")
            if (sender.hasPermission("profileplugin.admin")) {
                suggestions.add("reload")
            }
            return suggestions.filter { it.startsWith(args[0], ignoreCase = true) }
        }

        when (args[0].lowercase()) {
            "profile" -> {
                if (args.size == 2) {
                    val suggestions = mutableListOf("status")
                    suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                    return suggestions.filter { it.startsWith(args[1], ignoreCase = true) }
                }
                if (args.size == 3 && args[1].equals("status", ignoreCase = true)) {
                    val statusIds = plugin.statusConfig.getConfigurationSection("statuses")?.getKeys(false) ?: emptySet<String>()
                    return listOf("set", "clear", *statusIds.toTypedArray()).filter { it.startsWith(args[2], ignoreCase = true) }
                }
            }
            "setprofiletitle", "like", "dislike" -> {
                if (args.size == 2) {
                    return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                }
            }
            "unlike", "undislike" -> {
                if (args.size == 2) {
                    val suggestions = mutableListOf<String>()
                    suggestions.addAll(Bukkit.getOnlinePlayers().map { it.name })
                    if (sender.hasPermission("profileplugin.admin")) {
                        suggestions.add("admin")
                    }
                    return suggestions.filter { it.startsWith(args[1], ignoreCase = true) }
                }
                if (args.size == 3 && args[1].equals("admin", ignoreCase = true)) {
                    return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
                }
            }
        }

        return emptyList()
    }
}