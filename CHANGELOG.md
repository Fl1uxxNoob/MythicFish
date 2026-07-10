# Changelog

All notable changes to MythicFish are documented in this file.

## [1.3.2]

### Fixed
- **Color code bug**: messages with placeholders that themselves contain `&` color codes (e.g. a
  colored fish name inside `{fish}`, or a colored quest name inside `{quest}`) were showing the
  literal `&` instead of being colored. `MessagesManager` now substitutes placeholders first and
  translates `&` codes once at the end, instead of the other way around.
- Fish and quest **descriptions** with `&` color codes were not translated when shown in item lore
  or GUIs (`FishingListener`, `CollectionGUI`, `QuestGUI`) — now translated consistently.

### Added
- **Configurable GUI/message strings**: rarity names, Collection GUI text (locked/unlocked labels,
  biome hints, stats item), Quest GUI text (requirement/progress/rewards/status labels, stats item),
  and the caught-fish item's "Caught with MythicFish" lore line are now all defined in
  `messages.yml` under `rarities`, `gui.collection.*`, `gui.quest.*`, `gui.stats.*` and
  `fish-item.caught-with`, instead of being hardcoded in Java. Existing `messages.yml` files
  without these new keys keep working via built-in fallback defaults.
- **Repeatable quests**: quests can now define `cooldown-seconds` in `config.yml` (default `0` =
  one-time, as before). A repeatable quest resets that many seconds after its reward is **claimed**,
  becoming completable again. The Quest GUI shows a "⏳ Available again in ..." countdown while a
  repeatable quest is on cooldown.
- **Admin command** `/mythicfish admin resetquest <player> <questId>`: immediately resets a
  player's progress/cooldown for a given quest (works online and offline, with tab-completion).
- **`/mythicfish credits`** command: available to all players (no permission required), prints
  "Developed by Fl1uxxNoob" (message configurable via `credits-message` in `messages.yml`).
- bStats metrics activated with the registered plugin ID (`32521`).

### Changed
- `messages.yml`: new `credits-message`, `rarities.*`, `fish-item.caught-with`, `gui.*` sections;
  admin help text and usage now mention `resetquest`; new `admin-resetquest-usage`,
  `quest-not-found`, `admin-quest-reset` messages; help message now lists `/mythicfish credits`.
- `config.yml`: every quest now documents/accepts an optional `cooldown-seconds` field; the
  `master_fisher` quest is set up as a repeatable example (`cooldown-seconds: 86400`, 24h).
- `plugin.yml`: command usage string now includes `credits`.

### Internal
- `PlayerData` tracks a per-quest claim timestamp and exposes `resetQuest(id)` to clear a single
  quest's progress/completion/claim state.
- `DatabaseManager` persists/reads the claim timestamp (`claimed_at` as epoch seconds) and exposes
  `resetQuest(uuid, questId)` to delete a single quest row.
- `QuestManager` exposes `applyQuestCooldowns(...)` (lazily resets expired repeatable quests on
  each catch and on Quest GUI open) and `getRemainingCooldown(...)`. Repeatable `CATCH_TOTAL`
  quests track progress per-cycle instead of lifetime total, so they don't instantly re-complete
  after a reset.

## [1.3.1]
See previous release notes on GitHub.
