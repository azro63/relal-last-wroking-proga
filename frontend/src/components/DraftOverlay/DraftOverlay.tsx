import { useDraftState } from '../../hooks/useWebSocket';
import { HERO_CDN } from '../../types';
import './DraftOverlay.css';

export const DraftOverlay: React.FC = () => {
  const data = useDraftState();

  if (!data?.draft) {
    return (
      <div className="draft-overlay">
        <div className="loading-state">
          <div className="spinner" />
          <p>Ожидание драфта...</p>
        </div>
      </div>
    );
  }

  const { draft } = data;
  const radPicks = draft.radiant?.picks || [];
  const dirPicks = draft.dire?.picks || [];
  const radBans = draft.radiant?.bans || [];
  const dirBans = draft.dire?.bans || [];

  const activeTeamLabel = draft.activeTeam === 2 ? 'RADIANT' : draft.activeTeam === 3 ? 'DIRE' : '—';
  const activeTeamColor = draft.activeTeam === 2 ? '#00e5ff' : draft.activeTeam === 3 ? '#ff3355' : '#888';

  return (
    <div className="draft-overlay">
      {/* Stage label */}
      <div className="draft-stage">DRAFT</div>

      {/* Main content */}
      <div className="draft-main">
        {/* Radiant */}
        <div className="team-panel radiant">
          <div className="team-label">RADIANT</div>
          <div className="picks-row">
            {Array.from({ length: 5 }).map((_, i) => (
              <HeroSlot key={`rad-p-${i}`} hero={radPicks[i]} type="pick" />
            ))}
          </div>
          <div className="bans-row">
            {Array.from({ length: 7 }).map((_, i) => (
              <BanSlot key={`rad-b-${i}`} hero={radBans[i]} />
            ))}
          </div>
        </div>

        {/* Center */}
        <div className="center-panel">
          <div className="turn-indicator" style={{ color: activeTeamColor }}>
            {draft.activeTeam === 2 ? '◀' : draft.activeTeam === 3 ? '▶' : ''}
            <span style={{ fontSize: '12px', marginLeft: '4px' }}>{activeTeamLabel}</span>
          </div>

          <div className="timer-section">
            <div className="timer-label">ЗАПРЕТ / ПИК</div>
            <div className="timer-value">{formatTime(draft.activeTimeRemaining)}</div>
          </div>
          <div className="timer-section">
            <div className="timer-label">РЕЗЕРВ</div>
            <div className="timer-value">{draft.activeTeam === 2 ? formatTime(draft.radiantBonusTime) : formatTime(draft.direBonusTime)}</div>
          </div>
        </div>

        {/* Dire */}
        <div className="team-panel dire">
          <div className="team-label">DIRE</div>
          <div className="picks-row">
            {Array.from({ length: 5 }).map((_, i) => (
              <HeroSlot key={`dir-p-${i}`} hero={dirPicks[i]} type="pick" />
            ))}
          </div>
          <div className="bans-row">
            {Array.from({ length: 7 }).map((_, i) => (
              <BanSlot key={`dir-b-${i}`} hero={dirBans[i]} />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

function formatTime(seconds: number): string {
  const abs = Math.abs(Math.round(seconds));
  const mins = Math.floor(abs / 60);
  const secs = abs % 60;
  return `${mins}:${String(secs).padStart(2, '0')}`;
}

function HeroSlot({ hero, type }: { hero?: { slug: string; name: string }; type: 'pick' | 'ban' }) {
  if (!hero?.slug) {
    return <div className={`hero-slot ${type}`} />;
  }

  const slug = hero.slug.replace('npc_dota_hero_', '');
  const src = `${HERO_CDN}/heroes/${slug}.png`;

  return (
    <div className={`hero-slot ${type}`} title={hero.name}>
      <img src={src} alt={hero.name} loading="lazy" />
    </div>
  );
}

function BanSlot({ hero }: { hero?: { slug: string; name: string } }) {
  if (!hero?.slug) {
    return <div className="ban-slot" />;
  }

  const slug = hero.slug.replace('npc_dota_hero_', '');
  const src = `${HERO_CDN}/heroes/${slug}.png`;

  return (
    <div className="ban-slot" title={hero.name}>
      <img src={src} alt={hero.name} loading="lazy" />
    </div>
  );
}
