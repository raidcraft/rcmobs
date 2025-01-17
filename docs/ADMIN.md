# RCMobs Admin Dokumentation

Die grundlegende Konfiguration von Mobs ist sehr einfach und in ein paar Minuten erledigt. Wenn man Mobs jedoch eigene Fähigkeiten geben will sollte man sich mit der [Konfiguration von Skills](https://git.faldoria.de/tof/plugins/raidcraft/rcskills/blob/master/docs/Skills.md) auskennen, da Mobs eine abgespeckte Version von Skills verwenden.

* [Basis Config](#basis-config)
  * [Minimal Config](#minimal-config)
  * [Schaden und Lebel Formeln](#schaden-und-lebel-formeln)
* [Abilities](#abilities)
* [Mob Gruppen](#mob-gruppen)

## Basis Config

In der Basis Config werden bis auf die Skills alle Eigenschaften des Mobs festgelegt. Einige der Werte werden je nach Einstellungen automatisch berechnet.

Die EXP die ein Mob gibt ist z.B. abhängig von seinem Level. Wenn man außerdem keine fixen Leben definiert wird das Leben des Mobs basierend auf dem Leben wie folgt berechnet:

```text
1.5126 * (level ^ 2) + 15.946 * level + 80
```

> Mob Configs müssen mit der Endung `.mob.yml` abgespeichert werden.

In der folgenden Config sind alle angegebenen Werte, bis auf wenige Ausnahmen, die Default Werte.

```yml
# Der Name wird über dem Mob und im Combatlog angezeigt.
# Der Name des Mobs darf maximal 15 Zeichen lang sein!
name: 'Schlächter'
# Der Bukkit EntityType des Mobs. Eine Liste von validen Entities findet man hier:
# https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html
type: SKELETON
# Kann nur bei Mobs gesetzt werden die auch in Vanilla Baby Mobs sein können.
baby: false
# Zählt nur für Wölfe und PigZombies
aggro: true
# Gibt an ab welcher Entfernung der Mob den Spieler angreift.
aggro-range: 8.0
# Die Loot-Tabelle bestimmt was für Items der Mob mit welcher Wahrscheinlichkeit droppt.
# Eine Standard Tabelle ist im Mob Plugin in der config.yml hinterlegt und kann hier
# für jeden Mob überschrieben werden.
# Einfach weglassen, wenn der Standard genutzt werden soll.
loot-table: meine-loottabelle
# Custom NMS Types funktionieren nur wenn die Mobs entsprechend ausprogrammiert wurden und
# in der globalen Config enable-nms-entities: true gesetzt wurde.
# Kann in den meisten Fällen einfach weggelassen werden.
# custom-type: RCDragon
# Hat aktuell keine Bedeutung
# spawn-naturally: false
# Funktioniert aktuell nicht und muss durch die Implementierung von NMS Mobs gefixt werden.
# Für einen Ranged Mob SKELETON als Typ auswählen und einen Bogen equipen
# Skeletons mit einem Schwert sind automatisch Nahkämpfer
# ranged: false
# Wenn der Mob zufällig in der Welt spawnt ist die Spawn Chance sehr gering.
# Ändert den DisplayNamen ab, sollte also auch in der Spawn Wahrscheinlichkeit wiedergespiegelt werden.
rare: false
# Ändert den DisplayName des Mobs.
# Leben und Schaden müssen manuell dafür angepasst werden.
elite: false
# Versteckt den Namen des Mobs.
hide-name: false
# Erlaubt dem Mob ins Wasser zu gehen
water: false
# Steuert ob der Mob den Spieler direkt angreift (false) oder erst angreift wenn er angegriffen wurde (true)
passive: false
# Wenn true, hebt der Mobs Items auf die am Boden liegen
item-pickup: false
# Bei jedem Spawn wird das Leben des Mobs zufällig zwischen den beiden Werten generiert.
# Für ein konstantes Leben einfach die gleichen Werte setzen.
# Wenn nichts gewählt wird ist das Leben des Mobs von seinem Level abhängig.
# Die Formel dafür ist: 1.5126 * (level ^ 2) + 15.946 * level + 80
min-health: 10
# Wenn nicht definiert: == min-health
max-health: 20
# Setzt das Leben des Mobs zurück sobald er den Kampf verlässt.
reset-health: true
# Bei jedem Schlag/Angriff macht der Mob zufällig in diesem Bereich Schaden.
# Für Kritische Treffer, oder die Chance auszuweichen müssen Custom Skills hinzugefügt werden.
min-damage: 5
# Wenn nicht definiert: == min-damage
max-damage: 8
# Wie bei den Leben wird bei jedem Spawn zufällig ein Level gewählt.
# Das Level hat einen Einfluss darauf wie viel Erfahrung der Mob bringt.
min-level: 3
# Wenn nicht definiert: == min-level
max-level: 5
# Alle Item IDs findet man auf: https://minecraft-ids.grahamedgecombe.com/
equipment:
    # Hier können auch Custom Items equiped werden.
    # Der Schaden von Custom Waffen wird verwendet!
    hand: AIR
    # Hier kann auch ein Custom Head verwendet werden, z.B.:
    # skull:Silthus für Köpfe mit Spielernamen
    # oder durch Verwendung der Seite https://minecraft-heads.com/ auch Custom Heads mit dem Base64 String
    # skull:eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQxY2I5ZTBhMDRhODRkZGE0ZTcxODhkYzE5MTVlY2JmNmZhYjlhNDAxZTUyNTFjNjYyMDI4N2MxZGZmYTc4NCJ9fX0=
    head: AIR
    chest: AIR
    legs: AIR
    boots: AIR
    # Hier könnte man die Items die der Mob trägt dann droppen lassen, z.B. Custom Waffen
    # Oder was am ehesten Verwendung findet: der Kopf des Mobs.
    hand-drop-chance: 0
    head-drop-chance: 0
    chest-drop-chance: 0
    legs-drop-chance: 0
    boots-drop-chance: 0
# Ermöglicht es eine Liste von Entities zu definieren die dieser Mob angreift.
# Es müssen die NMS Class Names der Entities angegeben werden.
# Nur für Profis! Eine grobe veraltete Liste gibt es hier: https://github.com/Bukkit/mc-dev/tree/master/net/minecraft/server
# targets:
#   - EntityPlayer
#   - EntityOcelot
#   - EntitySheep
# Eine Liste von allen Sounds gibt es auf: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html
sound:
    hurt: ENTITY_GENERIC_HURT
    death: ENTITY_GENERIC_DEATH
    hurt-pitch: 1.0
    death-pitch: 0.5
# Für die Konfiguration der Abilities bitte in der extra Dokumentation dafür nachsehen.
abilities:
    ...
```

### Minimal Config

Für einen gültigen Mob reicht bereits folgende minimal Config. Alles weitere wird automatisch basierend auf den [Formeln in der Plugin Config](../README.md#schaden-und-leben-formeln) berechnet.

```yml
name: Name des Mobs
min-level: 1
max-level: 5
```

### Schaden und Lebel Formeln

In jeder Mob Config ist es möglich die [Formeln für Schaden und Leben](../README.md#schaden-und-leben-formeln) aus der Default Config zu überschreiben.

```yml
name: Name des Mobs
min-level: 1
max-level: 5
# Hier kann man die default Formeln aus der config.yml für jeden Mob individuell setzen.
health-expression: "level * 2"
damage-expression: "level + 5"
```

## Abilities

Custom Mobs ohne Fähigkeiten wären keine Custom Mobs. Daher gibt es die Möglichkeit Mobs in abgespeckter Form analog Spielern Skills zu geben, welche dann automatisch während einem Kampf ausgeführt werden.

Der Mob wird versuchen alle seine Fähigkeiten so oft wie möglich auszuführen. Das Einzige was ihn daran hindert eine Fähigkeit auszuführen ist der `cooldown` der Fähigkeit.

> Auch Fähigkeiten nutzen die [Basis Config](https://git.faldoria.de/tof/plugins/raidcraft/rcskills/blob/master/docs/Skills.md#basis-config) aus dem Skills Plugin für Schaden, Cooldown, uvm.

Aktuell gibt es noch keine direkte Liste mit Fähigkeiten. Alle existierenden Fähigkeiten befinden sich im [Source Code](https://git.faldoria.de/raidcraft/rcmob-abilities/tree/master/src/main/java/de/raidcraft/mobs/abilities).

```yml
abilities:
  # Anstatt einem eigenen Namen kann hier auch der eindeutige Name der Fähigkeit stehen.
  # Siehe das Beispiel, kockback-resistance
  kopfnuss:
    # Der Name wird dem Spieler im CombatLog angezeigt.
    name: Kopfnuss
    # Der eindeutige Name der Fähigkeit, siehe Fähigkeiten Liste
    ability: strike
    # Alles in der Custom Sektion wird von der Fähigkeit direkt konfiguriert.
    custom:
      stun: true
    # Siehe dazu die Erklärung im Skills Plugin zu der "Basis Config" (Link oben)
    damage:
      base: 55
    cooldown:
      base: 20
      level-modifier: 2.5
    # Wie bei den Skills können Fähigkeiten Effekte auslösen.
    # Deren Details werden dann in diesem Bereich konfiguriert.
    effects:
      # Hier muss der eindeutige Name des Effects stehen.
      stun:
        # Dieser Name wird in der Seitenleiste des Spielers angezeigt.
        name: Kopfnuss
        # Die Dauer in Sekunden, siehe Basis Config
        duration:
          base: 4
  blutung:
    name: Nierenschlag
    ability: strike
    custom:
      bleed: true
    damage:
      base: 11
    cooldown:
      base: 24
    effects:
      bleed:
        name: Nierenschlag
        duration:
          base: 12
        interval:
          base: 3
        damage:
          base: 17
  wutanfall:
    name: Wutanfall
    ability: enrage
    custom:
      treshhold: 0.3
      attack-increase:
        base: 1.0
    effects:
      enrage:
        name: Wutanfall
        duration:
          base: 30
        interval:
          base: 1
        # Fähigkeiten und Effekte können wir Skills mit Visuellen Effekten verstärkt werden.
        # Siehe dazu die Erklärung im Skills Tutorial.
        visual-effects:
          APPLY:
            '1':
              type: bukkit
              effect: MOBSPAWNER_FLAMES
            '2':
              type: sound
              effect: WOLF_GROWL
          TICK:
            '1':
              type: bukkit
              effect: MOBSPAWNER_FLAMES
  knockback-resistance:
    custom:
      chance:
        base: 0.75
```

## Mob Gruppen

Mobs können zu Gruppen zusammengefasst werden um das Spawn und Respawn Verhalten zu beinflussen. Außerdem ist es leichter Spawnpunkte für eine Gruppe für Mobs zu setzen, anstatt für jeden Mob einzeln.

In Quests hat man zusätzlich die Möglichkeit mit einer [Action](https://git.faldoria.de/tof/plugins/raidcraft/raidcraft-api/blob/master/docs/ART-API.md#actions) eine ganze Mob Gruppe auf einmal zu spawnen. Man kann dann mit einem [Requirement](https://git.faldoria.de/tof/plugins/raidcraft/raidcraft-api/blob/master/docs/ART-API.md#requirements) abfragen ob der Spieler die gesamte Mob Gruppe getötet hat.

Eine Mob Gruppe befindet sich gemeinsam in einer Gruppe, d.h. Heilungsfähigkeiten der Mobs betreffen die eigene Gruppe, genauso wie das Betreten und Verlassen eines Kampfes.

> Mob Gruppen müssen mit der Dateiendung `.group.yml` abgespeichert werden.

```yml
# Spawn Intervall der Gruppe in Ticks
# Sind alle tot dauert es einen zufälligen Wert zwischen min und max bis die Gruppe wieder spawnt.
# Gruppen die manuell über Actions gespawnt werden ignorieren das Spawn Intervall.
# Wird das Intervall in der Config aktualisiert greift der Wert erst nach einem Reload
# UND nachdem die Gruppe das nächste Mal gespawnt ist (mit dem alten Intervall).
min-interval: 60
max-interval: 180
# Ein Zufälliger Wert wieviele Mobs aus der Gruppe spawnen.
min-amount: 1
max-amount: 1
# Die Mobs spawnen in diesem Radius um den gesetzten Spawnpunkt.
# Kann auf 0 gesetzt werden um alle Mobs am gleichen Ort spawnen zu lassen.
spawn-radis: 5
# Sobald die Gruppe nur noch aus dieser Anzahl Mobs besteht
# können neue Mobs im vorgegebenen Intervall gespawnt werden.
respawn-treshhold: 0
mobs:
    # Liste von beliebigen Namen, die ID der Mobs wird unten definiert
    mob1:
        # Pfade werden mit Punkten getrennt
        mob: 'mobs.mein-mob-ordner.supermob'
        # Die Spawn Chance des Mobs in %
        # 1.0 == 100%
        chance: 1.0
    
    zweiter-mob:
        # this. referenziert den aktuellen Pfad.
        # Der Name MUSS unbedingt in Anführungsstrichen stehen
        mob: 'this.mein-mob'
        chance: 1.0
```