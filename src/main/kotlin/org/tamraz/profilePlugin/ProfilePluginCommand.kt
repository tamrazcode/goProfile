package com.tamraz.profileplugin

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class ProfilePluginCommand(private val plugin: ProfilePlugin) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("profileplugin.admin")) {
            sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
            return true
        }

        if (args.isEmpty() || args[0].equals("reload", ignoreCase = true)) {
            plugin.reloadConfig()
            plugin.reloadMessages()
            sender.sendMessage("§aКонфигурация и сообщения перезагружены!")
            return true
        }

        sender.sendMessage("§cИспользование: /profileplugin reload")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("profileplugin.admin")) return emptyList()

        if (args.size == 1) {
            return listOf("reload").filter { it.startsWith(args[0], ignoreCase = true) }
        }
        return emptyList()
    }
}