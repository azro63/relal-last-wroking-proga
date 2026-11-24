package com.sue.overlay.model;

import java.util.List;

public class DraftPayload {
    public String mode = "SPECTATING";
    public MatchInfo match;
    public DraftData draft;

    public DraftPayload(MatchInfo match, DraftData draft) {
        this.match = match;
        this.draft = draft;
    }

    public record MatchInfo(String state, int gameTime) {}

    public record DraftData(
            int activeTeam,
            int activeTimeRemaining,
            int radiantBonusTime,
            int direBonusTime,
            TeamData radiant,
            TeamData dire
    ) {}

    public record TeamData(List<HeroDto> picks, List<HeroDto> bans) {}

    public record HeroDto(String slug, String name) {}
}