package com.sue.overlay.service;

import com.github.mrbean355.dota2.gamestate.SpectatingGameState;
import com.github.mrbean355.dota2.player.SpectatedPlayer;
import com.github.mrbean355.dota2.hero.SpectatedHero;
import com.github.mrbean355.dota2.draft.DraftHero; // Импорт для DraftHero
import com.sue.overlay.model.*;
import com.sue.overlay.repository.GameEventRepository;
import com.sue.overlay.repository.GameSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameStatePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(GameStatePersistenceService.class);

    private final GameSnapshotRepository snapshotRepository;
    private final GameEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Маппинг ID героев в slug для драфта
    private static final Map<Integer, String> HERO_SLUGS = Map.ofEntries(
            Map.entry(1, "antimage"), Map.entry(2, "axe"), Map.entry(3, "bane"), Map.entry(4, "bloodseeker"),
            Map.entry(5, "crystal_maiden"), Map.entry(6, "drow_ranger"), Map.entry(7, "earthshaker"),
            Map.entry(8, "juggernaut"), Map.entry(9, "mirana"), Map.entry(10, "morphling"),
            Map.entry(11, "nevermore"), Map.entry(12, "phantom_lancer"), Map.entry(13, "puck"),
            Map.entry(14, "pudge"), Map.entry(15, "razor"), Map.entry(16, "sand_king"),
            Map.entry(17, "storm_spirit"), Map.entry(18, "sven"), Map.entry(19, "tiny"),
            Map.entry(20, "vengefulspirit"), Map.entry(21, "windrunner"), Map.entry(22, "zuus"),
            Map.entry(23, "kunkka"), Map.entry(25, "lina"), Map.entry(26, "lion"), Map.entry(27, "shadow_shaman"),
            Map.entry(28, "slardar"), Map.entry(29, "tidehunter"), Map.entry(30, "witch_doctor"),
            Map.entry(31, "lich"), Map.entry(32, "riki"), Map.entry(33, "enigma"), Map.entry(34, "tinker"),
            Map.entry(35, "sniper"), Map.entry(36, "necrolyte"), Map.entry(37, "warlock"), Map.entry(38, "beastmaster"),
            Map.entry(39, "queenofpain"), Map.entry(40, "venomancer"), Map.entry(41, "faceless_void"),
            Map.entry(42, "skeleton_king"), Map.entry(43, "death_prophet"), Map.entry(44, "phantom_assassin"),
            Map.entry(45, "pugna"), Map.entry(46, "templar_assassin"), Map.entry(47, "viper"), Map.entry(48, "luna"),
            Map.entry(49, "dragon_knight"), Map.entry(50, "dazzle"), Map.entry(51, "rattletrap"), Map.entry(52, "leshrac"),
            Map.entry(53, "furion"), Map.entry(54, "life_stealer"), Map.entry(55, "dark_seer"), Map.entry(56, "clinkz"),
            Map.entry(57, "omniknight"), Map.entry(58, "enchantress"), Map.entry(59, "huskar"), Map.entry(60, "night_stalker"),
            Map.entry(61, "broodmother"), Map.entry(62, "bounty_hunter"), Map.entry(63, "weaver"), Map.entry(64, "jakiro"),
            Map.entry(65, "batrider"), Map.entry(66, "chen"), Map.entry(67, "spectre"), Map.entry(68, "ancient_apparition"),
            Map.entry(69, "doom_bringer"), Map.entry(70, "ursa"), Map.entry(71, "spirit_breaker"), Map.entry(72, "gyrocopter"),
            Map.entry(73, "alchemist"), Map.entry(74, "invoker"), Map.entry(75, "silencer"), Map.entry(76, "obsidian_destroyer"),
            Map.entry(77, "lycan"), Map.entry(78, "brewmaster"), Map.entry(79, "shadow_demon"), Map.entry(80, "lone_druid"),
            Map.entry(81, "chaos_knight"), Map.entry(82, "meepo"), Map.entry(83, "treant"), Map.entry(84, "ogre_magi"),
            Map.entry(85, "undying"), Map.entry(86, "rubick"), Map.entry(87, "disruptor"), Map.entry(88, "nyx_assassin"),
            Map.entry(89, "naga_siren"), Map.entry(90, "keeper_of_the_light"), Map.entry(91, "wisp"), Map.entry(92, "visage"),
            Map.entry(93, "slark"), Map.entry(94, "medusa"), Map.entry(95, "troll_warlord"), Map.entry(96, "centaur"),
            Map.entry(97, "magnataur"), Map.entry(98, "shredder"), Map.entry(99, "bristleback"), Map.entry(100, "tusk"),
            Map.entry(101, "skywrath_mage"), Map.entry(102, "abaddon"), Map.entry(103, "elder_titan"), Map.entry(104, "legion_commander"),
            Map.entry(105, "techies"), Map.entry(106, "ember_spirit"), Map.entry(107, "earth_spirit"), Map.entry(108, "abyssal_underlord"),
            Map.entry(109, "terrorblade"), Map.entry(110, "phoenix"), Map.entry(111, "oracle"), Map.entry(112, "winter_wyvern"),
            Map.entry(113, "arc_warden"), Map.entry(114, "monkey_king"), Map.entry(115, "dark_willow"), Map.entry(116, "pangolier"),
            Map.entry(117, "grimstroke"), Map.entry(118, "hoodwink"), Map.entry(119, "void_spirit"), Map.entry(120, "snapfire"),
            Map.entry(121, "mars"), Map.entry(122, "dawnbreaker"), Map.entry(123, "marci"), Map.entry(124, "primal_beast"),
            Map.entry(125, "muerta"), Map.entry(126, "ringmaster"), Map.entry(127, "kez")
    );

    public GameStatePersistenceService(GameSnapshotRepository snapshotRepository,
                                       GameEventRepository eventRepository,
                                       SimpMessagingTemplate messagingTemplate) {
        this.snapshotRepository = snapshotRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public void processGameState(SpectatingGameState state) {
        if (state == null || state.getMap() == null) return;

        saveSnapshot(state);

        OverlayState overlayState = buildOverlayState(state);
        messagingTemplate.convertAndSend("/topic/overlay", overlayState);

        // Отправляем данные для драфт-оверлея
        DraftPayload draftPayload = buildDraftPayload(state);
        if (draftPayload != null) {
            messagingTemplate.convertAndSend("/topic/draft", draftPayload);
        }
    }

    private DraftPayload buildDraftPayload(SpectatingGameState state) {
        var draft = state.getDraft();
        if (draft == null) return null;

        int clockTime = state.getMap().getClockTime();
        int activeTeam = draft.getActiveTeam();

        // Во время драфта игровые часы ещё не идут (clock_time == 0), поэтому по знаку clock_time
        // фазу определять нельзя. Драфт идёт, пока кто-то из команд (2 = Radiant, 3 = Dire) делает пик/бан.
        String matchState;
        if (activeTeam == 2 || activeTeam == 3) matchState = "HERO_SELECTION";
        else if (clockTime < 0) matchState = "PRE_GAME";
        else if (clockTime > 0) matchState = "GAME_IN_PROGRESS";
        else matchState = "HERO_SELECTION";

        // Используем radiantDraft и direDraft
        var radDraft = draft.getRadiantDraft();
        var dirDraft = draft.getDireDraft();

        List<DraftPayload.HeroDto> radPicks = extractHeroes(radDraft != null ? radDraft.getPicks() : null);
        List<DraftPayload.HeroDto> radBans = extractHeroes(radDraft != null ? radDraft.getBans() : null);
        List<DraftPayload.HeroDto> dirPicks = extractHeroes(dirDraft != null ? dirDraft.getPicks() : null);
        List<DraftPayload.HeroDto> dirBans = extractHeroes(dirDraft != null ? dirDraft.getBans() : null);

        int activeTime = draft.getActiveTeamTimeRemaining();
        // Резервное время: берём из драфта, если библиотека его отдаёт; иначе стандартные 130 секунд
        int radBonus = readInt(draft, DEFAULT_BONUS_TIME, "getRadiantBonusTime", "getBonusTimeRadiant");
        int direBonus = readInt(draft, DEFAULT_BONUS_TIME, "getDireBonusTime", "getBonusTimeDire");

        DraftPayload.DraftData draftData = new DraftPayload.DraftData(
                activeTeam,
                activeTime,
                radBonus,
                direBonus,
                new DraftPayload.TeamData(radPicks, radBans),
                new DraftPayload.TeamData(dirPicks, dirBans)
        );

        return new DraftPayload(new DraftPayload.MatchInfo(matchState, clockTime), draftData);
    }

    private static final int DEFAULT_BONUS_TIME = 130;

    /** Читает int-геттер через рефлексию (в разных версиях библиотеки поле может называться по-разному). */
    private static int readInt(Object target, int fallback, String... getters) {
        for (String name : getters) {
            try {
                Object v = target.getClass().getMethod(name).invoke(target);
                if (v instanceof Number n) return n.intValue();
            } catch (ReflectiveOperationException ignored) {
                // метода нет — пробуем следующее имя
            }
        }
        return fallback;
    }

    private List<DraftPayload.HeroDto> extractHeroes(List<DraftHero> heroes) {
        if (heroes == null) return new ArrayList<>();
        return heroes.stream()
                .filter(h -> h.getId() > 0)
                .map(h -> {
                    String slug = HERO_SLUGS.getOrDefault(h.getId(), "default");
                    return new DraftPayload.HeroDto(slug, slug);
                })
                .collect(Collectors.toList());
    }

    private void saveSnapshot(SpectatingGameState state) {
        var map = state.getMap();
        if (map == null) return;

        GameSnapshot snapshot = new GameSnapshot(
                null,
                map.getClockTime(),
                map.getGameTime(),
                map.isDaytime(),
                map.getWinningTeam() != null ? map.getWinningTeam().name() : null,
                LocalDateTime.now(ZoneOffset.UTC)
        );
        snapshotRepository.save(snapshot);
    }

    private OverlayState buildOverlayState(SpectatingGameState state) {
        List<HeroState> radiantHeroes = new ArrayList<>();
        List<HeroState> direHeroes = new ArrayList<>();
        double radiantEco = 0;
        double direEco = 0;

        Map<String, List<SpectatedPlayer>> playersMap = state.getPlayers();
        Map<String, SpectatedHero> heroesMap = state.getHeroes();

        if (playersMap != null && heroesMap != null) {
            for (Map.Entry<String, List<SpectatedPlayer>> entry : playersMap.entrySet()) {
                String teamName = entry.getKey();
                List<SpectatedPlayer> teamPlayers = entry.getValue();
                if (teamPlayers == null) continue;

                boolean isRadiant = "radiant".equalsIgnoreCase(teamName) || "team2".equalsIgnoreCase(teamName);

                for (int i = 0; i < teamPlayers.size(); i++) {
                    SpectatedPlayer p = teamPlayers.get(i);
                    int heroIndex = isRadiant ? i : (i + 5);
                    SpectatedHero hero = heroesMap.get("player" + heroIndex);

                    String playerName = p.getName();
                    if (playerName == null || playerName.isBlank() || "null".equals(playerName)) {
                        playerName = "Player " + heroIndex;
                    }

                    double netWorth = p.getNetWorth();
                    HeroState heroState = mapHero(playerName, hero);

                    if (isRadiant) {
                        radiantHeroes.add(heroState);
                        radiantEco += netWorth;
                    } else {
                        direHeroes.add(heroState);
                        direEco += netWorth;
                    }
                }
            }
        }

        var map = state.getMap();
        int radiantScore = map.getRadiantScore();
        int direScore = map.getDireScore();

        return new OverlayState(
                map.getClockTime(),
                map.isDaytime(),
                radiantScore,
                direScore,
                radiantEco,
                direEco,
                radiantHeroes,
                direHeroes
        );
    }

    private HeroState mapHero(String playerName, SpectatedHero hero) {
        int hpPercent = 0;
        int mpPercent = 0;
        int level = 0;
        boolean alive = false;
        int respawnTimer = 0;
        String heroName = "default";

        if (hero != null) {
            heroName = hero.getName();
            hpPercent = hero.getHealthPercent();
            mpPercent = hero.getManaPercent();
            level = hero.getLevel();
            alive = hero.isAlive();
            respawnTimer = hero.getRespawnTimer();
        }

        return new HeroState(
                playerName,
                heroName,
                level,
                hpPercent,
                mpPercent,
                alive,
                respawnTimer
        );
    }
}