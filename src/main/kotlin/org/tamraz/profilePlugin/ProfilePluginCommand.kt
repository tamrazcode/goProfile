package com.tamraz.profileplugin

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ProfilePluginCommand(private val plugin: ProfilePlugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.getMessage("profileplugin.usage"))
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                plugin.reloadConfig()
                plugin.reloadMessages()
                sender.sendMessage(plugin.getMessage("profileplugin.reloaded"))
            }
            else -> {
                sender.sendMessage(plugin.getMessage("profileplugin.usage"))
            }
        }
        return true
    }
}