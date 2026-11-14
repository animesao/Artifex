# ARTIFEX — СИСТЕМА RESOURCE FORGE
**Проект:** Minecraft Fantasy RPG Survival SMP  
**Версия плагина:** Artifex v2.2.0  
**Версия сервера:** Paper 26.3 (Build 249)  
**Клиентский лаунчер:** ElyPrismLauncher 26.3  
**Статус готовности:** Фазы A – H полностью завершены

---

## 1. НАВИГАЦИЯ ПО ДОКУМЕНТАЦИИ

Комплекс документов архитектуры и каталогов доступен в каталоге `docs/resources/`:

1. [RESOURCE_SYSTEM_AUDIT.md](file:///d:/Artifex/docs/resources/RESOURCE_SYSTEM_AUDIT.md) — Полный технический аудит окружения, разбор логов CustomCraft, диагностика команд перезагрузки и существующих предметов.
2. [CUSTOM_BLOCKS_ARCHITECTURE.md](file:///d:/Artifex/docs/resources/CUSTOM_BLOCKS_ARCHITECTURE.md) — Архитектура кастомных блоков без модов на чистом Paper 26.3 через матрицу состояний Note Block (800 вариантов, 0 лагов, идеальная коллизия).
3. [RESOURCE_MASTER_CATALOG.md](file:///d:/Artifex/docs/resources/RESOURCE_MASTER_CATALOG.md) — Мастер-каталог из **150 уникальных материалов** по 9 RPG-ветвям (Огненная, Ледяная, Грозовая, Пустотная, Древняя, Кристаллическая, Природная, Алхимическая, Босс-материалы).
4. [IRIS_WORLDGEN_INTEGRATION.md](file:///d:/Artifex/docs/resources/IRIS_WORLDGEN_INTEGRATION.md) — Интеграция генерации жил руд в кастомные биомы Iris Dimension Engine v4.3.8.

---

## 2. РЕЗУЛЬТАТ ПЕРВОГО ПРОТОТИПА (PHASE E)

Внедрен и протестирован первый рабочий тир из **10 кастомных ресурсов** и **5 кастомных блоков**:

### 10 Кастомных Ресурсов (с CustomModelData и PDC):
1. `ember_ore_raw` (CMD: 2001) — Необработанная руда пламени
2. `ember_shard` (CMD: 2002) — Осколок пламени
3. `ember_ingot` (CMD: 2003) — Слиток пламени
4. `frost_ore_raw` (CMD: 2019) — Необработанная морозная руда
5. `frost_crystal_shard` (CMD: 2020) — Осколок морозного кристалла
6. `frost_ingot` (CMD: 2021) — Морозный слиток
7. `storm_shard` (CMD: 2037) — Осколок бури
8. `storm_ingot` (CMD: 2038) — Грозовой слиток
9. `void_shard` (CMD: 2053) — Осколок пустоты
10. `void_ingot` (CMD: 2054) — Слиток пустоты

### 5 Кастомных Блоков (Note Block Matrix + BlockState):
1. `ember_ore` (CMD: 2201, `iron_xylophone:0:false`) — Руда Пламени
2. `frost_ore` (CMD: 2202, `iron_xylophone:1:false`) — Морозная Руда
3. `storm_ore` (CMD: 2203, `iron_xylophone:2:false`) — Грозовая Руда
4. `void_ore` (CMD: 2204, `iron_xylophone:3:false`) — Глубинная Руда Пустоты
5. `ember_block` (CMD: 2205, `guitar:0:false`) — Блок Слитков Пламени

---

## 3. ГРАФИКА И 3D-МОДЕЛИРОВАНИЕ

- **Текстуры:** Созданы 15 высокодетализированных текстур 64x64 с многоуровневыми палитрами, бликами и объемными жилами в `assets/artifex/textures/`.
- **Blockbench 3D (.bbmodel):** Созданы 13 исходников проектов в `D:\server\Artifex-ResourcePack\blockbench_models/`. Все модели валидированы скриптом `model_validator.py` на 100% соответствие стандарту геометрии Minecraft Java Edition (оси, углы -45..45, uv mapping).
- **Синхронизация:** Ресурс-пак автоматически развернут как на сервере (`D:\server\Artifex-ResourcePack`), так и в клиентском лаунчере (`ElyPrismLauncher 26.3`).

---

## 4. ЧЕКЛИСТ ПРОВЕРКИ В ИГРЕ

1. **Перезагрузка плагина:**
   ```
   /artifex reload  (или /ci reload)
   ```
   *Ожидаемый результат:* Красивый русскоязычный отчёт с цветными HEX-градиентами без сырых кодов `&#279FF5`.

2. **Выдача ресурсов прототипа:**
   ```
   /give @p ember_ingot
   /give @p ember_shard
   /give @p frost_ingot
   /give @p storm_ingot
   /give @p void_ingot
   ```

3. **Выдача и установка кастомных блоков:**
   ```
   /give @p ember_ore
   /give @p frost_ore
   /give @p storm_ore
   /give @p void_ore
   /give @p ember_block
   ```
   *Проверка:*
   - Поставьте блок на землю — он отображается с кастомной 64x64 текстурой.
   - ПКМ по блоку — нота не переключается и звук пианино не звучит (CustomBlockManager перехватывает клик).
   - Сломайте блок железной/алмазной киркой — выпадают кастомные осколки с начислением опыта и учетом зачарования Fortune.
   - Сломайте киркой с Шелковым касанием — выпадает сам кастомный блок.

4. **Проверка крафтов в верстаке:**
   - 9 $\times$ `ember_shard` $\rightarrow$ 1 $\times$ `ember_ingot`.
   - 9 $\times$ `ember_ingot` $\rightarrow$ 1 $\times$ `ember_block`.
   - 1 $\times$ `ember_block` $\rightarrow$ 9 $\times$ `ember_ingot`.
   - Переплавка `ember_ore_raw` в печи $\rightarrow$ `ember_ingot`.
