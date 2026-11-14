# ИНТЕГРАЦИЯ С ГЕНЕРАЦИЕЙ МИРА IRIS DIMENSION ENGINE
**Модуль:** Iris Worldgen & Ore Injector  
**Версия Iris:** 4.3.8 `[CraftBukkit] 26.1.2-26.3.jar`  
**Мировой пак:** `overworld v4022`  
**Кастомные руды:** Note Block Matrix Carrier (NBM)

---

## 1. СТРУКТУРА ПАКА IRIS И ТОЧКИ ВНЕДРЕНИЯ

Iris Dimension Engine поддерживает кастомные `features` (особенности мира), включая генераторы жил полезных ископаемых (`ore` / `scattered_ore`).

Путь к конфигурациям активного пака:
`D:\server\plugins\Iris\packs\overworld\`

Кастомная руда Artifex генерируется через состояние ванильного блока:
`minecraft:note_block[instrument=<instrument>,note=<note>,powered=<powered>]`

Благодаря этому Iris воспринимает кастомную руду как стандартный блок Minecraft со специфическим состоянием BlockData, не требуя сторонних модов на клиенте!

---

## 2. КАРТА РАСПРЕДЕЛЕНИЯ КАСТОМНЫХ РУД В БИОМАХ IRIS

| Руда / Блок | Состояние BlockData | Высота (Y) | Биомы Iris | Размер жилы | Жилы / чанк | Заменяемый блок |
|---|---|---|---|---|---|---|
| **Руда Пламени (`ember_ore`)** | `instrument=iron_xylophone,note=0,powered=false` | Y: 0..64 | `hot`, `volcanic`, `mesa`, `desert` | 4–8 | 8 | `minecraft:stone` |
| **Глубинная Руда Пламени** | `instrument=iron_xylophone,note=0,powered=true` | Y: -64..0 | `hot`, `volcanic` (глубокие пещеры) | 3–6 | 5 | `minecraft:deepslate` |
| **Морозная Руда (`frost_ore`)** | `instrument=iron_xylophone,note=1,powered=false` | Y: 60..240 | `frozen`, `tundra`, `glacier`, `peaks` | 4–8 | 7 | `minecraft:stone`, `packed_ice` |
| **Грозовая Руда (`storm_ore`)** | `instrument=iron_xylophone,note=2,powered=false` | Y: 120..280 | `magnetics`, `windswept_hills`, `peaks` | 3–6 | 6 | `minecraft:stone` |
| **Глубинная Руда Пустоты (`void_ore`)** | `instrument=iron_xylophone,note=3,powered=false` | Y: -64..-30 | `terralost`, `estranged`, `deep_caves` | 2–4 | 3 | `minecraft:deepslate` |
| **Рубиновая Жила (`ruby_ore`)** | `instrument=bell,note=0,powered=false` | Y: -32..32 | Все умеренные биомы (`temperate`, `forests`)| 2–4 | 4 | `minecraft:stone`, `deepslate` |
| **Сапфировая Жила (`sapphire_ore`)** | `instrument=bell,note=1,powered=false` | Y: 40..180 | `temperate`, `mountain`, `highlands` | 2–5 | 4 | `minecraft:stone` |
| **Адамантитовая Жила (`adamantite_ore`)**| `instrument=bell,note=2,powered=false` | Y: -64..-45 | Редкие жилы близ бедрока | 1–3 | 2 | `minecraft:deepslate` |

---

## 3. ФОРМАТ КОНФИГУРАЦИОННОГО ФАЙЛА IRIS FEATURE

Пример файла описания жилы: `packs/overworld/features/ores/ember_ore.json`:
```json
{
  "type": "iris:ore",
  "block": "minecraft:note_block[instrument=iron_xylophone,note=0,powered=false]",
  "target": [
    "minecraft:stone",
    "minecraft:granite",
    "minecraft:diorite",
    "minecraft:andesite"
  ],
  "size": 7,
  "count": 8,
  "distribution": {
    "type": "uniform",
    "min": 0,
    "max": 64
  }
}
```

Для глубинных руд (в сланце) — `packs/overworld/features/ores/void_ore.json`:
```json
{
  "type": "iris:ore",
  "block": "minecraft:note_block[instrument=iron_xylophone,note=3,powered=false]",
  "target": [
    "minecraft:deepslate",
    "minecraft:tuff"
  ],
  "size": 3,
  "count": 3,
  "distribution": {
    "type": "triangle",
    "min": -64,
    "max": -20
  }
}
```

---

## 4. ДВУХУРОВНЕВЫЙ МЕХАНИЗМ ГЕНЕРАЦИИ (FAIL-SAFE ARCHITECTURE)

Чтобы генерация не зависела исключительно от сторонних обновлений паков Iris, в Artifex реализован двухуровневый генератор:
1. **Основной уровень (Iris Native):** Iris генерирует рудные жилы прямо во время асинхронного пайплайна генерации мира (максимальная скорость, 0 задержек тика сервера).
2. **Резервный декоратор (Artifex Chunk Populator):** В мирах, где пак Iris не настроен или в случае генерации стандартных чанков (например, обычный Nether или End), Artifex слушает генерацию чанков и по псевдослучайной сетке сида мира подмешивает кастомные жилы в породу.
