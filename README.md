Особенности плагина:



• Очень прост в использовании

• Поддержка схематик

• Поддержка HEX цветов



См.конфигурация:




settings:

min-height: 10   # Минимальная высота для использования трапки

  save-radius: 5   # Радиус сохранения блоков при создании трапки

  check-regions: true   # Включить проверку регионов WorldGuard

  enable-sounds: true   # Включить звуки

  enable-particles: true   # Включить частицы

  enable-effects: true   # Включить эффекты

  disable-flight: true   # Отключать полет в трапках

  disable-god: true   # Отключать режим бога в трапках

  debug: false   # Дебаг режим (логирование флагов)



  disabled-worlds:

- "spawn"

    - "world_nether"

    - "world_the_end"

    - "pvp_arena"

  allow-in-regions: true   # Разрешить использование трапок в регионах

  bypass-trap-check: false   # Разрешить байпас проверки трапок (только для админов)



# Настройки партиклов

particles:

count: 20    # Количество частиц для игрока

  offset-x: 0.5   # Смещение по X

  offset-y: 0.5  # Смещение по Y

  offset-z: 0.5   # Смещение по Z

  speed: 0.1   # Скорость частиц

  circle-radius: 2.0   # Радиус круга для частиц

  spawn-count: 50    # Количество партиклов вокруг схемы

  spawn-radius: 3.0   # Радиус спавна партиклов вокруг схемы

  spawn-height: 2.0   # Высота спавна партиклов вокруг схемы



traps:

 basic:

schematic: "traps/3x3_trapka.schem"

    name: "&7 &8&l☆ &x&F&F&0&0&0&0Базовая Трапка"

    lore:

- ''

      - "  §x&F&F&0&0&0&0Особенности:"

      - " §x&F&F&0&0&0&0╠ §x&D&D&D&D&D&DСоздает прочную трапку вокруг игрока"

      - " §x&F&F&0&0&0&0╠ §x&D&D&D&D&D&DБлокирует использование эндер-жемчуга"

      - " §x&F&F&0&0&0&0╠ §x&D&D&D&D&D&DДлительность: §x&F&F&0&0&0&015 секунд"

      - ''

      - "§7Перезарядка: §e30 секунд"

    material: CHORUS_PLANT

cooldown: 30   # Перезарядка в секундах

    time: 15 # Длительность трапки в секундах

    sound-start: "BLOCK_ANVIL_PLACE" # Звуки при создании и окончании трапки

    sound-stop: "BLOCK_ANVIL_LAND"

    particle-start: "REDSTONE" # Частицы при создании и окончании трапки

    particle-stop: "REDSTONE"

    particle-color-start: "#FF0000" # Цвет частиц (для REDSTONE)

    particle-color-stop: "#00FF00"

    effects:     # Эффекты на игрока в трапке (ЭФФЕКТ:ВРЕМЯ В СЕКУНДАХ:УРОВЕНЬ ЭФФЕКТА  )

      - "SLOW:15:1"

      - "BLINDNESS:15:1"

    region-flags:     # Флаги региона WorldGuard

      - "enderpearl:deny"

      - "pvp:allow"

      - "block-break:deny"

      - "block-place:deny"

      - "invincible:deny"

      - "fly:deny"

      - "interact:deny"

      - "build:deny"      # Запрет на строительство

      - "chest-access:deny"

      - "use:deny"

    region-padding: 3     # Отступ региона от схемы (в блоках)



  advanced:

schematic: "traps/5x5_trapka.schem"

    name: "&7 &8&l☆ &x&5&5&F&F&0&0Улучшенная Трапка"

    lore:

- ''

      - "  §x&5&5&F&F&0&0Особенности:"

      - " §x&5&5&F&F&0&0╠ §x&D&D&D&D&D&DСоздает усиленную трапку с эффектами"

      - " §x&5&5&F&F&0&0╠ §x&D&D&D&D&D&DПолная блокировка телепортации"

      - " §x&5&5&F&F&0&0╠ §x&D&D&D&D&D&DДлительность: §x&5&5&F&F&0&020 секунд"

      - " §x&5&5&F&F&0&0╠ §x&D&D&D&D&D&DНаносит эффекты слабости и слепоты"

      - ''

      - "§7Перезарядка: §e45 секунд"

    material: CRYING_OBSIDIAN

 cooldown: 45

 time: 20

sound-start: "BLOCK_BEACON_ACTIVATE"

    sound-stop: "BLOCK_BEACON_DEACTIVATE"

    particle-start: "SOUL_FIRE_FLAME"

    particle-stop: "DRAGON_BREATH"

    particle-color-start: "#FF5500"

    particle-color-stop: "#5500FF"

    effects:

- "SLOW:20:2"

      - "BLINDNESS:20:1"

      - "WEAKNESS:20:1"

    region-flags:

- "enderpearl:deny"

      - "pvp:allow"

      - "block-break:deny"

      - "block-place:deny"

      - "invincible:deny"

      - "fly:deny"

      - "interact:deny"

      - "build:deny"      # Запрет на строительство

      - "chest-access:deny"

      - "use:deny"

    region-padding: 4

[/CODE]



См. конфигурация локализации:





[CODE=yaml]

prefix: "§x§5§A§9§B§F§F§lᴡɪsʜᴍɪɴᴇ &7»"



# Основные сообщения об ошибках

no-permission: "{prefix} &fУ вас §x§5§A§9§B§F§F§недостаточно&f прав!"

player-not-found: "{prefix} &fИгрок не найден."

invalid-amount: "{prefix} &fНеверное количество."

trap-not-found: "{prefix} &fТип трапки не найден!"

schematic-not-found: "{prefix} &fСхема для трапки не найдена."

in-region: "{prefix} &fВы не можете §x§5§A§9§B§F§F§использовать&f трапку в чужом регионе."

height-block: "{prefix} &fВы не можете §x§5§A§9§B§F§F§использовать&f трапку ниже §x§5§A§9§B§F§F§10&f уровня высоты."

cooldown: "{prefix} &fВы не можете §x§5§A§9§B§F§F§использовать&f трапку сейчас. Подождите еще §x§5§A§9§B§F§F§%cooldown%&f секунд."

item-creation-failed: "{prefix} &fОшибка создания предмета."

trap-too-close: "{prefix} &fВы не можете §x§5§A§9§B§F§Fиспользовать&f трапку так близко к другой трапке!"

world-disabled: "{prefix} &fИспользование трапок запрещено в этом мире!"

# Сообщения команд

command-usage:

  - "&6&lKTrapLeave &7- Команды:"

  - "&6/ktrapleave give <игрок> <количество> [тип] &7- выдать трапку"

  - "&6/ktrapleave list &7- список доступных трапок"

  - "&6/ktrapleave help &7- показать это сообщение"



command-usage-admin:

  - "&6&lKTrapLeave &7- Команды администратора:"

  - "&6/ktrapleave give <игрок> <количество> [тип] &7- выдать трапку"

  - "&6/ktrapleave list &7- список доступных трапок"

  - "&6/ktrapleave wand &7- получить инструмент для создания схем"

  - "&6/ktrapleave pos1 &7- установить первую позицию схемы"

  - "&6/ktrapleave pos2 &7- установить вторую позицию схемы"

  - "&6/ktrapleave save <название> &7- сохранить схему"

  - "&6/ktrapleave reload &7- перезагрузить конфигурацию"

  - "&6/ktrapleave help &7- показать это сообщение"



give-success: "{prefix} &fИгроку §x§5§A§9§B§F§F§%player%&f выдано §x§5§A§9§B§F§F%amount% &fтрапок типа §x§5§A§9§B§F§F%trap%&f."

give-usage: "{prefix} &fИспользование: /ktrapleave give <игрок> <количество> [тип]"

save-usage: "{prefix} &fИспользование: /ktrapleave save <название>"



# Сообщения трапок

trap-create-success: "{prefix} &fТрапка §x§5§A§9§B§F§F§успешно&f создана!"

trap-active: "{prefix} &fВаша трапка активна! Длительность: §x§5§A§9§B§F§F§%time% &fсекунд"

trap-expired: "{prefix} &fВремя действия трапки истекло!"



# Сообщения для создания схем

wand-received: "{prefix} &fВы получили §x§5§A§9§B§F§F§волшебный топор&f для создания схематик!"

pos1-set: "{prefix} &fПервая позиция установлена: X: §x§5§A§9§B§F§F§%x%&f, Y: §x§5§A§9§B§F§F§%y%&f, Z: §x§5§A§9§B§F§F§%z%"

pos2-set: "{prefix} &fВторая позиция установлена: X: §x§5§A§9§B§F§F§%x%&f, Y: §x§5§A§9§B§F§F§%y%&f, Z: §x§5§A§9§B§F§F§%z%"

positions-not-set: "{prefix} &fСначала установите обе позиции!"

invalid-name: "{prefix} &fУкажите §x§5§A§9§B§F§F§название&f для схемы!"

invalid-name-format: "{prefix} &fНазвание схемы может содержать §x§5§A§9§B§F§F§только буквы, цифры, дефисы и подчеркивания&f!"

schematic-saved: "{prefix} &fСхематика §x§5§A§9§B§F§F§успешно&f сохранена как: §x§5§A§9§B§F§F§%name%.schem"

save-error: "{prefix} &fОшибка при сохранении схемы!"

positions-cleared: "{prefix} &fПозиции сброшены!"



# Список трапок

trap-list-header: "{prefix} &fДоступные трапки:"

trap-list-entry: "&7- §x§5§A§9§B§F§F§%id% &8» &f%name%"



# Перезагрузка

reload-success: "{prefix} &fКонфигурация §x§5§A§9§B§F§F§успешно&f перезагружена!"
