# 📚 Reference Java examples (the `EXAMPLES/` folder)

This folder contains **complete reference Java items** for learning the Java API.

## ⚠️ This folder is not loaded

`EXAMPLES/` — reference material: it is **not copied** to the server
(see `DefaultContentExtractor`) and **not compiled** by the runtime compiler
(see `ItemRegistry`). The same applies to the `_template/` folder.

## 📋 The `EXAMPLE-*` sample rule

Files with the `EXAMPLE-*` prefix in the root of `items/` are samples: on the first
launch the plugin copies them into `plugins/Artifex/items/` as reference
material, but the compiler and the loaders ignore them.

## ▶️ How to enable an example

1. Copy the needed file from this folder (or from `items/` in the jar)
   into `plugins/Artifex/items/`.
2. Remove the `EXAMPLE-` prefix from the file name (or the folder).
3. Run `/ci reload`.
4. Check: `api-item list` → the item ID should appear.
5. Give it: `/api-item give <id>`.

> The public class name must match the file name
> (the runtime compiler matches the class to the file name, but it is better
> not to rename it).

## 🧭 What is inside

| File | Demonstrates |
|------|---------------|
| `DragonboneArmor.java` | A complete armor item: `getRecipes()` (shaped+shapeless, material ingredients and custom IDs), `getActivationSlot`/`getType`, the `onEquip`/`onUnequip`/`onPeriodic` hooks, the combat `onDamageTaken`, the `ItemAPI` helpers |
| `Frostblade.java` | Complete Java weapon: the click hooks `onRightClick`/`onLeftClick`, `getClickCooldown()` (shared by both clicks), the combat `onDamageDealt`/`onKill`, the crafting `RecipeDef.shaped`, damage/AoE/effects through `ItemAPI` |

See also: `EXAMPLE-dark-sword.java` and `EXAMPLE-royal-helmet.java` (root
`items/`) — short weapon and armor samples.
