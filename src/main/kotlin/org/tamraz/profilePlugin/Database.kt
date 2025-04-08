package com.tamraz.profileplugin

import org.bukkit.OfflinePlayer
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class Database(private val plugin: ProfilePlugin) {

    private lateinit var connection: Connection

    init {
        initializeDatabase()
    }

    private fun initializeDatabase() {
        val dbFile = File(plugin.dataFolder, "database.db")
        if (!dbFile.exists()) {
            dbFile.parentFile.mkdirs()
            dbFile.createNewFile()
        }

        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().use { statement ->
            // Таблица для профилей
            statement.execute("""
                CREATE TABLE IF NOT EXISTS profiles (
                    uuid TEXT PRIMARY KEY,
                    title TEXT,
                    likes INTEGER DEFAULT 0,
                    dislikes INTEGER DEFAULT 0
                )
            """.trimIndent())

            // Таблица для отслеживания, кто кому поставил лайк/дизлайк
            statement.execute("""
                CREATE TABLE IF NOT EXISTS ratings (
                    rater_uuid TEXT,
                    target_uuid TEXT,
                    rating TEXT, -- 'LIKE' или 'DISLIKE'
                    PRIMARY KEY (rater_uuid, target_uuid)
                )
            """.trimIndent())
        }
    }

    fun setTitle(player: OfflinePlayer, title: String) {
        connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, ?, COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0), COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0))").use { statement ->
            statement.setString(1, player.uniqueId.toString())
            statement.setString(2, title)
            statement.setString(3, player.uniqueId.toString())
            statement.setString(4, player.uniqueId.toString())
            statement.executeUpdate()
        }
    }

    fun getTitle(player: OfflinePlayer): String? {
        connection.prepareStatement("SELECT title FROM profiles WHERE uuid = ?").use { statement ->
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getString("title") else null
        }
    }

    fun getLikes(player: OfflinePlayer): Int {
        connection.prepareStatement("SELECT likes FROM profiles WHERE uuid = ?").use { statement ->
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getInt("likes") else 0
        }
    }

    fun getDislikes(player: OfflinePlayer): Int {
        connection.prepareStatement("SELECT dislikes FROM profiles WHERE uuid = ?").use { statement ->
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            return if (resultSet.next()) resultSet.getInt("dislikes") else 0
        }
    }

    fun setRating(rater: OfflinePlayer, target: OfflinePlayer, rating: String): Boolean {
        // Проверяем, ставил ли rater уже оценку target
        connection.prepareStatement("SELECT rating FROM ratings WHERE rater_uuid = ? AND target_uuid = ?").use { statement ->
            statement.setString(1, rater.uniqueId.toString())
            statement.setString(2, target.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                // Игрок уже ставил оценку
                return false
            }
        }

        // Добавляем запись в таблицу ratings
        connection.prepareStatement("INSERT INTO ratings (rater_uuid, target_uuid, rating) VALUES (?, ?, ?)").use { statement ->
            statement.setString(1, rater.uniqueId.toString())
            statement.setString(2, target.uniqueId.toString())
            statement.setString(3, rating)
            statement.executeUpdate()
        }

        // Обновляем количество лайков или дизлайков в таблице profiles
        if (rating == "LIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0) + 1, COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0))").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.setString(4, target.uniqueId.toString())
                statement.executeUpdate()
            }
        } else if (rating == "DISLIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0), COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0) + 1)").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.setString(4, target.uniqueId.toString())
                statement.executeUpdate()
            }
        }

        return true
    }

    // Метод для удаления одного лайка/дизлайка от конкретного игрока
    fun removeRating(rater: OfflinePlayer, target: OfflinePlayer, rating: String): Boolean {
        // Проверяем, ставил ли rater указанную оценку target
        connection.prepareStatement("SELECT rating FROM ratings WHERE rater_uuid = ? AND target_uuid = ? AND rating = ?").use { statement ->
            statement.setString(1, rater.uniqueId.toString())
            statement.setString(2, target.uniqueId.toString())
            statement.setString(3, rating)
            val resultSet = statement.executeQuery()
            if (!resultSet.next()) {
                // Игрок не ставил такую оценку
                return false
            }
        }

        // Удаляем запись из таблицы ratings
        connection.prepareStatement("DELETE FROM ratings WHERE rater_uuid = ? AND target_uuid = ? AND rating = ?").use { statement ->
            statement.setString(1, rater.uniqueId.toString())
            statement.setString(2, target.uniqueId.toString())
            statement.setString(3, rating)
            statement.executeUpdate()
        }

        // Уменьшаем количество лайков или дизлайков в таблице profiles
        if (rating == "LIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), MAX(COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0) - 1, 0), COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0))").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.setString(4, target.uniqueId.toString())
                statement.executeUpdate()
            }
        } else if (rating == "DISLIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0), MAX(COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0) - 1, 0))").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.setString(4, target.uniqueId.toString())
                statement.executeUpdate()
            }
        }

        return true
    }

    // Метод для удаления всех лайков/дизлайков (для админов)
    fun resetRatings(target: OfflinePlayer, rating: String) {
        // Удаляем все записи из таблицы ratings для указанного типа оценки
        connection.prepareStatement("DELETE FROM ratings WHERE target_uuid = ? AND rating = ?").use { statement ->
            statement.setString(1, target.uniqueId.toString())
            statement.setString(2, rating)
            statement.executeUpdate()
        }

        // Сбрасываем количество лайков или дизлайков в таблице profiles
        if (rating == "LIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), 0, COALESCE((SELECT dislikes FROM profiles WHERE uuid = ?), 0))").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.executeUpdate()
            }
        } else if (rating == "DISLIKE") {
            connection.prepareStatement("INSERT OR REPLACE INTO profiles (uuid, title, likes, dislikes) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), COALESCE((SELECT likes FROM profiles WHERE uuid = ?), 0), 0)").use { statement ->
                statement.setString(1, target.uniqueId.toString())
                statement.setString(2, target.uniqueId.toString())
                statement.setString(3, target.uniqueId.toString())
                statement.executeUpdate()
            }
        }
    }

    fun close() {
        if (::connection.isInitialized) {
            connection.close()
        }
    }
}