package org.tamraz.goprofile

import org.bukkit.OfflinePlayer
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class Database(private val plugin: GoProfile) {

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
            statement.execute("""
                CREATE TABLE IF NOT EXISTS profiles (
                    uuid TEXT PRIMARY KEY,
                    title TEXT,
                    status TEXT DEFAULT NULL
                )
            """.trimIndent())

            statement.execute("""
                CREATE TABLE IF NOT EXISTS ratings (
                    rater_uuid TEXT,
                    target_uuid TEXT,
                    rating_type TEXT, -- 'LIKE' или 'DISLIKE'
                    PRIMARY KEY (rater_uuid, target_uuid)
                )
            """.trimIndent())
        }
    }

    fun setTitle(player: OfflinePlayer, title: String) {
        try {
            val statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO profiles (uuid, title, status) VALUES (?, ?, COALESCE((SELECT status FROM profiles WHERE uuid = ?), NULL))"
            )
            statement.setString(1, player.uniqueId.toString())
            statement.setString(2, title)
            statement.setString(3, player.uniqueId.toString())
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to set title for ${player.uniqueId}: ${e.message}")
        }
    }

    fun getTitle(player: OfflinePlayer): String? {
        try {
            val statement = connection.prepareStatement("SELECT title FROM profiles WHERE uuid = ?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            val title = if (resultSet.next()) resultSet.getString("title") else null
            statement.close()
            return title
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get title for ${player.uniqueId}: ${e.message}")
            return null
        }
    }

    fun getLikes(player: OfflinePlayer): Int {
        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) FROM ratings WHERE target_uuid = ? AND rating_type = 'LIKE'")
            statement.setString(1, player.uniqueId.toString())
            val result = statement.executeQuery()
            val count = if (result.next()) result.getInt(1) else 0
            statement.close()
            return count
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get likes for ${player.uniqueId}: ${e.message}")
            return 0
        }
    }

    fun getDislikes(player: OfflinePlayer): Int {
        try {
            val statement = connection.prepareStatement("SELECT COUNT(*) FROM ratings WHERE target_uuid = ? AND rating_type = 'DISLIKE'")
            statement.setString(1, player.uniqueId.toString())
            val result = statement.executeQuery()
            val count = if (result.next()) result.getInt(1) else 0
            statement.close()
            return count
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get dislikes for ${player.uniqueId}: ${e.message}")
            return 0
        }
    }

    fun setRating(rater: OfflinePlayer, target: OfflinePlayer, ratingType: String): Boolean {
        try {
            val checkStatement = connection.prepareStatement(
                "SELECT rating_type FROM ratings WHERE rater_uuid = ? AND target_uuid = ?"
            )
            checkStatement.setString(1, rater.uniqueId.toString())
            checkStatement.setString(2, target.uniqueId.toString())
            val resultSet = checkStatement.executeQuery()
            if (resultSet.next()) {
                checkStatement.close()
                return false
            }
            checkStatement.close()

            val insertStatement = connection.prepareStatement(
                "INSERT INTO ratings (rater_uuid, target_uuid, rating_type) VALUES (?, ?, ?)"
            )
            insertStatement.setString(1, rater.uniqueId.toString())
            insertStatement.setString(2, target.uniqueId.toString())
            insertStatement.setString(3, ratingType)
            insertStatement.executeUpdate()
            insertStatement.close()
            return true
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to set $ratingType for ${rater.uniqueId} on ${target.uniqueId}: ${e.message}")
            return false
        }
    }

    fun removeRating(rater: OfflinePlayer, target: OfflinePlayer, ratingType: String): Boolean {
        try {
            val checkStatement = connection.prepareStatement(
                "SELECT rating_type FROM ratings WHERE rater_uuid = ? AND target_uuid = ? AND rating_type = ?"
            )
            checkStatement.setString(1, rater.uniqueId.toString())
            checkStatement.setString(2, target.uniqueId.toString())
            checkStatement.setString(3, ratingType)
            val resultSet = checkStatement.executeQuery()
            if (!resultSet.next()) {
                checkStatement.close()
                return false
            }
            checkStatement.close()

            val deleteStatement = connection.prepareStatement(
                "DELETE FROM ratings WHERE rater_uuid = ? AND target_uuid = ? AND rating_type = ?"
            )
            deleteStatement.setString(1, rater.uniqueId.toString())
            deleteStatement.setString(2, target.uniqueId.toString())
            deleteStatement.setString(3, ratingType)
            deleteStatement.executeUpdate()
            deleteStatement.close()
            return true
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to remove $ratingType for ${rater.uniqueId} on ${target.uniqueId}: ${e.message}")
            return false
        }
    }

    fun resetRatings(target: OfflinePlayer, ratingType: String) {
        try {
            val statement = connection.prepareStatement(
                "DELETE FROM ratings WHERE target_uuid = ? AND rating_type = ?"
            )
            statement.setString(1, target.uniqueId.toString())
            statement.setString(2, ratingType)
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to reset $ratingType ratings for ${target.uniqueId}: ${e.message}")
        }
    }

    fun setStatus(player: OfflinePlayer, status: String?) {
        try {
            val statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO profiles (uuid, title, status) VALUES (?, COALESCE((SELECT title FROM profiles WHERE uuid = ?), NULL), ?)"
            )
            statement.setString(1, player.uniqueId.toString())
            statement.setString(2, player.uniqueId.toString())
            statement.setString(3, status)
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to set status for ${player.uniqueId}: ${e.message}")
        }
    }

    fun getStatus(player: OfflinePlayer): String? {
        try {
            val statement = connection.prepareStatement("SELECT status FROM profiles WHERE uuid = ?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            val status = if (resultSet.next()) resultSet.getString("status") else null
            statement.close()
            return status
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get status for ${player.uniqueId}: ${e.message}")
            return null
        }
    }

    fun close() {
        try {
            if (!connection.isClosed) {
                connection.close()
            }
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to close database connection: ${e.message}")
        }
    }
}