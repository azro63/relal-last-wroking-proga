import { useOverlayState } from '../../hooks/useWebSocket';
import { HeroCard } from './HeroCard';
import { formatClockTime, formatEconomy } from '../../utils/helpers';
import './GameOverlay.css';

export const GameOverlay: React.FC = () => {
  const state = useOverlayState();

  if (!state) {
    return (
      <div className="game-overlay">
        <div className="loading-state">
          <div className="spinner" />
          <p>Ожидание данных Dota 2...</p>
        </div>
      </div>
    );
  }

  const totalEconomy = state.radiantEconomy + state.direEconomy;
  const radiantShare = totalEconomy > 0 ? (state.radiantEconomy / totalEconomy * 100) : 50;
  const direShare = totalEconomy > 0 ? (state.direEconomy / totalEconomy * 100) : 50;

  return (
    <div className="game-overlay">
      {/* Top bar: clock + scores */}
      <div className="overlay-top-bar">
        <div className="team-score radiant">
          <span className="team-label">RADIANT</span>
          <span className="score-value">{state.radiantScore}</span>
        </div>
        <div className="clock-display">
          <span className={`day-night-icon ${state.daytime ? 'day' : 'night'}`}>
            {state.daytime ? '☀' : '☾'}
          </span>
          <span className="clock-time">{formatClockTime(state.gameTime)}</span>
        </div>
        <div className="team-score dire">
          <span className="team-label">DIRE</span>
          <span className="score-value">{state.direScore}</span>
        </div>
      </div>

      {/* Economy bars */}
      <div className="overlay-economy">
        <div className="economy-bar radiant">
          <div className="economy-fill" style={{ width: `${radiantShare}%` }}>
            <span className="economy-value">{formatEconomy(state.radiantEconomy)}</span>
          </div>
        </div>
        <div className="economy-bar dire">
          <div className="economy-fill" style={{ width: `${direShare}%` }}>
            <span className="economy-value">{formatEconomy(state.direEconomy)}</span>
          </div>
        </div>
      </div>

      {/* Heroes */}
      <div className="overlay-heroes">
        <div className="team-heroes radiant">
          <div className="team-header radiant">RADIANT</div>
          {state.radiantHeroes.map((hero, i) => (
            <HeroCard key={i} hero={hero} team="radiant" />
          ))}
        </div>
        <div className="team-heroes dire">
          <div className="team-header dire">DIRE</div>
          {state.direHeroes.map((hero, i) => (
            <HeroCard key={i} hero={hero} team="dire" />
          ))}
        </div>
      </div>
    </div>
  );
};
