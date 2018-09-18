[![pipeline status](https://git.faldoria.de/raidcraft/rcmobs/badges/master/pipeline.svg)](https://git.faldoria.de/raidcraft/rcmobs/commits/master)

# RCMobs

Das RCMobs Plugin ermöglicht das Spawnen von Custom Mobs und Mob Gruppen. Custom Mobs können unter Verwendung des [RCSkill Plugins](https://git.faldoria.de/raidcraft/rcskills) eigene Zauber und Fähigkeiten besitzen.

- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
- [Authors](#authors)

## Getting Started

* [Project Details](https://git.faldoria.de/raidcraft/rcmobs)
* [Source Code](https://git.faldoria.de/raidcraft/rcmobs/tree/master)
* [Latest Stable Download](https://ci.faldoria.de/view/RaidCraft/job/RCMobs/lastStableBuild)
* [Issue Tracker](https://git.faldoria.de/raidcraft/rcmobs/issues)
* [Developer Documentation](docs/DEVELOPER.md)
* [Admin Documentation](docs/ADMIN.md)

### Prerequisites

Das `RCMobs` Plugin ist von der [RaidCraft API](https://git.faldoria.de/raidcraft/raidcraft-api) und dem  [RCSkill](https://git.faldoria.de/raidcraft/rcskills) Plugin abhängig und benötigt eine Verbindung zu einer MySQL Datenbank um die Spawn Punkte der Mobs zu speichern.

Optional: [RCLoot](https://git.faldoria.de/raidcraft/rcloot) zum Verknüpfen von Loot-Tabellen die Mobs nach ihrem Tod droppen.

### Installation

Beim ersten Start des Servers wird eine `database.yml` und eine `config.yml` angelegt. Am besten den Server direkt nochmal stoppen und die Angaben in der `database.yml` bearbeiten.

Die `config.yml` enthält folgende defaults:

```yml
# Aktiviert eigens programmierte NMS Entities mit extra Fähigkeiten
enable-nms-entities: false
default:
  # Verhindert das Spawnen von Pferden
  deny-horse-spawning: true
  # Verhindert das Spawnen von weiteren Mobs in dem festgelegten Radius um bestehende Mobs
  # -1 bedeutet deaktiviert
  spawn-deny-radius: -1
  # Interval wie oft versucht wird Mobs zu spawnen
  task-interval: 1.0
  # Reichtweite in Blocks bis ein Mob zu seinem Spawnpunkt teleportiert wird
  reset-range: 50
  # Ersetzt Vanilla Mobs mit Custom Mobs
  replace-hostile-mobs: true
  # Ersetzt Vanilla Tiere mit Custom Mobs
  replace-animals: false
  # Definiert in welchem Radius um bereits gespawnte Mobs ähnliche Mobs spawnen
  natural-spawning-adapt-radius: 25
  # Folgende Mobs werden ersetzt wenn replace-* true ist.
  replaced-mobs:
  - BLAZE
  - ZOMBIE
  - SKELETON
  - CREEPER
  - SPIDER
  - GIANT
  - SLIME
  - GHAST
  - PIG_ZOMBIE
  - ENDERMAN
  - CAVE_SPIDER
  - SILVERFISH
  - MAGMA_CUBE
  - WITCH
  - IRON_GOLEM
  # Spawnt ähnliche Mobs in der Nähe von bereits bestehenden Mobs
  spawn-similiar-random-mobs: false
  # Die Default Loot-Table für alle Mobs die keine Loot-Tabelle definiert habe
  loot-table: mobs.default-loottable
  # Folgende Entities werden nicht ersetzt
  ignored-entities:
  - ARMOR_STAND
  # Entity Spawns mit folgendem Grund werden nicht ersetzt.
  ignored-spawn-reasons:
  # Custom wird unbedingt benötigt, da sonst NPCs ersetzt werden
  - CUSTOM
  # Verhindert das Teilen von Slimes
  prevent-slime-splitting: true
  # Folgende Mobs werden nicht ersetzt und nicht gespawnt
  denied-entity-types:
  - IRON_GOLEM
# Folgende Werte haben sich bewährt und sollten eigentlich nicht geändert werden
respawn-task:
  interval: 1.0
  mob-batch-count: 5
  mob-group-batch-count: 5
  remove-entity-on-chunk-unload: true
  cleanup-interval: 30.0
  cleanup-removed-characters: true
  delay: 10.0
debug:
  mob-spawning: false
  fixed-spawn-locations: false
  vanilla-spawning: false
```

## Authors

* [**Michael (Silthus) Reichenbach**](https://git.faldoria.de/Silthus)
