# relal-last-wroking-proga
📚 ДОКУМЕНТАЦИЯ: Как создать свой оверлей
Ниже приведена полная документация, которую вы можете сохранить в проект (например, в README.md), чтобы любой разработчик мог создать оверлей.

1. Подключение к WebSocket
   Для получения данных подключитесь к эндпоинту /ws и подпишитесь на топик /topic/raw.

Шаблон HTML-файла:

html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Мой Кастомный Оверлей</title>
    <!-- Подключаем библиотеки -->
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <style>
        body { background: transparent; color: white; font-family: sans-serif; margin: 0; }
        /* Ваши стили здесь */
    </style>
</head>
<body>
    <!-- Ваша разметка -->
    <div id="roshan-timer">Roshan: --:--</div>

    <script>
        const socket = new SockJS('/ws');
        const stompClient = new StompJs.Client({
            webSocketFactory: () => socket,
            reconnectDelay: 3000
        });

        stompClient.onConnect = function () {
            console.log('Connected to Raw Data');
            
            // Подписываемся на сырые данные
            stompClient.subscribe('/topic/raw', function (message) {
                const gsiData = JSON.parse(message.body);
                
                // Передаем данные в вашу функцию отрисовки
                updateCustomOverlay(gsiData);
            });
        };

        stompClient.activate();

        function updateCustomOverlay(data) {
            // Пример: таймер Рошана
            if (data.map && data.map.roshan_timer) {
                // Логика отрисовки
            }
            
            // Пример: проверка предметов у первого игрока Radiant
            if (data.players && data.players.radiant) {
                const firstPlayer = data.players.radiant[0]; // ВАЖНО: массив!
                if (firstPlayer.items) {
                    // У игрока есть предметы в слотах 0-5, stash, teleport
                }
            }
        }
    </script>
</body>
</html>
2. Структура данных (Data Dictionary)
В топик /topic/raw приходит JSON, повторяющий официальную структуру Dota 2 GSI. Ниже описаны основные блоки, доступные в режиме наблюдателя (Spectator).

🗺️ Блок map (Состояние карты)
json
"map": {
"name": "start",
"matchid": "123456789",
"game_time": 1200,          // Игровое время в секундах (-90 до 0 это пик)
"clock_time": 1200,         // Время на часах
"daytime": true,            // День/Ночь
"nightstalker_night": false,// Ночь Ночного Сталкера
"radiant_score": 15,        // Убийства Света
"dire_score": 12,           // Убийства Тьмы
"win_team": "none",         // Какая команда победила
"roshan_state": "alive",    // "alive", "respawning", "dead"
"roshan_state_end_seconds": 0 // Время до следующего Рошана (если respawning)
}
🦸‍♂️ Блок heroes и players (Герои и игроки)
В Dota 2 данные разделены. players содержит никнеймы и экономику, а heroes — состояние конкретного героя (HP, мана, предметы).
Примечание: в JSON ключи команд пишутся с маленькой буквы (radiant, dire).

Блок players.radiant (массив из 5 игроков):

json
{
"player_slot": 0,           // Слот (0-4 для Radiant, 128-132 для Dire)
"name": "PlayerName",
"net_worth": 15000,         // Общая экономика
"kills": 5, "deaths": 2, "assists": 10,
"gpm": 500, "xpm": 600,     // Золото и опыт в минуту
"items": {                  // ID предметов в инвентаре
"slot0": 1, "slot1": 2, "slot2": 3, "slot3": 4, "slot4": 5, "slot5": 6,
"stash0": 7, "stash1": 8,
"teleport0": 0,         // ТП-свиток
"neutral0": 9           // Нейтральный предмет
}
}
Блок heroes (объект с ключами player0...player9):

json
"heroes": {
"player0": {
"name": "npc_dota_hero_axe", // Имя героя для картинок
"level": 15,
"alive": true,
"respawn_seconds": 0,        // Через сколько возродится (если мертв)
"health": 1500,              // Текущее HP
"max_health": 2000,          // Максимальное HP
"health_percent": 75,        // HP в процентах
"mana": 500,
"max_mana": 800,
"mana_percent": 62,
"has_debuff": false,
"silenced": false,
"stunned": false,
"hexed": false
},
// ... player1 по player9
}
📦 Дополнительные блоки GSI
Если в конфиге gamestate_integration_overlay.cfg включены соответствующие флаги ("items" "1", "abilities" "1", "draft" "1"), вам будут доступны:

items: Полные данные о предметах (кулдауны, заряды, можно ли купить, цена). Например, items.teleport0.cooldown.

abilities: Уровни прокачки способностей и их кулдауны для каждого героя (ключ player0 содержит массив ability0, ability1 и т.д.).

draft: Данные стадии пика/бана. draft.team2.pick (массив ID выбранных героев), draft.team3.ban (забаненные герои). Отлично подходит для оверлея пик/бан.

💡 Полезные ссылки и ID для кастомных оверлеев
При создании оверлеев вам понадобятся ID предметов и способностей, так как GSI передает именно их, а не названия.

Картинки героев:
https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/{hero_name}.png
(где {hero_name} — это axe, invoker и т.д., берется из heroes.player0.name, предварительно убрав npc_dota_hero_)

Картинки предметов:
https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/items/{item_name}.png
(однако, GSI передает ID предметов, а не имена. Вам понадобится JSON-маппинг ID предметов (можно найти в открытых источниках Dota 2 API), чтобы переводить ID в имена)

Иконки способностей:
https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/abilities/{ability_name}.png

🚀 Как это использовать
Теперь вы можете создавать неограниченное количество HTML-файлов в папке static.

overlay.html — ваш базовый оверлей со счетом.

roshan.html — маленький виджет только с таймером Рошана.

draft.html — экран пик/бан для стадии драфта.

Каждый из них вы добавляете в OBS как отдельный Browser Source, указывая нужный URL (http://localhost:8080/roshan.html), и они будут работать параллельно, получая данные в реальном времени!