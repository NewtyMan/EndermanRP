# EndermanRP (v0.1)

**EndermanRP** is a Minecraft plugin that provides players with many features that enhance roleplay as an enderman. It aims to include vast variety of different abilities and effects in order to suit the need of any server (every ability/effect can be **enabled | disabled** through config.yml).

***Supported Server Version: 1.13 - 1.16.x***
&nbsp;

## Download & Install

Download the latest plugin release and drag it into your **plugins/** server folder. Make sure to delete any old version of this plugin first.

[EndermanRP v.01](https://github.com/NewtyMan/EndermanRP/releases/download/0.1/endermanrp-0.1.jar)
&nbsp;
## Plugin features & TO-DO list

**Features:**
 - Players with carved pumpkin helmets become invisible
 - Enderman mobs are friendly with enderman players
 - Teleportation (hold shift for 3s)
 - Damage while in water or on rain
 - Silk touch (only with bare hands)

&nbsp;
**TO-DO:**
 - TO-DO requests opened
&nbsp;
## Config.yml

    # Features toggle  
    enable-pumpkin-invisibility: true  
    enable-friendly-enderman: true  
    enable-teleportation: true  
    enable-water-damage: true  
    enable-silk-touch: true  
      
    # Water damage settings  
    water-per-second-damage: 2  
    require-full-armor: false  
      
    # Teleportation settings  
    max-height-difference: 6  
    teleport-distance: 24

***enable-pumpkin-invisibility =>*** players that wear carved pumpkin will be invisible to enderman players

***enable-friendly-enderman =>*** enderman mobs won't attack player if they look at them or attack them

***enable-teleportation =>*** enderman players are able to teleport (additional configuration under *# Teleportration settings*)

***enable-water-damage =>*** enderman players will take damage upon being in water or on rain (additional configuration under *#Water damage settings*)

***enable-silk-touch =>*** enderman players will have automatic silk touch when harvesting with bare hands

---
***water-per-second-damage =>*** 1 damage = 0.5 hearts

***require-full-armor =>*** does enderman player require full armor to prevent water damage or only one piece

---
***max-height-difference =>*** maximum height difference between current location and final teleport location

***teleport-distance =>*** maximum teleport distance in blocks
&nbsp;
## Commands & Permissions

**Syntax:**

*/em on|off (player_name) => toggles enderman status on player*

*/s => plays enderman sound*

**Example:**

*/em on NewtyMan*

*/em off NewtyMan*

**Permissions:**

*enderman.set =>* gives permission to /em command
