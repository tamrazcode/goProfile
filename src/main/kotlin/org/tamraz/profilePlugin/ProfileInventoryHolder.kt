package com.tamraz.profileplugin

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class ProfileInventoryHolder : InventoryHolder {
    private lateinit var inventory: Inventory

    override fun getInventory(): Inventory {
        return inventory
    }

    // Метод для установки инвентаря (вызывается автоматически Bukkit)
    fun setInventory(inventory: Inventory) {
        this.inventory = inventory
    }
}