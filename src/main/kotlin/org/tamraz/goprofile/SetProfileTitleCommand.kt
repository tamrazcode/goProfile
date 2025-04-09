package org.tamraz.goprofile

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class SetProfileTitleCommand(private val plugin: GoProfile) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Проверяем, есть ли у отправителя права администратора
        if (!sender.hasPermission("profileplugin.admin")) {
            sender.sendMessage(plugin.getMessage("setprofiletitle.no-permission"))
            return true
        }

        // Проверяем количество аргументов
        if (args.size < 2) {
            sender.sendMessage(plugin.getMessage("setprofiletitle.usage"))
            return true
        }

        // Получаем игрока по имени
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayer(targetName)
        if (!target.hasPlayedBefore() && !target.isOnline) {
            sender.sendMessage(plugin.getMessage("setprofiletitle.player-not-found", null, targetName))
            return true
        }

        // Объединяем оставшиеся аргументы в title
        val title = args.drop(1).joinToString(" ")
        plugin.database.setTitle(target, title)
        sender.sendMessage(plugin.getMessage("setprofiletitle.success", null, targetName, title))

        // Закрываем GUI для всех игроков, которые смотрят профиль target
        Bukkit.getOnlinePlayers().forEach { player ->
            if (player.openInventory.topInventory.holder is ProfileInventoryHolder &&
                player.openInventory.title == plugin.database.getTitle(target)) {
                player.closeInventory()
            }
        }

        return true
    }
}