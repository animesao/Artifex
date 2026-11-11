# Модули и GUI — русский справочник

## 1. Что такое модуль

Модуль — отдельная функция внутри Artifex. Он может иметь свои Java-файлы, `config.yml`, `items.yml`, команды и меню.

Пример структуры:

```text
plugins/Artifex/items/my-feature/
├── config.yml
├── items.yml
├── my-feature.java
├── feature-command.java
└── menus/
    └── main.yml
```

Java-модуль должен наследовать `me.dcplugin.artifex.api.modules.Module` и иметь конструктор:

```java
public MyFeatureModule(Main plugin, String id, File folder) {
    super(plugin, id, folder);
}
```

Минимальные методы:

```java
@Override
protected void onEnable() {
    // включение механики
}

@Override
protected void onDisable() {
    // отмена задач и очистка ресурсов
}
```

В `onDisable()` обязательно отменяйте повторяющиеся задачи и освобождайте слушатели/ресурсы.

## 2. Конфигурация модуля

```yaml
name: 'Мой модуль'
version: '1.0'
enabled: true
commands:
  - myfeature
permissions:
  - myfeature.use
```

`config.yml` и `items.yml` загружаются модулем как YAML. `items.yml` может содержать раздел `items`, доступный через методы `getItem()` и `getAllItemIds()` базового класса.

После изменения файлов выполните:

```text
/ci reload
```

## 3. DeluxeMenuX

Готовый модуль в jar/repository лежит как образец `EXAMPLE-deluxemenux/`.
Включите его: скопируйте папку в `plugins/Artifex/items/` и уберите
префикс `EXAMPLE-` из имени (`EXAMPLE-deluxemenux/` → `deluxemenux/`),
затем `/ci reload`. После этого модуль находится в:

```text
plugins/Artifex/items/deluxemenux/
├── config.yml
├── deluxemenux.java
├── menu-command.java
├── kits-command.java
├── shop-command.java
└── menus/
    ├── main.yml
    ├── kits.yml
    └── shop.yml
```

При загрузке модуль ищет YAML в `menus/` и создаёт стандартные `main.yml`, `kits.yml` и `shop.yml`, если их нет. Существующие файлы не перезаписываются.

Доступные команды:

```text
/menu
/menu <id>
/menu list
/menu reload
/deluxemenu
/kits
/kit
/shop
```

Команды `/kits`, `/kit` и `/shop` открывают соответствующие меню. `/menu` без аргумента открывает `main`.

## 4. Формат меню

Базовая конфигурация:

```yaml
title: '&8Магазин'
size: 27
open-sound: 'BLOCK_CHEST_OPEN'
close-sound: 'BLOCK_CHEST_CLOSE'
fill:
  material: BLACK_STAINED_GLASS_PANE
  name: ' '
items:
  13:
    material: DIAMOND
    name: '&bАлмаз'
    lore:
      - '&7Нажмите, чтобы получить'
    permission: 'deluxemenux.shop.diamond'
    command: 'give %player% diamond 1'
    message: '&aАлмаз выдан!'
    sound: 'ENTITY_PLAYER_LEVELUP'
    close: true
```

Правила:

- `size` обычно равен `9`, `18`, `27`, `36`, `45` или `54`;
- слоты начинаются с `0`, поэтому в меню на 27 слотов последний слот — `26`;
- `material` должен быть валидным Bukkit Material;
- `permission` проверяется при нажатии;
- `command` выполняется от имени консоли;
- `%player%` заменяется именем игрока;
- `message` отправляется игроку;
- `close: true` закрывает меню после нажатия;
- текст цены в lore сам по себе не списывает валюту.

Текущая реализация читает `close-sound`, но не гарантирует проигрывание звука при каждом закрытии. Реальную экономику нужно подключить отдельно или написать Java-механику.

## 5. Как добавить своё меню

1. Создайте `plugins/Artifex/items/deluxemenux/menus/quests.yml`.
2. Добавьте настройки `title`, `size` и `items`.
3. Выполните `/ci reload`.
4. Откройте `/menu quests`.

Если меню не открылось, проверьте имя файла, YAML-отступы, размер инвентаря и консоль.

## 6. Как добавить кнопку с командой

```yaml
items:
  11:
    material: EMERALD
    name: '&aТелепорт'
    permission: 'myserver.warp.spawn'
    command: 'warp spawn %player%'
    close: true
```

Команда выполняется с правами консоли. Это удобно, но опасно: не позволяйте обычным игрокам редактировать YAML меню.

## 7. Перезагрузка и обновление

После изменения Java, YAML или меню:

```text
/ci reload
```

Для обновления самого JAR нужен полный перезапуск сервера. Перед изменениями сохраните резервную копию папки `items/`.
