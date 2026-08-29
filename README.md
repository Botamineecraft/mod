# TACZ Soldiers (Forge 1.20.1)

Мод для Minecraft Forge 1.20.1, который добавляет **солдат**, вооружённых оружием из мода
**TACZ (Timeless and Classics Zero)**. Солдаты сами ищут враждебных мобов, целятся,
стреляют настоящими пулями TACZ, перезаряжаются и могут охранять позицию.

## Возможности

- **Солдат** — человекоподобный моб (30 HP, броня 2), спавнится с яйцом призыва
  (`soldier_spawn_egg`, вкладка «Яйца призыва» / «Сражения» в креативе) или командой
  `/summon taczsoldiers:soldier`.
- При спавне солдат получает случайное оружие из пула (конфиг `gun_pool`):
  по умолчанию `tacz:ak47`, `tacz:m4a1`, `tacz:m16a4`, `tacz:hk_mp5a5`, `tacz:ump45`,
  `tacz:uzi`, `tacz:scar_h`, `tacz:glock_17`.
- Стрельба идёт через публичный API TACZ `IGunOperator` — пули, урон, звуки,
  перезарядки и трассеры полностью из TACZ. Солдаты используют «dummy ammo»
  (встроенный резерв патронов TACZ), поэтому им не нужен инвентарь.
- Солдаты атакуют монстров (криперов — только при `target_creepers = true`),
  отвечают обидчикам и не горят у костров (избегают огня).
- **ПКМ пустой рукой + Shift** — режим охраны: солдат возвращается к указанной позиции.
- **ПКМ оружием TACZ** — выдать солдату другое оружие (старое вернётся вам).
- Без TACZ мод тоже работает: солдаты откатываются на стрельбу обычными стрелами
  (TACZ в `mods.toml` объявлен как необязательная зависимость).

## Конфигурация

Серверный конфиг `config/taczsoldiers-server.toml`:

| Параметр | По умолчанию | Описание |
|---|---|---|
| `gun_pool` | список выше | пул id оружия TACZ |
| `infinite_ammo` | `true` | бесконечный резерв патронов |
| `ammo_reserve` | `2000` | размер резерва dummy ammo |
| `attack_range` | `24.0` | дальность стрельбы в блоках |
| `max_health` | `30.0` | здоровье солдата |
| `target_creepers` | `false` | атаковать ли криперов |

## Требования

- Minecraft 1.20.1, Forge 47.x (сборка проверена на `1.20.1-47.3.0`)
- TACZ (любая актуальная версия для 1.20.1) — для настоящего оружия
  (без него — стрелы).

## Сборка

Сборка выполняется автоматически в GitHub Actions (`.github/workflows/build.yml`):
workflow собирает мод Gradle-ом, скачивает TACZ с Modrinth, поднимает настоящий
production-сервер Forge 1.20.1 + TACZ + наш мод, призывает солдата командой
`summon taczsoldiers:soldier` и проверяет логи. Готовый jar и все логи лежат в
папке `build-output/` этой ветки:

- `build-output/taczsoldiers-1.0.0.jar` — **готовый мод** (reobf, production);
- `build-output/server.log` — лог smoke-теста (видно «TACZ API bridge
  initialized successfully» и «Summoned new Soldier» без ошибок).

Локально (нужен интернет для maven Forge/Mojang):

```bash
./gradlew build      # jar будет в build/libs/taczsoldiers-1.0.0.jar
```

Установка: положите jar из `build/libs` (или `build-output/`) в папку `mods`
клиента и сервера вместе с Forge и TACZ.

## Структура интеграции с TACZ

`src/main/java/.../tacz/TaczBridge.java` — весь доступ к TACZ через рефлексию:

- `IGunOperator.fromLivingEntity(entity).shoot(pitch, yaw)` — выстрел;
- `.reload()` / `.draw(supplier)` / `.initialData()`;
- `TimelessAPI.getCommonGunIndex(id)` — проверка валидности id оружия;
- создание стака оружия: предмет `tacz:modern_kinetic_gun` + NBT-теги
  `GunId`, `GunCurrentAmmoCount`, `HasBulletInBarrel`, `DummyAmmo`, `MaxDummyAmmo`.

Прямых импортов классов TACZ нет, поэтому мод компилируется и загружается без TACZ.
