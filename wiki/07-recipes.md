# 07 — Рецепты

## Что ты узнаешь

- Где живут рецепты и почему они описаны **внутри** предмета.
- Все пять типов рецептов в YAML: `shaped`, `shapeless`, `furnace`, `smithing`, `stonecutting` — с точным форматом каждого ключа.
- Как записать ингредиент: имя материала (`DIAMOND`) или id другого своего предмета (`vampire-blade`).
- Как написать те же рецепты на Java через `RecipeDef`.
- Что делают параметры `amount`, `experience` и `cooking-time`.
- Где рецепты работают: в обычном верстаке и в GUI-модуле `/craft`.
- Как изменить или удалить рецепт и что происходит при `/ci reload`.
- Типичные ошибки и то, как о них сообщает консоль.

## Предусловия

- Плагин установлен, предметы созданы — [Предметы (YAML)](03-yaml-items.md) или [Предметы (Java)](04-java-items.md).
- Вы умеете писать YAML — [Как это работает](02-how-it-works.md).
- Для Java-части — базовые знания Java 21 и [Предметы (Java)](04-java-items.md).
- Общий список страниц — [оглавление](README.md).

## Содержание

- [Что такое рецепт](#что-такое-рецепт)
- [Рецепты в YAML](#рецепты-в-yaml)
  - [Формульный рецепт shaped](#формульный-рецепт-shaped)
  - [Рецепт без формулы shapeless](#рецепт-без-формулы-shapeless)
  - [Рецепт печи furnace](#рецепт-печи-furnace)
  - [Рецепт кузнеца smithing](#рецепт-кузнеца-smithing)
  - [Рецепт камнереза stonecutting](#рецепт-камнереза-stonecutting)
- [Ингредиенты](#ингредиенты)
- [Рецепты на Java](#рецепты-на-java)
- [Дополнительные параметры](#дополнительные-параметры)
- [Где работают рецепты](#где-работают-рецепты)
- [Изменение и удаление рецепта](#изменение-и-удаление-рецепта)
- [Проверка и типичные ошибки](#проверка-и-типичные-ошибки)

---

## Что такое рецепт

**Рецепт** — это правило, по которому из ингредиентов получается ваш предмет.

Главная особенность Artifex: рецепт — это **не отдельный файл и не отдельная команда**, а раздел `recipes:` внутри самого предмета. Предмет, который вы скрафтили, — это и есть результат рецепта, со всеми своими эффектами, действиями и триггерами.

```yaml
my-crafted-sword:
  item:
    type: DIAMOND_SWORD
    title: '&bКлинок кузнеца'

  recipes:          # ← рецепты живут здесь
    shaped:
      - pattern:
          - " A "
          - "ABA"
          - " A "
        keys:
          A: DIAMOND
          B: STICK
```

Почему так удобно:

- Один файл — и описание предмета, и способ его получения.
- Ингредиентом может быть **другой ваш предмет**.
- Не нужно писать `/give`, чтобы выдать предмет — игрок сам скрафтит его.

Рецепты регистрируются в момент загрузки конфигурации. После правки файла обязательно выполните:

```bash
/ci reload
```

## Рецепты в YAML

Раздел `recipes:` содержит пять списков. Каждый — список рецептов, каждый рецепт — блок с ключами.

| Секция | Тип рецепта | Ключи внутри |
|--------|-------------|--------------|
| `shaped` | По формуле (сетка) | `pattern`, `keys`, `amount` |
| `shapeless` | Без формулы (любая раскладка) | `ingredients`, `amount` |
| `furnace` | Плавка в печи | `ingredient`, `experience`, `cooking-time` |
| `smithing` | Верстак кузнеца | `template`, `base`, `addition`, `amount` |
| `stonecutting` | Камнерез | `ingredient`, `amount` |

Можно использовать все пять в одном предмете — это не запрещено.

### Формульный рецепт shaped

Формульный рецепт описывает сетку 3×3 (или меньше) символами.

```yaml
  recipes:
    shaped:
      - pattern:
          - " A "
          - "ABA"
          - " A "
        keys:
          A: DIAMOND
          B: STICK
        amount: 1
```

| Ключ | Обязательный | Описание |
|------|--------------|----------|
| `pattern` | да | Список строк — «рисунок» рецепта. От 1 до 3 строк, в каждой до 3 символов |
| `keys` | да | Таблица «буква → ингредиент». Пробел в рисунке = пустая клетка, для неё ключ не нужен |
| `amount` | нет | Сколько предметов получится. По умолчанию `1` |

Правила рисунка:

- Все строки должны быть **одинаковой длины** — иначе рецепт не совпадёт.
- Пробел — пустая клетка. Не используйте табуляцию.
- Каждая буква (кроме пробела) должна быть описана в `keys`.
- Рецепт можно свернуть: для результата 3×3 достаточно одной строки из 3 символов — Minecraft сам развернёт её по сетке.

```yaml
      # Три золотых слитка в ряд — тот же результат, что и полная сетка
      - pattern:
          - "GGG"
        keys:
          G: GOLD_INGOT
```

### Рецепт без формулы shapeless

Ингредиенты кладутся в любом порядке и в любом месте сетки.

```yaml
  recipes:
    shapeless:
      - ingredients:
          - DIAMOND
          - DIAMOND
          - my-crafted-sword   # другой свой предмет — тоже можно
        amount: 1
```

| Ключ | Обязательный | Описание |
|------|--------------|----------|
| `ingredients` | да | Список ингредиентов. От 1 до 9 штук |
| `amount` | нет | Сколько предметов получится. По умолчанию `1` |

### Рецепт печи furnace

```yaml
  recipes:
    furnace:
      - ingredient: IRON_INGOT
        experience: 0.7
        cooking-time: 200
```

| Ключ | Обязательный | По умолчанию | Описание |
|------|--------------|--------------|----------|
| `ingredient` | да | — | Один ингредиент (материал или id своего предмета) |
| `experience` | нет | `0.0` | Сколько опыта получает игрок за плавку |
| `cooking-time` | нет | `200` | Время плавки в тиках. 20 тиков = 1 секунда, 200 тиков = 10 секунд |

- Ингредиент здесь **один**, а не список.
- Важно: у `furnace` **нет** ключа `amount` — количество результата берётся из `item.amount` самого предмета.

### Рецепт кузнеца smithing

Верстак кузнеца (в игре — «Smithing Table»). Нужны три вещи: шаблон, основа и добавка.

```yaml
  recipes:
    smithing:
      - template: NETHERITE_UPGRADE_SMITH_TEMPLATE
        base: DIAMOND_SWORD
        addition: NETHERITE_INGOT
        amount: 1
```

| Ключ | Обязательный | Описание |
|------|--------------|----------|
| `template` | да | Шаблон (например, `NETHERITE_UPGRADE_SMITH_TEMPLATE`) |
| `base` | да | Основа — то, что улучшают (меч, доспех) |
| `addition` | да | Добавка — материал улучшения (`NETHERITE_INGOT`) |
| `amount` | нет | Сколько предметов получится. По умолчанию `1` |

Порядок важен: сначала шаблон, потом основа, потом добавка.

### Рецепт камнереза stonecutting

```yaml
  recipes:
    stonecutting:
      - ingredient: STONE
        amount: 2
```

| Ключ | Обязательный | Описание |
|------|--------------|----------|
| `ingredient` | да | Один ингредиент |
| `amount` | нет | Сколько предметов получится. По умолчанию `1` |

## Ингредиенты

Ингредиент записывается одной строкой и может быть двух видов.

### 1. Имя материала Minecraft

Обычное имя материала: `DIAMOND`, `STICK`, `IRON_INGOT`, `NETHERITE_SWORD`.

```yaml
        keys:
          A: DIAMOND
          B: STICK
```

Удобства:

- Регистр не важен: `diamond`, `Diamond`, `DIAMOND` — одно и то же.
- Дефис и подчёркивание равнозначны: `IRON_INGOT` = `iron-ingot`.
- Префикс `minecraft:` можно оставить или убрать: `minecraft:diamond` = `DIAMOND`.

### 2. Id другого своего предмета

Вместо материала можно указать **id предмета Artifex**. Так делают «крафт из крафта»:

```yaml
    shapeless:
      - ingredients:
          - DIAMOND
          - DIAMOND
          - vampire-blade   # id предмета из YAML
```

```yaml
    shaped:
      - pattern:
          - " A "
          - "ABA"
          - " A "
        keys:
          A: DIAMOND
          B: ancient-shield   # id предмета из YAML
```

Что нужно знать:

- Id пишется **в точности как в YAML-файле** (обычно строчными буквами и через дефис), но плагин сравнивает его без учёта регистра.
- Подходят предметы **и из YAML, и из Java API** — плагин ищет сначала в YAML-предметах, потом в Java-предметах.
- Инвредиент-предмет сравнивается **целиком** (точное совпадение стека: материал, имя, лор, метки). Перекрашенный или переименованный вручную предмет ингредиентом не посчитает.
- В качестве ингредиента **нельзя** указать id предмета, которого не существует: плагин не найдёт его и в консоль появится предупреждение.

### Порядок поиска ингредиента

Плагин проверяет значение в таком порядке:

1. Это название материала Minecraft? → берём его.
2. Это id YAML-предмета? → берём его.
3. Это id Java-предмета? → берём его.
4. Ничего не нашлось → рецепт не регистрируется, в консоль пишется предупреждение.

## Рецепты на Java

Java-предметы задают рецепты в методе `getRecipes()`. Он возвращает список объектов `RecipeDef` — по одному на каждый рецепт.

```java
import me.dcplugin.artifex.api.RecipeDef;

import java.util.List;
import java.util.Map;

@Override
public List<RecipeDef> getRecipes() {
    Map<Character, String> keys = Map.of(
        'A', "DIAMOND",
        'B', "STICK"
    );

    return List.of(
        // Формульный рецепт
        RecipeDef.shaped(List.of(" A ", "ABA", " A "), keys),
        // То же, но два предмета за раз
        RecipeDef.shaped(List.of(" A ", "ABA", " A "), keys, 2),
        // Без формулы
        RecipeDef.shapeless("IRON_INGOT", "vampire-blade"),
        RecipeDef.shapeless(List.of("IRON_INGOT", "vampire-blade"), 3),
        // Печь
        RecipeDef.furnace("IRON_INGOT"),
        RecipeDef.furnace("IRON_INGOT", 0.7f, 200),
        // Кузнец
        RecipeDef.smithing("NETHERITE_UPGRADE_SMITH_TEMPLATE", "DIAMOND_SWORD", "NETHERITE_INGOT"),
        RecipeDef.smithing("NETHERITE_UPGRADE_SMITH_TEMPLATE", "DIAMOND_SWORD", "NETHERITE_INGOT", 1),
        // Камнерез
        RecipeDef.stonecutting("STONE"),
        RecipeDef.stonecutting("STONE", 2)
    );
}
```

Полный список конструкторов `RecipeDef`:

| Вызов | Что делает |
|-------|-----------|
| `RecipeDef.shaped(List<String> pattern, Map<Character, String> keys)` | Формульный рецепт, 1 предмет |
| `RecipeDef.shaped(List<String> pattern, Map<Character, String> keys, int amount)` | Формульный рецепт, `amount` предметов |
| `RecipeDef.shapeless(String... ingredients)` | Рецепт без формулы, 1 предмет |
| `RecipeDef.shapeless(List<String> ingredients)` | То же, список ингредиентов |
| `RecipeDef.shapeless(List<String> ingredients, int amount)` | То же, `amount` предметов |
| `RecipeDef.furnace(String ingredient)` | Плавка, 0 опыта, 200 тиков |
| `RecipeDef.furnace(String ingredient, float experience, int cookingTime)` | Плавка с опытом и временем |
| `RecipeDef.smithing(String template, String base, String addition)` | Кузнец, 1 предмет |
| `RecipeDef.smithing(String template, String base, String addition, int amount)` | Кузнец, `amount` предметов |
| `RecipeDef.stonecutting(String ingredient)` | Камнерез, 1 предмет |
| `RecipeDef.stonecutting(String ingredient, int amount)` | Камнерез, `amount` предметов |

Особенности:

- Формульный рисунок в Java должен содержать **1–3 строки**, иначе рецепт не зарегистрируется, а в консоль появится предупреждение `the shaped pattern must be 1-3 rows`.
- Ингредиенты записываются точно так же, как в YAML: `"DIAMOND"` или `"vampire-blade"`.
- Для кузнеца нужно ровно три ингредиента в порядке «шаблон, основа, добавка».

### Стиль builder

Если хочется писать по шагам:

```java
RecipeDef.builder(RecipeDef.Type.FURNACE)
    .ingredient("IRON_INGOT")
    .experience(0.7f)
    .cookingTime(200)
    .build();
```

Методы builder'а: `row(String)`, `key(char, String)`, `ingredient(String)`, `amount(int)`, `experience(float)`, `cookingTime(int)`, `build()`.

## Дополнительные параметры

| Параметр | Где | По умолчанию | Что означает |
|----------|-----|--------------|--------------|
| `amount` | `shaped`, `shapeless`, `smithing`, `stonecutting` (в YAML) и во всех `RecipeDef` | `1` | Сколько предметов в результате. В рецепте кузнеца значение меньше 1 заменяется на 1 |
| `experience` | `furnace` | `0.0` | Сколько опыта (очков) получает игрок за плавку. Дробное число допустимо: `0.7` |
| `cooking-time` | `furnace` | `200` | Время плавки в тиках. 20 тиков = 1 секунда |

Отдельно про количество результата в `furnace`: ключа `amount` у печи нет — количество берётся из `item.amount` в описании предмета:

```yaml
ingot-batch:
  item:
    type: IRON_INGOT
    amount: 3        # ← столько слитков получится при плавке

  recipes:
    furnace:
      - ingredient: IRON_BLOCK
        experience: 0.7
        cooking-time: 200
```

## Где работают рецепты

Рецепты регистрируются **в двух местах**:

| Где | Что работает |
|-----|--------------|
| Обычный верстак в игре (и печь, камнерез, верстак кузнеца) | `shaped`, `shapeless`, `furnace`, `smithing`, `stonecutting` |
| GUI-модуль крафта (`/craft`) | `shaped` и `shapeless` |

GUI-модуль — это отдельный модуль плагина (см. образец `samples/EXAMPLE-customcraft/`). Он собирает рецепты из всех предметов — и YAML, и Java — и показывает их в виде сетки 3×3. Рецепты печи в GUI не попадают: они работают в обычной печи.

Чтобы рецепты появились в `/craft`, модуль должен быть установлен и включён. Подробнее — [Модули](10-modules.md).

## Изменение и удаление рецепта

Рецепты перечитываются только при перезагрузке конфигурации.

### Изменить

1. Откройте YAML-файл предмета в папке `items/`.
2. Правьте раздел `recipes:`.
3. Выполните `/ci reload`.

### Удалить

- **Один рецепт** — удалите соответствующий блок из списка (например, весь `- pattern: ...` с вложенными ключами).
- **Все рецепты одного типа** — удалите секцию целиком, например `shapeless:` со всем содержимым.
- **Все рецепты предмета** — удалите весь раздел `recipes:`.

После `/ci reload` старые рецепты удаляются с сервера, а новые регистрируются заново. Если рецепт удалён, а `/ci reload` не выполнен, он останется работать до перезапуска сервера.

### Что происходит при /ci reload

Порядок работы:

1. Создаётся резервная копия папки `items/` (если включено в конфиге `backup.on-reload`).
2. Перечитывается `config.yml` и все файлы из `items/`.
3. **Все рецепты, зарегистрированные плагином, удаляются** с сервера.
4. Предметы загружаются заново, и их рецепты регистрируются заново.
5. Перечитываются бонусы комплектов, Java-предметы, команды и плейсхолдеры, модули.

Идентификаторы рецептов:

| Источник | Ключ рецепта |
|----------|--------------|
| YAML-предмет | `<id>_<тип>_<номер>`, например `fire-sword_shaped_0` |
| Java-предмет | `java_<id>_<тип>_<номер>`, например `java_fire-sword_shaped_0` |

Если два рецепта получают одинаковый ключ, второй не зарегистрируется — в консоль появится `Could not add recipe '...' (duplicate?)`.

## Проверка и типичные ошибки

### /ci validate

Команда проверяет рецепты и говорит, что с ними не так:

```bash
/ci validate
```

- Требуется право `artifex.reload`.
- Проверяются секции `shaped` и `shapeless` у всех YAML-предметов.
- Если проблем нет — сообщение `Validation OK: no problems found.` Если есть — список проблем и их количество.

Что именно проверяется:

| Проверка | Сообщение |
|----------|-----------|
| Пустой рисунок | `recipes.shaped.pattern: empty pattern (needs 1-3 rows)` |
| Больше трёх строк | `recipes.shaped.pattern: more than 3 rows` |
| Строка длиннее трёх символов | `recipes.shaped.pattern: row longer than 3: '...'` |
| Строки разной длины | `recipes.shaped.pattern: uneven rows` |
| Буква без ключа | `recipes.shaped.keys: no key for symbol 'A'` |
| Неизвестный ингредиент | `recipes.shaped.keys.A: unknown ingredient 'XYZ'` |
| Пустой список ингредиентов | `recipes.shapeless: empty ingredients list` |
| Больше девяти ингредиентов | `recipes.shapeless: more than 9 ingredients` |
| Неизвестный ингредиент без формулы | `recipes.shapeless: unknown ingredient 'XYZ'` |

### Что пишет консоль при загрузке

Успех — обычное сообщение в консоли:

```text
[Recipes] 'fire-sword': shaped recipe #0
[Recipes] 'fire-sword': shapeless recipe #0
[Recipes] 'fire-sword': furnace recipe #0
```

Ошибки — предупреждения:

| Сообщение | Что означает |
|-----------|--------------|
| `[Recipes] 'my-item': unknown ingredient 'XYZ' for key 'A'` | В `keys` указан ингредиент, которого нет |
| `[Recipes] 'my-item': unknown ingredient 'XYZ'` | То же для `shapeless` |
| `[Recipes] 'my-item': unknown furnace ingredient 'XYZ'` | Неизвестный ингредиент печи |
| `[Recipes] 'my-item': smithing #0 - check template/base/addition` | Не заполнены или не найдены `template` / `base` / `addition` |
| `[Recipes] 'my-item': stonecutting #0 - unknown ingredient` | Неизвестный ингредиент камнереза |
| `[Recipes] Could not add recipe 'my-item_shaped_0' (duplicate?)` | Рецепт с таким ключом уже есть |
| `[Recipes] Error in shaped recipe 'my-item': ...` | Рецепт упал при регистрации (обычно — из-за YAML) |
| `[Recipes] 'my-item': the shaped pattern must be 1-3 rows` | Java-рецепт: неверное число строк в рисунке |
| `[Recipes] smithing is not available on this server core: ...` | Ядро сервера не поддерживает верстак кузнеца |

### Типичные ошибки

#### Неправильный отступ

YAML чувствителен к отступам. Все ключи рецепта должны быть на одном уровне, а список — сдвинут относительно имени:

```yaml
  recipes:                  # ← 2 пробела
    shaped:                 # ← 4 пробела
      - pattern:            # ← 6 пробелов
          - " A "           # ← 10 пробелов
        keys:               # ← 8 пробелов
          A: DIAMOND        # ← 10 пробелов
```

Ошибка отступа в YAML обычно означает, что раздел просто не прочитался: рецепт молча не зарегистрируется, а в логе не будет ни слова. Проверьте файл командой `/ci validate` и внимательно посмотрите на отступы.

#### Ингредиент не найден

```yaml
        keys:
          A: DIAMNOD     # ← опечатка
```

Результат: `[Recipes] 'my-item': unknown ingredient 'DIAMNOD' for key 'A'` — рецепт не зарегистрирован, предмет нельзя скрафтить.

#### Ссылка на несуществующий предмет

```yaml
    shapeless:
      - ingredients:
          - DIAMOND
          - legendary-sword   # ← такого предмета нет
```

Та же ошибка: `unknown ingredient 'legendary-sword'`. Проверьте, что файл с этим предметом лежит в `items/` и не начинается с `EXAMPLE-`.

#### Рецепт не появляется в верстаке

- Не выполнен `/ci reload` после правки.
- Рецепт не зарегистрировался из-за ошибки выше — посмотрите лог.
- Рисунок имеет неодинаковые строки или символ без ключа.
- `keys` описывает букву, которой нет в рисунке (лишний ключ — не ошибка, но стоит убрать).

#### Рецепт есть, а предмет не тот

- В `item:` указано другое имя/лор, чем ожидалось: результат рецепта — это ровно тот предмет, который описан в YAML.
- Для ингредиента-предмета сравнивается весь стек: если игрок перекрасил или переименовал клинок, рецепт его не примет.

#### Количество не то, что ожидалось

- В `furnace` нет ключа `amount` — количество берётся из `item.amount`.
- В остальных типах рецептов `amount` задаёт результат, но значение меньше 1 для кузнеца заменяется на 1.

---

Смотрите также: [Предметы (YAML)](03-yaml-items.md) · [Предметы (Java)](04-java-items.md) · [Действия](05-actions.md) · [Модули](10-modules.md) · [Решение проблем](17-troubleshooting.md)
