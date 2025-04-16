package org.tamraz.goprofile

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.json.JSONObject
import java.io.File
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

    private val legacySerializer = LegacyComponentSerializer.builder()
        .character('§')
        .hexColors()
        .build()

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
        database.assignPlayerId(player)

        if (!player.hasPermission("goprofile.admin")) return
        if (notifiedAdmins.contains(player)) return

        if (versionCheckFailed || latestVersion == null || latestVersion == description.version) return

        if (isVersionOutdated(description.version, latestVersion!!)) {
            val plugin = this
            player.sendMessage(plugin.componentToLegacyString(plugin.parseMiniMessage(
                "<yellow>[goProfile] <red>A new version <white><latest_version> <red>is available! You are using <white><current_version><red>.",
                Placeholder.parsed("latest_version", latestVersion!!),
                Placeholder.parsed("current_version", description.version)
            )))
            player.sendMessage(plugin.componentToLegacyString(plugin.parseMiniMessage(
                "<yellow>[goProfile] <red>Download it from: <white><click:open_url:'https://github.com/tamrazcode/goprofile/releases'>https://github.com/tamrazcode/goprofile/releases</click>"
            )))
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
        val processedText = preprocessLegacyColors(text)
        val finalText = "<i:false>$processedText"
        return miniMessage.deserialize(finalText, *resolvers)
    }

    fun componentToLegacyString(component: Component): String {
        return legacySerializer.serialize(component)
    }

    private fun preprocessLegacyColors(text: String): String {
        var result = text

        val hexPattern = Regex("[&§]#([A-Fa-f0-9]{6})")
        result = hexPattern.replace(result) { match ->
            val hex = match.groupValues[1]
            "<color:#$hex>"
        }

        val legacyPattern = Regex("[&§]([0-9a-fklmnor])")
        result = legacyPattern.replace(result) { match ->
            when (match.groupValues[1]) {
                "0" -> "<black>"
                "1" -> "<dark_blue>"
                "2" -> "<dark_green>"
                "3" -> "<dark_aqua>"
                "4" -> "<dark_red>"
                "5" -> "<dark_purple>"
                "6" -> "<gold>"
                "7" -> "<gray>"
                "8" -> "<dark_gray>"
                "9" -> "<blue>"
                "a" -> "<green>"
                "b" -> "<aqua>"
                "c" -> "<red>"
                "d" -> "<light_purple>"
                "e" -> "<yellow>"
                "f" -> "<white>"
                "k" -> "<obfuscated>"
                "l" -> "<bold>"
                "m" -> "<strikethrough>"
                "n" -> "<underline>"
                "o" -> "<italic>"
                "r" -> "<reset>"
                else -> ""
            }
        }

        return result
    }

    fun getPlayerById(id: Int): OfflinePlayer? {
        return database.getPlayerById(id)
    }
}