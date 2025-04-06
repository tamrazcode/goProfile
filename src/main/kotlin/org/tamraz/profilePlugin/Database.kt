package com.tamraz.profileplugin

import org.bukkit.OfflinePlayer
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class Database(private val plugin: ProfilePlugin) {

    private lateinit var connection: Connection

    init {
        setupDatabase()
    }

    private fun setupDatabase() {
        val dbFile = File(plugin.dataFolder, "profiles.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
            dbFile.createNewFile()
        }
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().executeUpdate("""
            CREATE TABLE IF NOT EXISTS profiles (
                uuid TEXT PRIMARY KEY,
                title TEXT DEFAULT '&eПрофиль'
            )
        """.trimIndent())
        // Проверяем, существует ли столбец title, и если нет — добавляем его
        try {
            connection.createStatement().executeQuery("SELECT title FROM profiles")
        } catch (e: Exception) {
            connection.createStatement().executeUpdate("ALTER TABLE profiles ADD COLUMN title TEXT DEFAULT '&eПрофиль'")
        }
    }

    fun setTitle(player: OfflinePlayer, title: String) {
        val statement = connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title) VALUES (?, ?)")
        statement.setString(1, player.uniqueId.toString())
        statement.setString(2, title)
        statement.executeUpdate()
    }

    fun getTitle(player: OfflinePlayer): String {
        val statement = connection.prepareStatement("SELECT title FROM profiles WHERE uuid = ?")
        statement.setString(1, player.uniqueId.toString())
        val result = statement.executeQuery()
        return if (result.next()) result.getString("title") else "&eПрофиль"
    }

    fun close() {
        connection.close()
    }
}