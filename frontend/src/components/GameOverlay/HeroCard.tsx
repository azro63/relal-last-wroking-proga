import React from 'react';
import type { HeroState } from '../../types';
import { getHeroImageUrl, hpColor, manaColor } from '../../utils/helpers';
import './HeroCard.css';

interface HeroCardProps {
  hero: HeroState;
  team: 'radiant' | 'dire';
  compact?: boolean;
}

export const HeroCard: React.FC<HeroCardProps> = ({ hero, team, compact = false }) => {
  const heroSlug = hero.heroName.replace('npc_dota_hero_', '');
  const hp = hpColor(hero.hpPercent);
  const mp = manaColor(hero.manaPercent);

  return (
    <div className={`hero-card ${team} ${!hero.alive ? 'dead' : ''}`} data-hero={heroSlug}>
      <div className="hero-avatar">
        <img src={getHeroImageUrl(hero.heroName)} alt={hero.name} loading="lazy" />
        {!hero.alive && <div className="death-icon">💀</div>}
      </div>
      <div className="hero-info">
        <div className="hero-name">{hero.name}</div>
        <div className="hero-level">Lv.{hero.level}</div>
        {!compact && (
          <>
            <div className="hero-bars">
              <div className="bar hp-bar">
                <div className="bar-fill" style={{ width: `${hero.hpPercent}%`, backgroundColor: hp }} />
              </div>
              <div className="bar mana-bar">
                <div className="bar-fill" style={{ width: `${hero.manaPercent}%`, backgroundColor: mp }} />
              </div>
            </div>
            <div className="hero-percentages">
              <span className="hp-text" style={{ color: hp }}>{hero.hpPercent}%</span>
              <span className="mp-text" style={{ color: mp }}>{hero.manaPercent}%</span>
            </div>
          </>
        )}
        {hero.alive && hero.respawnTimer > 0 && (
          <div className="respawn-timer">Respawn: {hero.respawnTimer}s</div>
        )}
      </div>
    </div>
  );
};
