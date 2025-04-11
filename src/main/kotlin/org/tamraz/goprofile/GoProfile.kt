package org.tamraz.goprofile

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import org.bukkit.scheduler.BukkitRunnable
import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.inventory.ItemStack
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GoProfile : JavaPlugin(), Listener {

    lateinit var database: Database
        private set

    lateinit var messages: YamlConfiguration
        private set

    lateinit var statusConfig: YamlConfiguration
        private set

    private val activeProfiles = mutableMapOf<org.bukkit.entity.Player, ProfileGUI>()
    private val notifiedAdmins = mutableSetOf<org.bukkit.entity.Player>()
    private var latestVersion: String? = null
    private var versionCheckFailed: Boolean = false

    private val miniMessage = MiniMessage.miniMessage()

    override fun onEnable() {
        saveDefaultConfig()
        saveDefaultMessages()
        saveDefaultStatusConfig()
        database = Database(this)

        val profilePluginCommand = GoProfileCommand(this)
        getCommand("goprofile")?.setExecutor(profilePluginCommand)
        getCommand("goprofile")?.tabCompleter = profilePluginCommand

        Bukkit.getPluginManager().registerEvents(InventoryClickListener(this), this)
        Bukkit.getPluginManager().registerEvents(this, this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            ProfilePlaceholderExpansion(this).register()
        }

        startGuiUpdateTask()

        if (config.getBoolean("version-check", true)) {
            checkForUpdates()
        }

        logger.info("goProfile successfully started!")
    }

    override fun onDisable() {
        database.close()
        activeProfiles.clear()
        notifiedAdmins.clear()
        logger.info("goProfile disabled!")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!player.hasPermission("goprofile.admin")) return
        if (notifiedAdmins.contains(player)) return

        if (versionCheckFailed || latestVersion == null || latestVersion == description.version) return

        if (isVersionOutdated(description.version, latestVersion!!)) {
            player.sendMessage(parseMiniMessage(
                "<yellow>[goProfile] <red>A new version <white><latest_version> <red>is available! You are using <white><current_version><red>.",
                Placeholder.parsed("latest_version", latestVersion!!),
                Placeholder.parsed("current_version", description.version)
            ))
            player.sendMessage(parseMiniMessage(
                "<yellow>[goProfile] <red>Download it from: <white><click:open_url:'https://github.com/tamrazcode/goprofile/releases'>https://github.com/tamrazcode/goprofile/releases</click>"
            ))
            notifiedAdmins.add(player)
        }
    }

    private fun checkForUpdates() {
        object : BukkitRunnable() {
            override fun run() {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url("https://api.github.com/repos/tamrazcode/goprofile/releases/latest")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            logger.warning("Failed to check for updates: HTTP ${response.code}")
                            versionCheckFailed = true
                            return
                        }

                        val body = response.body?.string()
                        if (body == null) {
                            logger.warning("Failed to check for updates: Response body is null")
                            versionCheckFailed = true
                            return
                        }

                        val json = JSONObject(body)
                        latestVersion = json.getString("tag_name").trimStart('v')
                        logger.info("Latest version on GitHub: $latestVersion")
                    }
                } catch (e: Exception) {
                    logger.warning("Failed to check for updates: ${e.message}")
                    versionCheckFailed = true
                }
            }
        }.runTaskAsynchronously(this)
    }

    private fun isVersionOutdated(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (currentPart < latestPart) return true
            if (currentPart > latestPart) return false
        }
        return false
    }

    private fun startGuiUpdateTask() {
        object : BukkitRunnable() {
            override fun run() {
                if (activeProfiles.isEmpty()) return

                object : BukkitRunnable() {
                    override fun run() {
                        val updates = mutableMapOf<org.bukkit.entity.Player, Map<Int, ItemStack>>()
                        for ((player, gui) in activeProfiles.entries.toList()) {
                            if (!player.isOnline || player.openInventory.topInventory.holder !is ProfileInventoryHolder) {
                                activeProfiles.remove(player)
                                continue
                            }
                            val updatedItems = gui.getUpdatedItems()
                            updates[player] = updatedItems
                        }

                        object : BukkitRunnable() {
                            override fun run() {
                                for ((player, items) in updates) {
                                    val inventory = player.openInventory.topInventory
                                    for ((slot, item) in items) {
                                        inventory.setItem(slot, item)
                                    }
                                }
                            }
                        }.runTask(this@GoProfile)
                    }
                }.runTaskAsynchronously(this@GoProfile)
            }
        }.runTaskTimer(this, 0L, 100L)
    }

    fun addActiveProfile(player: org.bukkit.entity.Player, gui: ProfileGUI) {
        activeProfiles[player] = gui
    }

    fun removeActiveProfile(player: org.bukkit.entity.Player) {
        activeProfiles.remove(player)
    }

    fun setPlaceholders(player: org.bukkit.OfflinePlayer, text: String): String {
        return PlaceholderAPI.setPlaceholders(player, text)
    }

    private fun saveDefaultMessages() {
        val messagesEnFile = File(dataFolder, "messages_en.yml")
        val messagesRuFile = File(dataFolder, "messages_ru.yml")

        if (!messagesEnFile.exists()) {
            saveResource("messages_en.yml", false)
        }
        if (!messagesRuFile.exists()) {
            saveResource("messages_ru.yml", false)
        }

        val language = config.getString("language", "en_us")
        val messagesFileName = if (language == "ru_ru") "messages_ru.yml" else "messages_en.yml"
        val messagesFile = File(dataFolder, messagesFileName)
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    private fun saveDefaultStatusConfig() {
        val statusFile = File(dataFolder, "status.yml")
        if (!statusFile.exists()) {
            saveResource("status.yml", false)
        }
        statusConfig = YamlConfiguration.loadConfiguration(statusFile)
    }

    fun reloadMessages() {
        val language = config.getString("language", "en_us")
        val messagesFileName = if (language == "ru_ru") "messages_ru.yml" else "messages_en.yml"
        val messagesFile = File(dataFolder, messagesFileName)
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }

    fun reloadStatusConfig() {
        val statusFile = File(dataFolder, "status.yml")
        statusConfig = YamlConfiguration.loadConfiguration(statusFile)
    }

    fun getMessage(key: String, player: org.bukkit.OfflinePlayer? = null, vararg args: TagResolver): Component {
        val message = messages.getString(key, "Message not found: $key") ?: "Message not found: $key"
        val withPlaceholders = if (player != null) setPlaceholders(player, message) else message
        return parseMiniMessage(withPlaceholders, *args)
    }

    fun parseMiniMessage(text: String, vararg resolvers: TagResolver): Component {
        // Сначала преобразуем устаревшие форматы (HEX и & коды) в MiniMessage
        val processedText = preprocessLegacyColors(text)
        // Добавляем <i:false> в начало, чтобы отключить курсив по умолчанию
        val finalText = "<i:false>$processedText"
        // Парсим с помощью MiniMessage
        return miniMessage.deserialize(finalText, *resolvers)
    }

    private fun preprocessLegacyColors(text: String): String {
        var result = text

        // Преобразуем HEX-коды (например, &#E2ADFD) в MiniMessage формат <color:#E2ADFD>
        val hexPattern = Regex("&#([A-Fa-f0-9]{6})")
        result = hexPattern.replace(result) { match ->
            val hex = match.groupValues[1]
            "<color:#$hex>"
        }

        // Преобразуем обычные цветовые коды & в MiniMessage теги
        result = result.replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underline>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>")

        return result
    }
}