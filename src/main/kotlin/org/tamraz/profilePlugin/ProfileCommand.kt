package com.tamraz.profileplugin

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ProfileCommand(private val plugin: ProfilePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Эта команда только для игроков!")
            return true
        }

        val target = if (args.isEmpty()) {
            sender // Если аргументов нет, показываем профиль отправителя
        } else {
            // Сначала ищем онлайн-игрока
            val onlinePlayer = Bukkit.getPlayerExact(args[0])
            if (onlinePlayer != null) {
                onlinePlayer
            } else {
                // Если игрок не онлайн, ищем в кэше оффлайн-игроков
                val offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[0])
                if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                    offlinePlayer
                } else {
                    sender.sendMessage(plugin.getMessage("profile.player-not-found", sender, args[0]))
                    return true
                }
            }
        }

        val gui = ProfileGUI(plugin, target)
        gui.open(sender)
        return true
    }
}