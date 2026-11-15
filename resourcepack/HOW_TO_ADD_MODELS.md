# 🛠 Инструкция: Как добавить новую 3D модель в ресурс-пак и плагин

Добавление новой кастомной модели занимает 4 простых шага.

---

### 📌 Шаг 1. Создание модели в Blockbench
1. Откройте **Blockbench**.
2. Выберите тип проекта: **Minecraft Java Block/Item**.
3. Создайте геометрию:
   * Используйте углы поворота только `-45°`, `-22.5°`, `0°`, `22.5°`, `45°` по одной оси.
   * Настройте толщину клинка / рукояти в пределах `0.5 .. 2.0` пикселей.
4. Нарисуйте пиксель-арт текстуру размером `16x16`, `32x32` или `64x64`.
5. Во вкладке **Display** настройте хват в руках (`Thirdperson`, `Firstperson`, `GUI`, `Ground`).
6. Сохраните проект: **File ➔ Save Project** (`.bbmodel`).
7. Экспортируйте JSON: **File ➔ Export ➔ Export Block/Item Model** (`.json`).
8. Экспортируйте текстуру: сохраните текстуру как `.png`.

---

### 📌 Шаг 2. Размещение файлов в ресурс-паке по категориям
Допустим, вы создали огненный меч `flame_blade`:

1. **Исходник Blockbench** положите в:
   `blockbench_models/weapons/flame_blade.bbmodel`

2. **JSON модель** положите в:
   `assets/artifex/models/item/weapons/flame_blade.json`
   *(и для удобства продублируйте в `assets/artifex/models/item/flame_blade.json`)*

3. **Текстуру** положите в:
   `assets/artifex/textures/item/flame_blade.png`
   *(убедитесь, что внутри вашего `flame_blade.json` текстура прописана как `"0": "artifex:item/flame_blade"`)*

4. **Item Model 1.21.4+** создайте файл `assets/artifex/items/flame_blade.json`:
   ```json
   {
     "model": {
       "type": "minecraft:model",
       "model": "artifex:item/flame_blade"
     }
   }
   ```

---

### 📌 Шаг 3. Регистрация в ванильном перехвате (Overrides)
Если предмет базируется, например, на незеритовом мече (`Material.NETHERITE_SWORD`), добавьте свободный `CMD` (например, `1003`):

В `assets/minecraft/items/netherite_sword.json` добавьте в `entries`:
```json
{
  "threshold": 1003,
  "model": {
    "type": "minecraft:model",
    "model": "artifex:item/flame_blade"
  }
}
```

В `assets/minecraft/models/item/netherite_sword.json` добавьте в `overrides`:
```json
{
  "predicate": { "custom_model_data": 1003 },
  "model": "artifex:item/flame_blade"
}
```

---

### 📌 Шаг 4. Регистрация в плагине Artifex (Java-класс)
В папке `plugins/Artifex/items/` создайте `FlameBlade.java`:

```java
import me.dcplugin.artifex.api.AbstractCustomItem;
import org.bukkit.Material;
import java.util.List;

public class FlameBlade extends AbstractCustomItem {
    @Override public String getId() { return "flame_blade"; }
    @Override public String getDisplayName() { return "&#FF5555&lОГНЕННЫЙ КЛИНОК"; }
    @Override public Material getMaterial() { return Material.NETHERITE_SWORD; }
    @Override public int getCustomModelData() { return 1003; }
    @Override public String getItemModel() { return "artifex:item/flame_blade"; }
    @Override public boolean isUnbreakable() { return true; }
    @Override public List<String> getLore() {
        return List.of("&8Редкое оружие", "&c⚔ Урон: &f+10.0");
    }
}
```

После этого напишите **/ci** на сервере и нажмите **F3 + T** в клиенте!
