package net.fliuxx.mythicFish.database;

import net.fliuxx.mythicFish.MythicFish;
import net.fliuxx.mythicFish.player.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * SQLite persistence layer.
 * <p>
 * All SQL runs through a single-threaded {@link ExecutorService} so the single (non thread-safe)
 * connection is never touched concurrently. Writes are fire-and-forget; reads return a
 * {@link CompletableFuture}. Gameplay reads are served from the in-memory player cache instead of
 * this class, so the only reads here are the per-player load and the leaderboard aggregate.
 * <p>
 * When {@code settings.async-database} is {@code false} every operation runs inline on the calling
 * thread (original behaviour, useful for debugging).
 */
public class DatabaseManager {

    /** A single leaderboard row. */
    public record TopEntry(String name, int catches) {}

    private final MythicFish plugin;
    private Connection connection;
    private ExecutorService executor;
    private boolean async;

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

            this.async = plugin.getConfigManager().isAsyncDatabase();
            this.executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MythicFish-DB");
                t.setDaemon(true);
                return t;
            });
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

        String createPlayerStatsTable = """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid TEXT PRIMARY KEY,
                name TEXT,
                total_catches INTEGER DEFAULT 0
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayerFishTable);
            stmt.execute(createPlayerQuestsTable);
            stmt.execute(createPlayerStatsTable);

            // Schema migrations for databases created by older versions (each ignored if already applied)
            for (String migration : new String[]{
                    "ALTER TABLE player_quests ADD COLUMN progress INTEGER DEFAULT 0",
                    "ALTER TABLE player_quests ADD COLUMN completed BOOLEAN DEFAULT FALSE",
                    "ALTER TABLE player_stats ADD COLUMN name TEXT"
            }) {
                try {
                    stmt.execute(migration);
                } catch (SQLException ignored) {
                    // Column already exists
                }
            }

            // Speeds up the leaderboard ORDER BY on large databases
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_stats_catches ON player_stats(total_catches)");
        }
    }

    // --- Async dispatch helpers ---

    private void write(Runnable raw) {
        if (async && executor != null && !executor.isShutdown()) {
            executor.execute(raw);
        } else {
            raw.run();
        }
    }

    private <T> CompletableFuture<T> read(Supplier<T> raw) {
        if (async && executor != null && !executor.isShutdown()) {
            return CompletableFuture.supplyAsync(raw, executor);
        }
        return CompletableFuture.completedFuture(raw.get());
    }

    // --- Loading & leaderboard (the only reads that hit the DB) ---

    /**
     * Load a full progression snapshot for one player. Runs on the DB thread.
     */
    public CompletableFuture<PlayerData> loadPlayerData(UUID playerUUID) {
        return read(() -> {
            PlayerData data = new PlayerData();
            String uuid = playerUUID.toString();

            String fishSql = "SELECT fish_id FROM player_fish WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(fishSql)) {
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    data.addCaughtFish(rs.getString("fish_id"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player fish: " + e.getMessage());
            }

            String statsSql = "SELECT total_catches FROM player_stats WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(statsSql)) {
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.setTotalCatches(rs.getInt("total_catches"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player stats: " + e.getMessage());
            }

            // claimed_at is stored via CURRENT_TIMESTAMP (UTC); strftime('%s', ...) gives its epoch seconds.
            String questSql = "SELECT quest_id, progress, completed, claimed, " +
                    "CAST(strftime('%s', claimed_at) AS INTEGER) AS claimed_epoch " +
                    "FROM player_quests WHERE player_uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(questSql)) {
                pstmt.setString(1, uuid);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String questId = rs.getString("quest_id");
                    data.setQuestProgress(questId, rs.getInt("progress"));
                    if (rs.getBoolean("completed")) {
                        data.markQuestCompleted(questId);
                    }
                    if (rs.getBoolean("claimed")) {
                        data.markQuestClaimed(questId);
                        long claimedEpoch = rs.getLong("claimed_epoch");
                        if (claimedEpoch > 0) {
                            data.setQuestClaimedAt(questId, claimedEpoch);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load player quests: " + e.getMessage());
            }

            return data;
        });
    }

    public CompletableFuture<List<TopEntry>> getTopCatches(int limit) {
        return read(() -> {
            List<TopEntry> top = new ArrayList<>();
            String sql = "SELECT name, total_catches FROM player_stats " +
                        "WHERE name IS NOT NULL ORDER BY total_catches DESC LIMIT ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    top.add(new TopEntry(rs.getString("name"), rs.getInt("total_catches")));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to load leaderboard: " + e.getMessage());
            }
            return top;
        });
    }

    // --- Writes (fire-and-forget) ---

    public void addFishToPlayer(UUID playerUUID, String fishId, String biome) {
        write(() -> {
            String sql = "INSERT OR IGNORE INTO player_fish (player_uuid, fish_id, biome) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, fishId);
                pstmt.setString(3, biome);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to add fish to player: " + e.getMessage());
            }
        });
    }

    public void incrementTotalCatches(UUID playerUUID, String name) {
        write(() -> {
            String sql = "INSERT INTO player_stats (player_uuid, name, total_catches) VALUES (?, ?, 1) " +
                        "ON CONFLICT(player_uuid) DO UPDATE SET total_catches = total_catches + 1, name = excluded.name";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to increment total catches: " + e.getMessage());
            }
        });
    }

    public void setQuestClaimed(UUID playerUUID, String questId) {
        write(() -> {
            String sql = "UPDATE player_quests SET claimed = TRUE, claimed_at = CURRENT_TIMESTAMP WHERE player_uuid = ? AND quest_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, questId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to set quest as claimed: " + e.getMessage());
            }
        });
    }

    public void updateQuestProgress(UUID playerUUID, String questId, int increment) {
        write(() -> {
            String selectSql = "SELECT progress FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
            String insertSql = "INSERT INTO player_quests (player_uuid, quest_id, progress, completed, claimed) VALUES (?, ?, ?, FALSE, FALSE)";
            String updateSql = "UPDATE player_quests SET progress = progress + ? WHERE player_uuid = ? AND quest_id = ?";
            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
                selectStmt.setString(1, playerUUID.toString());
                selectStmt.setString(2, questId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, increment);
                        updateStmt.setString(2, playerUUID.toString());
                        updateStmt.setString(3, questId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, playerUUID.toString());
                        insertStmt.setString(2, questId);
                        insertStmt.setInt(3, increment);
                        insertStmt.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to update quest progress: " + e.getMessage());
            }
        });
    }

    public void markQuestCompleted(UUID playerUUID, String questId) {
        write(() -> {
            // Ensure a row exists (a CATCH_TOTAL quest may complete before any progress row is written)
            String upsertSql = "INSERT INTO player_quests (player_uuid, quest_id, completed, completed_at) " +
                        "VALUES (?, ?, TRUE, CURRENT_TIMESTAMP) " +
                        "ON CONFLICT(player_uuid, quest_id) DO UPDATE SET completed = TRUE, completed_at = CURRENT_TIMESTAMP";
            try (PreparedStatement pstmt = connection.prepareStatement(upsertSql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, questId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to mark quest as completed: " + e.getMessage());
            }
        });
    }

    // --- Admin writes ---

    public void removeFishFromPlayer(UUID playerUUID, String fishId) {
        write(() -> {
            String sql = "DELETE FROM player_fish WHERE player_uuid = ? AND fish_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, fishId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove fish from player: " + e.getMessage());
            }
        });
    }

    /**
     * Remove a single quest's row so its progress/completion/claim state is wiped. Used both by the
     * repeatable-quest cooldown reset and by the admin {@code resetquest} command.
     */
    public void resetQuest(UUID playerUUID, String questId) {
        write(() -> {
            String sql = "DELETE FROM player_quests WHERE player_uuid = ? AND quest_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, questId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset quest: " + e.getMessage());
            }
        });
    }

    public void resetPlayerCollection(UUID playerUUID) {
        write(() -> {
            String uuid = playerUUID.toString();
            try (PreparedStatement fishStmt = connection.prepareStatement("DELETE FROM player_fish WHERE player_uuid = ?");
                 PreparedStatement questStmt = connection.prepareStatement("DELETE FROM player_quests WHERE player_uuid = ?");
                 PreparedStatement statsStmt = connection.prepareStatement("DELETE FROM player_stats WHERE player_uuid = ?")) {
                fishStmt.setString(1, uuid);
                questStmt.setString(1, uuid);
                statsStmt.setString(1, uuid);
                fishStmt.executeUpdate();
                questStmt.executeUpdate();
                statsStmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset player collection: " + e.getMessage());
            }
        });
    }

    // --- Lifecycle ---

    public void closeConnection() {
        // Drain pending async writes before closing the connection so nothing is lost on shutdown.
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Database tasks did not finish within 10s; forcing shutdown.");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}
