
# 📖 Руководство по созданию кастомных оверлеев для Dota 2

Данное приложение выступает в роли сервера, который принимает данные от игры Dota 2 (Game State Integration), обрабатывает их и передает в реальном времени на любые веб-страницы (оверлеи) через WebSocket.

## 1. Как это работает (Архитектура)

1. Игра Dota 2 отправляет JSON-пакеты на локальный сервер (порт `44444`).
2. Сервер сохраняет данные и мгновенно рассылает их обработанный вид всем подключенным клиентам через WebSocket (STOMP): топики `/topic/overlay` и `/topic/draft`.
3. Оверлей — это обычная HTML-страница, которая лежит в папке `src/main/resources/static/`. Она подключается к WebSocket, получает данные и отрисовывает их с помощью HTML/CSS/JS.
4. В OBS добавляется источник **"Браузер" (Browser Source)**, который указывает на URL этой страницы (например, `http://localhost:8080/my-overlay.html`). Фон страницы должен быть прозрачным.

---

## 2. Быстрый старт: Создание первой страницы

1. Создайте файл в папке `src/main/resources/static/`, например `my-custom-overlay.html`.
2. Вставьте в него следующий шаблон:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Мой Оверлей</title>
    <!-- Подключаем библиотеки для WebSocket -->
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <style>
        /* Прозрачный фон для OBS */
        body { background: transparent; color: white; font-family: sans-serif; margin: 0; padding: 20px; }
        #my-data { font-size: 24px; background: rgba(0,0,0,0.7); padding: 10px; border-radius: 5px; display: inline-block; }
    </style>
</head>
<body>
    <!-- Элемент для вывода данных -->
    <div id="my-data">Ожидание данных...</div>

    <script>
        // 1. Подключаемся к WebSocket серверу
        const socket = new SockJS('/ws');
        const stompClient = new StompJs.Client({
            webSocketFactory: () => socket,
            reconnectDelay: 3000 // Авто-переподключение при обрыве связи
        });

        // 2. При успешном подключении подписываемся на данные
        stompClient.onConnect = function () {
            console.log('Подключено к серверу Dota 2');
            
            stompClient.subscribe('/topic/overlay', function (message) {
                // message.body - это JSON-строка, парсим её
                const data = JSON.parse(message.body);
                
                // 3. Передаем данные в функцию отрисовки
                updateOverlay(data);
            });
        };

        stompClient.activate();

        // 4. Функция отрисовки
        function updateOverlay(data) {
            // Пример: выводим текущее игровое время (топик /topic/overlay)
            if (data.gameTime !== undefined) {
                const seconds = Math.abs(data.gameTime);
                const m = Math.floor(seconds / 60);
                const s = seconds % 60;
                document.getElementById('my-data').innerText = `Время игры: ${m}:${s < 10 ? '0' : ''}${s}`;
            }
        }
    </script>
</body>
</html>
```

---

## 3. Источники данных (Топики WebSocket)

Сервер публикует два топика (оба в формате JSON):

### 📌 `/topic/overlay` (Обработанные данные)
Используется для простых оверлеев. Содержит только базовую информацию.
```json
{
  "gameTime": 1200,
  "daytime": true,
  "radiantScore": 15,
  "direScore": 12,
  "radiantEconomy": 55000.0,
  "direEconomy": 52000.0,
  "radiantHeroes": [
    {
      "name": "PlayerName",
      "heroName": "npc_dota_hero_axe",
      "level": 15,
      "hpPercent": 80,
      "manaPercent": 50,
      "alive": true,
      "respawnTimer": 0
    }
  ],
  "direHeroes": [ ... ]
}
```

### 📌 `/topic/draft` (Драфт: пики и баны)
Используется оверлеем `overlay-draft.html`. Публикуется, только если игра присылает блок `draft`
(в `gamestate_integration_*.cfg` должно быть `"draft" "1"`, матч нужно смотреть как зритель).
```json
{
  "mode": "SPECTATING",
  "match": { "state": "HERO_SELECTION", "gameTime": 0 },
  "draft": {
    "activeTeam": 2,
    "activeTimeRemaining": 25,
    "radiantBonusTime": 130,
    "direBonusTime": 130,
    "radiant": { "picks": [ { "slug": "axe", "name": "axe" } ], "bans": [ ] },
    "dire":    { "picks": [ ], "bans": [ ] }
  }
}
```
* `match.state` — `HERO_SELECTION`, `PRE_GAME` или `GAME_IN_PROGRESS`.
* `activeTeam` — `2` (ход Radiant), `3` (ход Dire), иначе никто не ходит.
* `slug` — имя героя для картинок (`.../dota_react/heroes/<slug>.png`).

> Сырой JSON игры (`map`, `heroes`, `items` и т.д.) через WebSocket **не публикуется**:
> сервер получает данные через библиотеку dota2-gsi, которая отдаёт уже разобранные объекты.
> Если нужны дополнительные поля — их нужно добавить в `OverlayState` / `DraftPayload`.

---

## 4. Справочник по формату GSI (справочный)

> ⚠️ Этот раздел описывает **исходный формат Game State Integration от Dota 2**. Эти данные в WebSocket напрямую не попадают (см. раздел 3): используйте их как справочник, чтобы понимать, какие поля есть в игре и что можно добавить в `OverlayState` / `DraftPayload`.

### 🗺️ Блок `map` (Карта и глобальные события)
```javascript
data.map.clock_time             // Время игры в секундах (например, 1200)
data.map.game_time              // Игровое время (может быть отрицательным во время пика)
data.map.daytime                // true (день) / false (ночь)
data.map.nightstalker_night     // true, если активна ночь Ночного Сталкера
data.map.radiant_score          // Убийства Света
data.map.dire_score             // Убийства Тьмы
data.map.win_team               // "radiant", "dire" или "none"

// Данные о Рошане
data.map.roshan_state           // "alive" (жив), "respawning" (скоро появится), "dead"
data.map.roshan_state_end_seconds // Через сколько секунд появится Рошан (если state == respawning)
```

### 🦸‍♂️ Блок `heroes` (Состояние героев)
Ключи в объекте: `player0`, `player1` ... `player9`.
*(0-4 это Свет, 5-9 это Тьма)*
```javascript
const hero = data.heroes.player0;

hero.name               // Имя героя для картинок (например, "npc_dota_hero_axe")
hero.level              // Уровень героя
hero.alive              // true / false
hero.respawn_seconds    // Время до возрождения (если мертв)

hero.health             // Текущее здоровье
hero.max_health         // Максимальное здоровье
hero.health_percent     // Процент здоровья (0-100)

hero.mana               // Текущая мана
hero.max_mana           // Максимальная мана
hero.mana_percent       // Процент маны (0-100)

// Статусы (очень полезно для оверлеев)
hero.stunned            // true (в стане)
hero.silenced           // true (в сайленсе)
hero.hexed              // true (под хексом)
```

### 👤 Блок `players` (Игроки и экономика)
Данные разбиты по командам. Массивы всегда содержат 5 игроков.
```javascript
// Игроки Света
const radiantPlayers = data.players.radiant; 
const p1 = radiantPlayers[0];

p1.name         // Никнейм (например, "Miracle-")
p1.net_worth    // Общая экономика игрока
p1.kills        // Убийства
p1.deaths       // Смерти
p1.assists      // Помощь
p1.gpm          // Золото в минуту
p1.xpm          // Опыт в минуту

// Игроки Тьмы
const direPlayers = data.players.dire; 
```

### 🎒 Предметы и Способности (`items` / `abilities`)
*Внимание: чтобы эти блоки приходили, в конфиге `gamestate_integration.cfg` должны быть включены параметры `"items" "1"` и `"abilities" "1"`.*
```javascript
// Предметы игрока (по индексу player)
const items = data.items.player0;
items.slot0             // ID предмета в первом слоте
items.slot0_cooldown    // Кулдаун предмета (если есть)
items.stash0            // Предмет на складе
items.teleport0         // ТП-свиток

// Способности игрока
const abilities = data.abilities.player0;
abilities.ability0      // Название способности
abilities.ability0_level // Уровень прокачки
abilities.ability0_cooldown // Кулдаун
```

### ⚔️ Блок `draft` (Стадия Пика/Бана)
Идеально подходит для создания экранов драфта.
```javascript
data.draft.team2.pick // Массив ID выбранных героев за Свет
data.draft.team2.ban  // Массив ID забаненных героев за Свет
data.draft.team3.pick // Массив ID выбранных героев за Тьму
```

---

## 5. Полезные ссылки и ресурсы

Так как игра передает технические названия (например, `npc_dota_hero_axe` или ID предмета `1`), вам понадобятся ресурсы для их визуализации:

### 🖼️ Картинки героев (CDN Valve)
```javascript
// В GSI имя выглядит как: npc_dota_hero_axe
// Для картинки нужно отрезать префикс:
const heroName = hero.name.replace('npc_dota_hero_', ''); // получится "axe"
const imgUrl = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/${heroName}.png`;
```

### 🖼️ Картинки способностей (CDN Valve)
```javascript
const imgUrl = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/abilities/${abilityName}.png`;
```

### 🖼️ Картинки предметов
*Особенность Dota 2 GSI: игра присылает цифровые ID предметов (например, `127`), а не их названия.*
Чтобы вывести картинку, вам понадобится справочник (маппинг) ID -> Название.
Готовые JSON-справочники предметов можно найти на GitHub (ищите `dota 2 item ids json`).
Зная название предмета, картинка строится так:
```javascript
const imgUrl = `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/items/${itemName}.png`;
```

---

## 6. Советы по созданию оверлеев для OBS

1. **Прозрачность**: Всегда используйте `background: transparent;` для тега `body`, иначе в OBS будет черный или белый фон.
2. **Позиционирование**: Используйте `position: absolute;` и отступы (`top`, `left`, `right`, `bottom`) для размещения элементов по краям экрана (в центре обычно идет игра).
3. **Разрешение**: По умолчанию OBS использует разрешение `1920x1080`. Создавайте оверлеи с учетом этого размера.
4. **Производительность**: Не пересоздавайте DOM-элементы (через `innerHTML`) каждую секунду, если данные не изменились. Обновляйте только текст или ширину полосок (как показано в примере с `overlay.html`).
5. **Таймауты**: Если игра выключена, данные перестанут приходить. Рекомендуется добавить `setTimeout` в JavaScript, чтобы скрывать элементы оверлея, если данные не обновлялись более 5 секунд (чтобы не показывать зависший счет).