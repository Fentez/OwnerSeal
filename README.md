# OwnerSeal

A Paper 1.21+ plugin that introduces a craftable **Owner's Seal** item to protect Filled Maps (map art) and Smithing Templates from unauthorized duplication.

**Author:** [Fentez](https://fentez.pp.ua) • **GitHub:** [Fentez/OwnerSeal](https://github.com/Fentez/OwnerSeal)

[English](#english) • [Українська](#українська)

---

## English

**OwnerSeal** allows players on survival and SMP servers to lock their custom Map Art (`FILLED_MAP`) and rare Smithing Templates (`*_SMITHING_TEMPLATE`) so that only the original owner can duplicate them.

### Key Features

* **Craftable Owner's Seal**: Players can craft the seal item (defaults to Honeycomb, Gold Ingot, and Stick) and automatically discover the recipe on join.
* **Drag & Drop Sealing**: Simply pick up the Owner's Seal in your inventory and **Left-Click** a Filled Map or Smithing Template to apply or remove your seal.
* **Full Anti-Copy Protection**: Blocks unauthorized cloning via Crafting Tables, Cartography Tables, and automated redstone **Crafters** (`CrafterCraftEvent`).
* **Custom Durability via Data Components**: Uses Paper's modern `DataComponentTypes` (`MAX_DAMAGE`, `DAMAGE`, `ITEM_MODEL`) for durability tracking on any item material.
* **MiniMessage & Custom Sounds**: Fully customizable item lore, messages, and sound effects.

### Commands & Permissions

| Command | Description | Permission | Default |
| --- | --- | --- | --- |
| `/ownerseal reload` | Reload the plugin configuration and crafting recipe | `ownerseal.admin` | `op` |
| `/ownerseal unseal` | Forcefully remove an Owner's Seal from the item in your main hand | `ownerseal.admin` | `op` |

### Build & Installation

```bash
mvn clean package
```

1. Requires **Paper 1.21+** and **Java 21+**.
2. Place `target/OwnerSeal-1.0.0.jar` into your server's `plugins/` folder.
3. Restart the server.

---

## Українська

**OwnerSeal** — це плагін для Paper 1.21+, який додає крафтовий предмет **Печатка власника** для захисту заповнених мап (мап-артів) та ковальських шаблонів від несанкціонованого копіювання іншими гравцями.

### Основні можливості

* **Крафтова печатка власника**: Гравці можуть створити печатку на верстаку (рецепт автоматично відкривається у книзі рецептів при вході).
* **Зручне використання в інвентарі**: Візьміть печатку курсором в інвентарі та натисніть **ЛКМ** по Карті або Шаблону, щоб накласти або зняти захист.
* **Повний захист від копіювання**: Блокує клонування чужих предметів у звичайному верстаку, столі картографа та автоматичному **Крафтері** (Crafter).
* **Міцність через Data Components**: Використовує сучасні компоненти предметів Paper 1.21 для відображення шкали міцності.
* **Підтримка MiniMessage та звуків**: Повне налаштування назви, опису предмета, звуків та повідомлень у `config.yml`.

### Команди та права

| Команда | Опис | Право | За замовчуванням |
| --- | --- | --- | --- |
| `/ownerseal reload` | Перезавантажити конфігурацію та рецепт крафту | `ownerseal.admin` | `op` |
| `/ownerseal unseal` | Примусово зняти печатку з предмета в основній руці | `ownerseal.admin` | `op` |
