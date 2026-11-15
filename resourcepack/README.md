# ⚔ ARTIFEX — RESOURCE PACK

Официальный модульный ресурс-пак проекта **Artifex** с поддержкой Minecraft Java 1.20.5 .. 1.21.4+ (Paper 26.3).

## 📁 Архитектура и категории ресурс-пака:

```
Artifex-ResourcePack/
├── pack.mcmeta                       # Конфигурация и версия ресурс-пака
├── pack.png                          # Официальная иконка ресурс-пака в меню
├── README.md                         # Данный файл: описание архитектуры и категорий
├── HOW_TO_ADD_MODELS.md              # Пошаговая инструкция: как добавить новую 3D-модель
│
├── blockbench_models/                # Исходники моделей (.bbmodel для Blockbench)
│   ├── weapons/                      # ⚔ Оружие (катаны, мечи, кинжалы, секиры, молоты)
│   │   └── katana/                   #   Самурайская катана (drawn и sheathed)
│   ├── armor/                        # 🛡 Броня (шлемы, короны, нагрудники)
│   ├── tools/                        # ⛏ Инструменты (буры, кирки, топоры)
│   ├── materials/                    # 💎 Материалы (слитки, руды, самоцветы)
│   ├── relics/                       # 💍 Реликвии (кольца, амулеты, свитки)
│   └── blocks/                       # 🧱 Кастомные блоки и руды
│
└── assets/
    ├── artifex/                      # Пространство имен плагина Artifex
    │   ├── items/                    # 1.21.4+ Item-model декларации
    │   │   ├── katana_drawn.json
    │   │   ├── katana_sheathed.json
    │   │   └── weapons/
    │   ├── models/item/              # 3D JSON-геометрия моделей Minecraft
    │   │   ├── katana_drawn.json
    │   │   ├── katana_sheathed.json
    │   │   └── weapons/
    │   └── textures/item/            # 64x64 текстурные атласы
    │       ├── katana_drawn.png
    │       ├── katana_sheathed.png
    │       └── weapons/
    │
    └── minecraft/                    # Ванильные перехваты
        ├── items/netherite_sword.json # Перехват 1.21.4+ range_dispatch
        └── models/item/netherite_sword.json # Перехват Legacy CustomModelData
```

## 🎯 Текущие подключенные предметы:
1. **Катана Самурая (Обнаженная)**: `CMD: 1001`, `Model: artifex:item/katana_drawn`
2. **Катана Самурая (В ножнах)**: `CMD: 1002`, `Model: artifex:item/katana_sheathed`
