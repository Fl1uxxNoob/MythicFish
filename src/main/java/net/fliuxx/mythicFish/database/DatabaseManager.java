package net.fliuxx.mythicFish.database;

import net.fliuxx.mythicFish.MythicFish;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final MythicFish plugin;
    private Connection connection;

    public DatabaseManager(MythicFish plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            String path = plugin.getConfigManager().getDatabasePath();
            File dbFile = new File(path);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            createTables();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            return false;
        }
    }

    private void createTables() throws SQLException {
        String createPlayerFishTable = """
            CREATE TABLE IF NOT EXISTS player_fish (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                fish_id TEXT NOT NULL,
                caught_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                biome TEXT NOT NULL,
                UNIQUE(player_uuid, fish_id)
            )
        """;

        String createPlayerQuestsTable = """
            CREATE TABLE IF NOT EXISTS player_quests (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid TEXT NOT NULL,
                quest_id TEXT NOT NULL,
                progress INTEGER DEFAULT 0,
                completed BOOLEAN DEFAULT FALSE,
                completed_at TIMESTAMP NULL,
                claimed BOOLEAN DEFAULT FALSE,
                claimed_at TIMESTAMP NULL,
                UNIQUE(player_uuid, quest_id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerFishTable);
            stmt.execute(createPlayerQuestsTable);

            // Add progress column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE player_quests ADD COLUMN progress INTEGER DEFAULT 0");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Add completed column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE player_quests ADD COLUMN completed BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
                // Column already exists, ignore
            }
        }
    }

    public void addFishToPlayer(UUID playerUUID, String fishId, String biome) {
        String sql = "INSERT OR IGNORE INTO player_fish (player_uuid, fish_id, biome) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, fishId);
            pstmt.setString(3, biome);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add fish to player: " + e.getMessage());
        }
    }

    public Set<String> getPlayerFish(UUID playerUUID) {
        Set<String> fishIds = new HashSet<>();
        String sql = "SELECT fish_id FROM player_fish WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                fishIds.add(rs.getString("fish_id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player fish: " + e.getMessage());
        }

        return fishIds;
    }

    public boolean hasPlayerCaughtFish(UUID playerUUID, String fishId) {
        String sql = "SELECT 1 FROM player_fish WHERE player_uuid = ? AND fish_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, fishId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check if player caught fish: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Long> getPlayerFishWithTimestamps(UUID playerUUID) {
        Map<String, Long> fishData = new HashMap<>();
        String sql = "SELECT fish_id, caught_at FROM player_fish WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                fishData.put(rs.getString("fish_id"), rs.getTimestamp("caught_at").getTime());
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player fish with timestamps: " + e.getMessage());
        }

        return fishData;
    }

    // Quest-related methods
    public void addCompletedQuest(UUID playerUUID, String questId) {
        String sql = "INSERT OR IGNORE INTO player_quests (player_uuid, quest_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add completed quest: " + e.getMessage());
        }
    }

    public boolean hasPlayerCompletedQuest(UUID playerUUID, String questId) {
        String sql = "SELECT completed FROM player_quests WHERE player_uuid = ? AND quest_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("completed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check completed quest: " + e.getMessage());
        }
        return false;
    }

    public boolean hasPlayerClaimedQuest(UUID playerUUID, String questId) {
        String sql = "SELECT claimed FROM player_quests WHERE player_uuid = ? AND quest_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("claimed");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check claimed quest: " + e.getMessage());
        }
        return false;
    }

    public void setQuestClaimed(UUID playerUUID, String questId) {
        String sql = "UPDATE player_quests SET claimed = TRUE, claimed_at = CURRENT_TIMESTAMP WHERE player_uuid = ? AND quest_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set quest as claimed: " + e.getMessage());
        }
    }

    public int getCompletedQuestCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM player_quests WHERE player_uuid = ? AND completed = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get completed quest count: " + e.getMessage());
        }
        return 0;
    }

    public int getClaimedQuestCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM player_quests WHERE player_uuid = ? AND claimed = 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get claimed quest count: " + e.getMessage());
        }
        return 0;
    }

    public void setQuestProgress(UUID playerUUID, String questId, int progress) {
        String sql = "INSERT OR REPLACE INTO player_quests (player_uuid, quest_id, progress, completed, claimed) " +
                    "VALUES (?, ?, ?, " +
                    "COALESCE((SELECT completed FROM player_quests WHERE player_uuid = ? AND quest_id = ?), 0), " +
                    "COALESCE((SELECT claimed FROM player_quests WHERE player_uuid = ? AND quest_id = ?), 0))";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            pstmt.setInt(3, progress);
            pstmt.setString(4, playerUUID.toString());
            pstmt.setString(5, questId);
            pstmt.setString(6, playerUUID.toString());
            pstmt.setString(7, questId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set quest progress: " + e.getMessage());
        }
    }

    public int getPlayerFishCountByRarity(UUID playerUUID, net.fliuxx.mythicFish.fish.FishRarity rarity) {
        String sql = """
            SELECT COUNT(*) FROM player_fish pf 
            JOIN (SELECT fish_id, rarity FROM fish_config) fc ON pf.fish_id = fc.fish_id 
            WHERE pf.player_uuid = ? AND fc.rarity = ?
        """;

        // Since we don't have a fish_config table, we'll get all player fish and check their rarity
        Set<String> playerFish = getPlayerFish(playerUUID);
        int count = 0;

        for (String fishId : playerFish) {
            net.fliuxx.mythicFish.fish.Fish fish = plugin.getFishManager().getFish(fishId);
            if (fish != null && fish.getRarity() == rarity) {
                count++;
            }
        }

        return count;
    }

    // Admin methods
    public void removeFishFromPlayer(UUID playerUUID, String fishId) {
        String sql = "DELETE FROM player_fish WHERE player_uuid = ? AND fish_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, fishId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove fish from player: " + e.getMessage());
        }
    }

    public void resetPlayerCollection(UUID playerUUID) {
        String deleteFishSql = "DELETE FROM player_fish WHERE player_uuid = ?";
        String deleteQuestsSql = "DELETE FROM player_quests WHERE player_uuid = ?";

        try (PreparedStatement fishStmt = connection.prepareStatement(deleteFishSql);
             PreparedStatement questStmt = connection.prepareStatement(deleteQuestsSql)) {

            fishStmt.setString(1, playerUUID.toString());
            questStmt.setString(1, playerUUID.toString());

            fishStmt.executeUpdate();
            questStmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to reset player collection: " + e.getMessage());
        }
    }

    public void updateQuestProgress(UUID playerUUID, String questId, int increment) {
        String selectSql = "SELECT progress FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
        String insertSql = "INSERT INTO player_quests (player_uuid, quest_id, progress, completed, claimed) VALUES (?, ?, ?, FALSE, FALSE)";
        String updateSql = "UPDATE player_quests SET progress = progress + ? WHERE player_uuid = ? AND quest_id = ?";

        try {
            // Check if quest progress record exists
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setString(1, playerUUID.toString());
                selectStmt.setString(2, questId);
                ResultSet rs = selectStmt.executeQuery();

                if (rs.next()) {
                    // Update existing progress
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, increment);
                        updateStmt.setString(2, playerUUID.toString());
                        updateStmt.setString(3, questId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Create new progress record
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.setString(2, questId);
                        insertStmt.setInt(3, increment);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update quest progress: " + e.getMessage());
        }
    }

    public int getQuestProgress(UUID playerUUID, String questId) {
        String sql = "SELECT progress FROM player_quests WHERE player_uuid = ? AND quest_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("progress");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get quest progress: " + e.getMessage());
        }
        return 0;
    }

    public void markQuestCompleted(UUID playerUUID, String questId) {
        String sql = "UPDATE player_quests SET completed = TRUE WHERE player_uuid = ? AND quest_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, questId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to mark quest as completed: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}