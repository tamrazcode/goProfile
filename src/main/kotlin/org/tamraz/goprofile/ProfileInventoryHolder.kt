package org.tamraz.goprofile

import org.bukkit.OfflinePlayer
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class ProfileInventoryHolder(private val target: OfflinePlayer? = null) : InventoryHolder {
    private lateinit var inventory: Inventory

    override fun getInventory(): Inventory {
        return inventory
    }

    fun setInventory(inventory: Inventory) {
        this.inventory = inventory
    }

    fun getTarget(): OfflinePlayer? {
        return target
    }
}