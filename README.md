
# MythicFish

A Minecraft plugin for Bukkit/Spigot servers that transforms the fishing experience with custom fish, collections, and quests.

## 📋 Description

MythicFish is a comprehensive plugin that replaces Minecraft's vanilla fishing system with a completely customized experience. Players can catch unique biome-based fish, collect them, and complete quests to earn rewards.

## ✨ Main Features

### 🎣 Custom Fishing System
- **Biome-based fishing**: Different fish appear in specific biomes
- **Multiple rarities**: Fish have different rarity levels (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY)
- **Catch probabilities**: Each fish has its own probability of being caught
- **Custom materials**: Each fish can have a unique material and appearance
- **Detailed descriptions**: Each fish has a customizable description

### 📚 Collection System
- **Persistent collection**: Caught fish are saved permanently
- **Interactive GUI**: Graphical interface to view your collection
- **Progress tracking**: Shows how many fish you've caught in total
- **SQLite database**: Safe and reliable data storage

### 🎯 Quest System
- **Dynamic quests**: Challenges based on catching specific fish
- **Custom rewards**: Earn prizes by completing quests
- **Automatic tracking**: System automatically monitors progress
- **Multiple quests**: Supports different types of objectives

### 🔧 Configuration Management
- **Flexible configuration**: YAML files for fish, messages, and settings
- **Customizable messages**: All plugin messages are modifiable
- **Live reloading**: Ability to reload configuration without server restart

## 🚀 Installation

1. Download the plugin `.jar` file
2. Place the file in your server's `plugins/` folder
3. Restart the server
4. Configuration files will be automatically generated in `plugins/MythicFish/`

## ⚙️ Configuration

### Main Files

- **`config.yml`**: General plugin configuration and fish definitions
- **`messages.yml`**: All messages sent to players

### Fish Structure (config.yml)

```yaml
fish:
  salmon_king:
    display-name: "&6Salmon King"
    color: "&6"
    rarity: "LEGENDARY"
    catch-chance: 0.05
    material: "SALMON"
    description: "A legendary salmon from the deep waters"
    allowed-biomes:
      - "RIVER"
      - "FROZEN_RIVER"
    restricted-biomes: []
```

### Configuration Options

- **`settings.disable-vanilla-fishing`**: Disables vanilla fishing (default: true)
- **`database.enabled`**: Enables SQLite database (default: true)
- **`database.path`**: Database file path (default: plugins/MythicFish/data.db)

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/mythicfish` | Shows plugin help | `mythicfish.help` |
| `/mythicfish collection` | Opens collection GUI | `mythicfish.collection` |
| `/mythicfish quest` | Manages quests | `mythicfish.quest` |
| `/mythicfish quests` | Alias for quest command | `mythicfish.quest` |

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `mythicfish.fish` | Allows fishing with MythicFish | `true` |
| `mythicfish.collection` | Access to collection | `true` |
| `mythicfish.quest` | Access to quests | `true` |
| `mythicfish.help` | View help | `true` |
| `mythicfish.admin` | Administrative commands | `op` |

## 🗃️ Database

The plugin uses SQLite to store:

- **Caught fish**: Every fish caught by every player
- **Timestamps**: When each fish was caught
- **Biomes**: In which biome the fish was caught
- **Quest progress**: Quest status for each player

### Database Structure

```sql
CREATE TABLE player_fish (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    fish_id TEXT NOT NULL,
    caught_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    biome TEXT NOT NULL,
    UNIQUE(player_uuid, fish_id)
);
```

## 🎨 Customization

### Adding New Fish

1. Open `config.yml`
2. Add a new section under `fish:`
3. Configure all necessary properties
4. Reload configuration with `/mythicfish reload`

### Fish Rarities

- **COMMON**: Common fish, easy to find
- **UNCOMMON**: Uncommon fish
- **RARE**: Rare fish
- **EPIC**: Epic fish, very rare
- **LEGENDARY**: Legendary fish, extremely rare

### Supported Biomes

The plugin supports all Minecraft biomes. You can specify:
- **`allowed-biomes`**: Biomes where the fish can be caught
- **`restricted-biomes`**: Biomes where the fish CANNOT be caught

## 🔧 Developer API

### Custom Events

The plugin provides custom events for other plugins:

```java
// Example API usage
MythicFish plugin = MythicFish.getInstance();
FishManager fishManager = plugin.getFishManager();
Fish fish = fishManager.getFish("salmon_king");
```

### Available Managers

- **`ConfigManager`**: Configuration management
- **`MessagesManager`**: Message management
- **`DatabaseManager`**: Database operations
- **`FishManager`**: Fish management
- **`PlayerDataManager`**: Player data
- **`QuestManager`**: Quest management

## 🚨 Troubleshooting

### Plugin won't start
1. Check Java version (requires Java 17+)
2. Check server logs for errors
3. Ensure database can be created

### Fish aren't being caught
1. Verify you have `mythicfish.fish` permission
2. Check if biome is configured correctly
3. Verify catch probabilities in config

### Database not working
1. Check write permissions in plugin folder
2. Verify `database.enabled` is `true`
3. Check logs for SQL errors

## 📝 Requirements

- **Minecraft**: 1.20+
- **Server**: Bukkit/Spigot/Paper
- **Java**: 17+
- **Dependencies**: No external dependencies required

## 📄 License

This project is distributed under license. See the `LICENSE` file for more details.

## 🤝 Contributing

Contributions are welcome! To contribute:

1. Fork the project
2. Create a branch for your feature
3. Commit your changes
4. Push the branch
5. Open a Pull Request

## 📞 Support

For support and bug reports:
- Open an issue on GitHub
- Contact the developers
- Check the documentation

## License

AFKGuard is licensed under the **GNU General Public License v3.0** (GPL-3.0).  
You are free to use, modify, and distribute this software under the terms of the license.  
A copy of the license is available in the [LICENSE](./LICENSE) file.

---

**Developed by Fl1uxxNoob**
