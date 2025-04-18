package org.tamraz.goprofile

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

class ProfileGUI(private val plugin: GoProfile, private val target: OfflinePlayer) {

    private lateinit var inventory: org.bukkit.inventory.Inventory

    private var cachedHelmet: ItemStack? = null
    private var cachedChestplate: ItemStack? = null
    private var cachedLeggings: ItemStack? = null
    private var cachedBoots: ItemStack? = null
    private var cachedMainHand: ItemStack? = null
    private var cachedOffHand: ItemStack? = null

    private val plainSerializer = PlainTextComponentSerializer.plainText()
    private val legacySerializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .build()

    private fun createInventory() {
        val holder = ProfileInventoryHolder(target)
        val rawTitle = plugin.database.getTitle(target) ?: plugin.config.getString("default_title", "<yellow>Profile of <player_name>")!!
        val titleWithPlaceholders = plugin.setPlaceholders(target, rawTitle)
        val titleComponent = plugin.parseMiniMessage(titleWithPlaceholders)
        val title = plugin.componentToLegacyString(titleComponent)
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
            if (key.contains("-")) {
                val rangeParts = key.split("-")
                if (rangeParts.size != 2) continue
                val start = rangeParts[0].toIntOrNull() ?: continue
                val end = rangeParts[1].toIntOrNull() ?: continue
                if (start < 0 || end < start || end >= inventory.size) continue

                val materialName = itemsSection.getString("$key.material")?.uppercase() ?: continue
                val material = Material.getMaterial(materialName) ?: continue

                val item = ItemStack(material)
                val meta = item.itemMeta ?: continue

                itemsSection.getString("$key.display_name")?.let {
                    val withPlaceholders = plugin.setPlaceholders(target, it)
                    meta.displayName(plugin.parseMiniMessage(withPlaceholders))
                }
                itemsSection.getStringList("$key.lore").map {
                    val withPlaceholders = plugin.setPlaceholders(target, it)
                    val componentWithPlaceholders = legacySerializer.deserialize(withPlaceholders)
                    val plainText = plainSerializer.serialize(componentWithPlaceholders)
                    plugin.parseMiniMessage(plainText)
                }.let {
                    if (it.isNotEmpty()) meta.lore(it)
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

                for (slot in start..end) {
                    inventory.setItem(slot, item.clone())
                }
            }
        }

        for (key in itemsSection.getKeys(false)) {
            if (key.contains("-")) continue

            val slot = key.toIntOrNull() ?: continue
            val materialName = itemsSection.getString("$key.material")?.uppercase() ?: continue
            val material = Material.getMaterial(materialName) ?: continue

            val item = ItemStack(material)
            val meta = item.itemMeta ?: continue

            itemsSection.getString("$key.display_name")?.let {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                meta.displayName(plugin.parseMiniMessage(withPlaceholders))
            }
            itemsSection.getStringList("$key.lore").map {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val componentWithPlaceholders = legacySerializer.deserialize(withPlaceholders)
                val plainText = plainSerializer.serialize(componentWithPlaceholders)
                plugin.parseMiniMessage(plainText)
            }.let {
                if (it.isNotEmpty()) meta.lore(it)
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
            playerItems.getInt("helmet").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.helmet
                inventory.setItem(slot, item)
                cachedHelmet = item?.clone()
            }
            playerItems.getInt("chestplate").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.chestplate
                inventory.setItem(slot, item)
                cachedChestplate = item?.clone()
            }
            playerItems.getInt("leggings").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.leggings
                inventory.setItem(slot, item)
                cachedLeggings = item?.clone()
            }
            playerItems.getInt("boots").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.boots
                inventory.setItem(slot, item)
                cachedBoots = item?.clone()
            }
            playerItems.getInt("main_hand").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.itemInMainHand
                inventory.setItem(slot, item)
                cachedMainHand = item?.clone()
            }
            playerItems.getInt("off_hand").takeIf { it >= 0 }?.let { slot ->
                val item = player.inventory.itemInOffHand
                inventory.setItem(slot, item)
                cachedOffHand = item?.clone()
            }
        }
    }

    fun open(player: Player) {
        createInventory()
        player.openInventory(inventory)
        plugin.addActiveProfile(player, this)
    }

    fun getUpdatedItems(): Map<Int, ItemStack> {
        val updatedItems = mutableMapOf<Int, ItemStack>()
        val itemsSection = plugin.config.getConfigurationSection("gui.items") ?: return updatedItems

        for (key in itemsSection.getKeys(false)) {
            if (key.contains("-")) continue

            val slot = key.toIntOrNull() ?: continue

            val shouldUpdate = itemsSection.getBoolean("$key.update", true)
            if (!shouldUpdate) continue

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
                meta.displayName(plugin.parseMiniMessage(withPlaceholders))
            }
            itemsSection.getStringList("$key.lore").map {
                val withPlaceholders = plugin.setPlaceholders(target, it)
                val componentWithPlaceholders = legacySerializer.deserialize(withPlaceholders)
                val plainText = plainSerializer.serialize(componentWithPlaceholders)
                plugin.parseMiniMessage(plainText)
            }.let {
                if (it.isNotEmpty()) meta.lore(it)
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

        if (target.isOnline) {
            val player = target.player ?: return updatedItems
            val playerItems = plugin.config.getConfigurationSection("gui.player_items") ?: return updatedItems

            playerItems.getInt("helmet").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.helmet
                if (!isItemEqual(currentItem, cachedHelmet)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedHelmet = currentItem?.clone()
                }
            }
            playerItems.getInt("chestplate").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.chestplate
                if (!isItemEqual(currentItem, cachedChestplate)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedChestplate = currentItem?.clone()
                }
            }
            playerItems.getInt("leggings").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.leggings
                if (!isItemEqual(currentItem, cachedLeggings)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedLeggings = currentItem?.clone()
                }
            }
            playerItems.getInt("boots").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.boots
                if (!isItemEqual(currentItem, cachedBoots)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedBoots = currentItem?.clone()
                }
            }
            playerItems.getInt("main_hand").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.itemInMainHand
                if (!isItemEqual(currentItem, cachedMainHand)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedMainHand = currentItem?.clone()
                }
            }
            playerItems.getInt("off_hand").takeIf { it >= 0 }?.let { slot ->
                val currentItem = player.inventory.itemInOffHand
                if (!isItemEqual(currentItem, cachedOffHand)) {
                    updatedItems[slot] = currentItem ?: ItemStack(Material.AIR)
                    cachedOffHand = currentItem?.clone()
                }
            }
        }

        return updatedItems
    }

    private fun isItemEqual(item1: ItemStack?, item2: ItemStack?): Boolean {
        if (item1 == null && item2 == null) return true
        if (item1 == null || item2 == null) return false
        if (item1.type != item2.type) return false
        if (item1.amount != item2.amount) return false
        return item1.isSimilar(item2)
    }
}