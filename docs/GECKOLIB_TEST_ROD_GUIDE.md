# Fishka: универсальный гайд по GeckoLib 5 (на примере тестовой удочки)

## 1. Что это за гайд
Это универсальная инструкция для добавления **любого** анимированного предмета через GeckoLib в `fishka-1.21.5`.

Тестовая удочка (`test_fishing_rod`) используется только как пример реализации.

## 2. Базовые требования
1. Minecraft/Fabric:
- `minecraft_version=1.21.5`
- `fabric-loader=0.18.4+`
2. Java:
- строго Java 21.
3. GeckoLib:
- `geckolib-fabric-1.21.5:5.0+`

## 3. Обязательная конфигурация проекта

### 3.1 `build.gradle`
В `repositories` нужен Cloudsmith GeckoLib:
- `https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/`
- `includeGroup("software.bernie.geckolib")`

В `dependencies`:
- `modImplementation "software.bernie.geckolib:geckolib-fabric-${project.minecraft_version}:${project.geckolib_version}"`

### 3.2 `gradle.properties`
Добавить:
- `geckolib_version=5.0` (или согласованную версию выше)

### 3.3 `fabric.mod.json`
В `depends`:
- `"geckolib": ">=5.0"`

## 4. Главный нюанс Fishka: split source sets
В проекте включен `splitEnvironmentSourceSets()`, поэтому:
1. `main` код должен быть server-safe (без client-only зависимостей).
2. `GeoItem`-реализация лучше держать в `src/client/java`.

### Рекомендуемый паттерн (универсально)
1. В `main`:
- базовый класс предмета с геймплейной логикой (наследник обычного item/custom item).
2. В `client`:
- класс-наследник с `implements GeoItem`, контроллерами и renderer/provider.
3. В регистрации предмета:
- на клиенте создавать client-класс (через `EnvType.CLIENT` + reflection),
- на сервере создавать базовый класс.

Этот паттерн предотвращает краши dedicated server.

## 5. Структура ресурсов для любого GeckoLib item

### 5.1 Критично для GeckoLib 5
Модели и анимации должны лежать именно тут:
- `assets/<modid>/geckolib/models/<name>.geo.json`
- `assets/<modid>/geckolib/animations/<name>.animation.json`

Текстуры:
- `assets/<modid>/textures/item/<name>.png`

Item model base:
- `assets/<modid>/models/item/<name>.json`

Item definition (1.21.4+):
- `assets/<modid>/items/<name>.json`

### 5.2 Пример из текущего проекта
1. `assets/fishka/geckolib/models/test_fishing_rod.geo.json`
2. `assets/fishka/geckolib/animations/test_fishing_rod.animation.json`
3. `assets/fishka/textures/item/test_fishing_rod.png`
4. `assets/fishka/models/item/test_fishing_rod.json`
5. `assets/fishka/items/test_fishing_rod.json`

## 6. Минимальный pipeline добавления нового GeckoLib-предмета

1. Создать базовый server-safe item в `main`.
2. Создать client-only наследник:
- `implements GeoItem`
- `AnimatableInstanceCache`
- `registerControllers(...)`
- `createGeoRenderer(...)`
3. Добавить `GeoModel` и `GeoItemRenderer` в `client`.
4. Зарегистрировать item в `ModItems`.
5. Добавить item в нужные item groups.
6. Добавить локализацию `ru_ru` и `en_us` (в проекте `en_us` тоже русским текстом).
7. Подложить ассеты в правильные папки GeckoLib 5.
8. Проверить сборку и запуск.

## 7. Настройка `GeoModel` (универсальное правило)
В `GeoModel` возвращайте id **без префикса папки и без суффикса**:
- `Fishka.id("my_item_name")`

Почему:
- GeckoLib 5 сам мапит это на `geckolib/models` и `geckolib/animations`.

Неправильно:
- `Fishka.id("geo/my_item.geo.json")`
- `Fishka.id("animations/my_item.animation.json")`

## 8. Контроллеры анимации: безопасные практики

1. Любой `DataTickets.*` перед чтением проверять через `state.hasData(...)`.
2. Не читать `ITEM_RENDER_PERSPECTIVE` без проверки.
3. Для “анимация только в руках” фильтровать контексты:
- first/third person left/right hand.
4. Для trigger-анимаций использовать отдельный контроллер:
- например `cast_controller` + `triggerableAnim(...)`.

## 9. Trigger с сервера (универсально)
Если анимация должна стартовать по игровому событию:
1. Делаем проверку события на сервере.
2. Получаем id экземпляра через `GeoItem.getOrAssignId(stack, serverWorld)`.
3. Вызываем `triggerAnim(...)`.

Для удочки это было событие успешного старта заброса.

## 10. JSON-шаблон для `assets/<modid>/items/<name>.json`
Используется special model с geckolib renderer:

```json
{
  "model": {
    "type": "minecraft:special",
    "base": "<modid>:item/<name>",
    "model": {
      "type": "geckolib:geckolib"
    }
  }
}
```

## 11. Проверка перед запуском

1. Компиляция:
```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:GRADLE_USER_HOME='D:\Code\WORK\fishka-1.21.5\.gradle-home'
./gradlew compileJava compileClientJava processResources
```

2. Полная сборка:
```powershell
./gradlew build
```

3. Dev запуск:
```powershell
./gradlew runClient
```

## 12. Таблица типовых ошибок и решений

### Ошибка
`Attempted to retrieve data from GeoRenderState that does not exist`

Причина:
- `state.getData(...)` вызван без проверки наличия ticket.

Решение:
- использовать `state.hasData(ticket)` перед `getData`.

### Ошибка
`Unable to find model file: <modid>:...`

Причина:
- модель не в `assets/<modid>/geckolib/models`,
- или неверный id в `GeoModel#getModelResource`.

Решение:
1. проверить папки `geckolib/models` и `geckolib/animations`,
2. в `GeoModel` вернуть короткий id `Fishka.id("<name>")`.

### Ошибка server-side class loading
`ClassNotFoundException: net.minecraft.client...`

Причина:
- client классы утекли в `main`.

Решение:
- разделить на server-safe base class + client Geo class.

## 13. Что брать из примера `test_fishing_rod`
Использовать как reference для любого нового предмета:
1. Паттерн разделения `main/client`.
2. Реализацию контроллеров loop + trigger.
3. Серверный trigger через `getOrAssignId + triggerAnim`.
4. Структуру ресурсов GeckoLib 5.

## 14. Быстрый чеклист “добавил новый gecko item”
1. Зависимости GeckoLib есть.
2. Item зарегистрирован и локализован.
3. Есть client-only GeoItem класс.
4. Есть `GeoModel` и `GeoItemRenderer`.
5. Ассеты лежат в `geckolib/models` и `geckolib/animations`.
6. `items/<name>.json` использует `minecraft:special` + `geckolib:geckolib`.
7. Компиляция проходит.
8. В креативе предмет не крашит GUI.
9. Анимации работают в нужных контекстах.

