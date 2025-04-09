# goProfile

![GitHub release (latest by date)](https://img.shields.io/github/v/release/tamrazcode/goProfile?color=blue&style=flat-square)
![GitHub issues](https://img.shields.io/github/issues/tamrazcode/goProfile?style=flat-square)

**goProfile** is a lightweight and feature-rich Minecraft plugin for Paper/Spigot servers that allows players to view profiles, set likes/dislikes, and customize their status. Perfect for enhancing the social experience on your server!

---

## ⚠️ A Small Note from the Author

I'm fairly new to creating Minecraft plugins, but at some point, I needed a plugin with similar functionality and couldn't find a good one in the open-source community, so I decided to make my own. I hope you enjoy using **goProfile**! ❤️

I also inform you that Grok AI was used to create the plugin - thank you bro.

---

## ✨ Features

- **Fully Customizable GUI**: Configure the profile GUI to fit your server's style.
- **Player Profiles**: View detailed player profiles with `/goprofile profile [player]`.
- **Likes & Dislikes**: Players can like or dislike others with `/goprofile like <player>` and `/goprofile dislike <player>`.
- **Remove Ratings**: Undo likes/dislikes with `/goprofile unlike <player>` and `/goprofile undislike <player>`.
- **Custom Statuses**: Set a status with `/goprofile profile status <identifier> | set <text> | clear`.
- **Admin Tools**: Reset ratings or set custom profile titles with admin commands (Targeted for use by Nexo or ItemsAdder).
- **PlaceholderAPI Support**: Use placeholders like `%profile_like%`, `%profile_dislike%`, and `%profile_status%`.
- **Multilingual**: Supports English (`en_us`) and Russian (`ru_ru`) languages or any other (Edit `messages_en.yml`).

---

## 📥 Installation

1. Download the latest version of `goProfile.jar` from the [Releases](https://github.com/tamrazcode/goProfile/releases) page.
2. Place the `goProfile.jar` file into your server's `plugins` folder.
3. Restart your server to load the plugin.
4. Configure the plugin in the `plugins/goProfile/` folder (optional).

---

## 🔧 Requirements

- **Server**: Paper/Spigot 1.21 (or higher).
- **Optional**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for placeholder support.

---

## 📜 Commands

| Command                                    | Description                                      | Permission         |
|--------------------------------------------|--------------------------------------------------|--------------------|
| `/goprofile profile [player]`              | Open a player's profile.                         | `goprofile.use`    |
| `/goprofile like <player>`                 | Like a player.                                   | `goprofile.use`    |
| `/goprofile dislike <player>`              | Dislike a player.                                | `goprofile.use`    |
| `/goprofile unlike [admin] <player>`       | Remove your like (or all likes if `admin`).      | `goprofile.use` / `goprofile.admin` |
| `/goprofile undislike [admin] <player>`    | Remove your dislike (or all dislikes if `admin`).| `goprofile.use` / `goprofile.admin` |
| `/goprofile profile status <id> \| set <text> \| clear` | Set or clear your status.                  | `goprofile.use`    |
| `/goprofile setprofiletitle <player> <title>` | Set a custom title for a player's profile.    | `goprofile.admin`  |
| `/goprofile reload`                        | Reload the plugin's configuration.               | `goprofile.admin`  |

---

## 🔗 Placeholders (with PlaceholderAPI)

| Placeholder         | Description               |
|---------------------|---------------------------|
| `%profile_like%`    | Number of likes.          |
| `%profile_dislike%` | Number of dislikes.       |
| `%profile_status%`  | Player's current status.  |

---

## 📖 Usage

1. **View a Profile**:
    - Use `/goprofile profile` to view your own profile, or `/goprofile profile <player>` to view someone else's.
    - The GUI shows playtime, rank, likes, dislikes, status, and more by default (Fully customizable).

2. **Set a Status**:
    - Use `/goprofile profile status online` to set a preset status.
    - Or set a custom status with `/goprofile profile status set Looking for friends!`.

3. **Like or Dislike**:
    - Like a player with `/goprofile like <player>`.
    - Remove your like with `/goprofile unlike <player>`.

4. **Admin Features**:
    - Reset all likes for a player: `/goprofile unlike admin <player>`.
    - Set a custom profile title: `/goprofile setprofiletitle <player> &aVIP Player`.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! If you have ideas, bug reports, or improvements:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

---

## 📬 Support

If you have questions or need help, feel free to open an issue on GitHub or join my [Discord](https://discord.gg/ktRjwkR7yp)

---

**Made with love by tamraz ❤️**
