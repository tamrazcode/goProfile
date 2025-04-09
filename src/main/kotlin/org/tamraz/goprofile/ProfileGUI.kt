package org.tamraz.goprofile

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey

class ProfileGUI(private val plugin: GoProfile, private val target: OfflinePlayer) {

    private lateinit var inventory: org.bukkit.inventory.Inventory

    private fun createInventory() {
        val holder = ProfileInventoryHolder(target)
        val rawTitle = plugin.database.getTitle(target) ?: plugin.config.getString("default_title", "&eПрофиль %player_name%")!!
        val titleWithPlaceholders = plugin.setPlaceholders(target, rawTitle)
        val title = plugin.translateColors(titleWithPlaceholders)
        inventory = Bukkit.createInventory(
            holder,
            plugin.config.getInt("gui.size"),
            title
        )
        holder.setInventory(inventory)
        loadItems()
        loadPlayerItems()
    }

    private fun loadItems() {
        val itemsSection = plugin.config.getConfigurationSection("gui.items") ?: return
        for (key in itemsSection.getKeys(false)) {
            val slot = key.toIntOrNull() ?: continue
            val materialName = itemsSection.getString("$key.material")?.uppercase() ?: continue
            val material = Material.getMaterial(materialName) ?: continue

            val item = ItemStack(material)
            val meta = item.itemMeta ?: continue

            itemsSection.getString("$key.display_name")?.let {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val translated = plugin.translateColors(withPlaceholders)
                val finalDisplayName = if (translated.startsWith("§")) translated else "§r$translated"
                meta.setDisplayName(finalDisplayName)
            }
            itemsSection.getStringList("$key.lore").map {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val translated = plugin.translateColors(withPlaceholders)
                if (translated.startsWith("§")) translated else "§r$translated"
            }.let {
                if (it.isNotEmpty()) meta.lore = it
            }

            if (material == Material.PLAYER_HEAD && itemsSection.getString("$key.head_owner") != null) {
                (meta as SkullMeta).owningPlayer = target
            }

            itemsSection.getString("$key.command")?.let { command ->
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_command"), PersistentDataType.STRING, command)
            }

            if (itemsSection.getBoolean("$key.close", false)) {
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_close"), PersistentDataType.BYTE, 1)
            }

            val cooldownSeconds = itemsSection.getInt("$key.cooldown", 0)
            if (cooldownSeconds > 0) {
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_cooldown"), PersistentDataType.INTEGER, cooldownSeconds)
            }

            itemsSection.getString("$key.sound")?.let { sound ->
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_sound"), PersistentDataType.STRING, sound)
            }

            item.itemMeta = meta
            inventory.setItem(slot, item)
        }
    }

    private fun loadPlayerItems() {
        if (target.isOnline) {
            val player = target.player ?: return
            val playerItems = plugin.config.getConfigurationSection("gui.player_items") ?: return
            playerItems.getInt("helmet").takeIf { it >= 0 }?.let { inventory.setItem(it, player.inventory.helmet) }
            playerItems.getInt("chestplate").takeIf { it >= 0 }?.let { inventory.setItem(it, player.inventory.chestplate) }
            playerItems.getInt("leggings").takeIf { it >= 0 }?.let { inventory.setItem(it, player.inventory.leggings) }
            playerItems.getInt("boots").takeIf { it >= 0 }?.let { inventory.setItem(it, player.inventory.boots) }
            playerItems.getInt("main_hand").takeIf { it >= 0 }?.let { inventory.setItem(it, player.inventory.itemInMainHand) }
            playerItems.getInt("off_hand").takeIf { it >= 0 }?.let {
                inventory.setItem(it, player.inventory.itemInOffHand)
            }
        }
    }

    fun open(player: Player) {
        createInventory()
        player.openInventory(inventory)
        plugin.addActiveProfile(player, this)
    }

    // Метод для получения обновлённых предметов с указанием их слотов
    fun getUpdatedItems(): Map<Int, ItemStack> {
        val updatedItems = mutableMapOf<Int, ItemStack>()
        val itemsSection = plugin.config.getConfigurationSection("gui.items") ?: return updatedItems

        for (key in itemsSection.getKeys(false)) {
            val slot = key.toIntOrNull() ?: continue

            // Проверяем параметр update (по умолчанию true)
            val shouldUpdate = itemsSection.getBoolean("$key.update", true)
            if (!shouldUpdate) continue

            // Обновляем только предметы, которые содержат любые плейсхолдеры
            val displayName = itemsSection.getString("$key.display_name") ?: ""
            val lore = itemsSection.getStringList("$key.lore")
            val placeholderPattern = Regex("%[^%]+%")
            if (!placeholderPattern.containsMatchIn(displayName) &&
                lore.none { placeholderPattern.containsMatchIn(it) }) {
                continue
            }

            val materialName = itemsSection.getString("$key.material")?.uppercase() ?: continue
            val material = Material.getMaterial(materialName) ?: continue

            val item = ItemStack(material)
            val meta = item.itemMeta ?: continue

            itemsSection.getString("$key.display_name")?.let {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val translated = plugin.translateColors(withPlaceholders)
                val finalDisplayName = if (translated.startsWith("§")) translated else "§r$translated"
                meta.setDisplayName(finalDisplayName)
            }
            itemsSection.getStringList("$key.lore").map {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val translated = plugin.translateColors(withPlaceholders)
                if (translated.startsWith("§")) translated else "§r$translated"
            }.let {
                if (it.isNotEmpty()) meta.lore = it
            }

            if (material == Material.PLAYER_HEAD && itemsSection.getString("$key.head_owner") != null) {
                (meta as SkullMeta).owningPlayer = target
            }

            itemsSection.getString("$key.command")?.let { command ->
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_command"), PersistentDataType.STRING, command)
            }

            if (itemsSection.getBoolean("$key.close", false)) {
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_close"), PersistentDataType.BYTE, 1)
            }

            val cooldownSeconds = itemsSection.getInt("$key.cooldown", 0)
            if (cooldownSeconds > 0) {
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_cooldown"), PersistentDataType.INTEGER, cooldownSeconds)
            }

            itemsSection.getString("$key.sound")?.let { sound ->
                val pdc = meta.persistentDataContainer
                pdc.set(NamespacedKey(plugin, "profile_sound"), PersistentDataType.STRING, sound)
            }

            item.itemMeta = meta
            updatedItems[slot] = item
        }

        return updatedItems
    }
}