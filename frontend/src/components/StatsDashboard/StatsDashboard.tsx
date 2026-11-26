import { useState } from 'react';
import { useAsyncData } from '../../hooks/useAsyncData';
import {
  fetchLatestSnapshots,
  fetchSnapshotStats,
  fetchNightSnapshots,
  fetchSnapshotsByTeam,
  fetchLatestEvents,
  fetchEventTypes,
  fetchEventStats,
} from '../../api/rest';
import { formatClockTime } from '../../utils/helpers';
import type { GameSnapshot, EventStats } from '../../types';
import './StatsDashboard.css';

type Tab = 'overview' | 'snapshots' | 'events' | 'night';

export const StatsDashboard: React.FC = () => {
  const [activeTab, setActiveTab] = useState<Tab>('overview');

  const { data: snapshots, loading: snapshotsLoading } = useAsyncData(
    () => fetchLatestSnapshots(20),
    [activeTab]
  );

  const { data: stats, loading: statsLoading } = useAsyncData(fetchSnapshotStats, [activeTab]);

  const { data: events, loading: eventsLoading } = useAsyncData(
    () => fetchLatestEvents(30),
    [activeTab]
  );

  const { data: eventStats, loading: eventStatsLoading } = useAsyncData(fetchEventStats, [activeTab]);

  const { data: eventTypes } = useAsyncData(fetchEventTypes, [activeTab]);

  const { data: nightSnapshots, loading: nightLoading } = useAsyncData(
    () => fetchNightSnapshots(),
    [activeTab]
  );

  const { data: radiantWins, loading: radLoading } = useAsyncData(
    () => fetchSnapshotsByTeam('radiant'),
    [activeTab]
  );

  const { data: direWins, loading: direLoading } = useAsyncData(
    () => fetchSnapshotsByTeam('dire'),
    [activeTab]
  );

  return (
    <div className="stats-dashboard">
      <div className="dashboard-header">
        <h1>Dota 2 Analytics</h1>
        <p className="subtitle">Статистика и аналитика игровых сессий</p>
      </div>

      <div className="tabs">
        <TabButton active={activeTab === 'overview'} onClick={() => setActiveTab('overview')}>
          Обзор
        </TabButton>
        <TabButton active={activeTab === 'snapshots'} onClick={() => setActiveTab('snapshots')}>
          Снимки
        </TabButton>
        <TabButton active={activeTab === 'events'} onClick={() => setActiveTab('events')}>
          События
        </TabButton>
        <TabButton active={activeTab === 'night'} onClick={() => setActiveTab('night')}>
          Ночные игры
        </TabButton>
      </div>

      <div className="tab-content">
        {activeTab === 'overview' && (
          <OverviewPanel stats={stats} loading={statsLoading} eventStats={eventStats} eventStatsLoading={eventStatsLoading} />
        )}
        {activeTab === 'snapshots' && (
          <SnapshotsPanel snapshots={snapshots as GameSnapshot[]} loading={snapshotsLoading} />
        )}
        {activeTab === 'events' && (
          <EventsPanel events={events} loading={eventsLoading} eventTypes={eventTypes as string[]} eventStats={eventStats as EventStats} eventStatsLoading={eventStatsLoading} />
        )}
        {activeTab === 'night' && (
          <NightPanel nightSnapshots={nightSnapshots as GameSnapshot[]} radiantWins={radiantWins as GameSnapshot[]} direWins={direWins as GameSnapshot[]} loading={nightLoading || radLoading || direLoading} />
        )}
      </div>
    </div>
  );
};

/* ---- Sub-components ---- */

interface TabButtonProps {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}

const TabButton: React.FC<TabButtonProps> = ({ active, onClick, children }) => (
  <button className={`tab-btn ${active ? 'active' : ''}`} onClick={onClick}>
    {children}
  </button>
);

interface OverviewPanelProps {
  stats: any;
  loading: boolean;
  eventStats: EventStats | null;
  eventStatsLoading: boolean;
}

const OverviewPanel: React.FC<OverviewPanelProps> = ({ stats, loading, eventStats, eventStatsLoading }) => {
  if (loading || eventStatsLoading) return <div className="loading-state">Загрузка...</div>;

  return (
    <div className="overview-grid">
      <StatCard title="Всего снимков" value={stats?.total ?? 0} />
      <StatCard title="Макс. время" value={stats?.maxClockTime != null ? formatClockTime(stats.maxClockTime) : '—'} />
      <StatCard title="Ср. время Radiant" value={stats?.avgForRadiant != null ? formatClockTime(stats.avgForRadiant) : '—'} />
      <StatCard title="Ср. время Dire" value={stats?.avgForDire != null ? formatClockTime(stats.avgForDire) : '—'} />

      <div className="event-stats-card">
        <h3>Типы событий</h3>
        {eventStats && Object.keys(eventStats).length > 0 ? (
          <div className="event-type-list">
            {Object.entries(eventStats).map(([type, count]) => (
              <div key={type} className="event-type-item">
                <span className="event-type-name">{type}</span>
                <span className="event-type-count">{count}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="empty-state">Нет данных</p>
        )}
      </div>
    </div>
  );
};

interface SnapshotsPanelProps {
  snapshots: GameSnapshot[] | null;
  loading: boolean;
}

const SnapshotsPanel: React.FC<SnapshotsPanelProps> = ({ snapshots, loading }) => {
  if (loading) return <div className="loading-state">Загрузка...</div>;

  return (
    <div className="data-table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Время</th>
            <th>Day/Night</th>
            <th>Победитель</th>
            <th>Запись</th>
          </tr>
        </thead>
        <tbody>
          {snapshots?.map((s) => (
            <tr key={s.id}>
              <td>{s.id}</td>
              <td>{s.clockTime != null ? formatClockTime(s.clockTime) : '—'}</td>
              <td>{s.daytime === true ? '☀ Day' : s.daytime === false ? '☾ Night' : '—'}</td>
              <td className={`winner-cell ${s.winTeam || ''}`}>{s.winTeam || '—'}</td>
              <td>{new Date(s.capturedAt).toLocaleTimeString()}</td>
            </tr>
          ))}
          {(!snapshots || snapshots.length === 0) && (
            <tr><td colSpan={5} className="empty-state">Нет данных</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

interface EventsPanelProps {
  events: any;
  loading: boolean;
  eventTypes: string[] | null;
  eventStats: EventStats | null;
  eventStatsLoading: boolean;
}

const EventsPanel: React.FC<EventsPanelProps> = ({ events, loading, eventTypes }) => {
  if (loading) return <div className="loading-state">Загрузка...</div>;

  const eventList = Array.isArray(events) ? events : [];

  return (
    <div className="events-panel">
      <div className="event-types-list">
        <h3>Доступные типы</h3>
        {eventTypes?.map((type) => (
          <span key={type} className="event-type-badge">{type}</span>
        ))}
        {(!eventTypes || eventTypes.length === 0) && <p className="empty-state">Нет данных</p>}
      </div>

      <div className="events-list">
        <h3>Последние события</h3>
        {eventList.length > 0 ? (
          <table className="data-table">
            <thead>
              <tr>
                <th>Тип</th>
                <th>GameTime</th>
                <th>Запись</th>
              </tr>
            </thead>
            <tbody>
              {eventList.slice(0, 20).map((e: any) => (
                <tr key={e.id}>
                  <td><span className="event-badge">{e.eventType}</span></td>
                  <td>{formatClockTime(e.gameTime)}</td>
                  <td>{new Date(e.capturedAt).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="empty-state">Нет данных</p>
        )}
      </div>
    </div>
  );
};

interface NightPanelProps {
  nightSnapshots: GameSnapshot[] | null;
  radiantWins: GameSnapshot[] | null;
  direWins: GameSnapshot[] | null;
  loading: boolean;
}

const NightPanel: React.FC<NightPanelProps> = ({ nightSnapshots, radiantWins, direWins, loading }) => {
  if (loading) return <div className="loading-state">Загрузка...</div>;

  const radCount = radiantWins?.length ?? 0;
  const direCount = direWins?.length ?? 0;

  return (
    <div className="night-panel">
      <div className="win-record">
        <div className="team-record radiant">
          <div className="team-icon">☀</div>
          <div className="team-name">Radiant</div>
          <div className="team-wins">{radCount}</div>
        </div>
        <div className="vs">VS</div>
        <div className="team-record dire">
          <div className="team-icon">☾</div>
          <div className="team-name">Dire</div>
          <div className="team-wins">{direCount}</div>
        </div>
      </div>

      {nightSnapshots && nightSnapshots.length > 0 ? (
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Время</th>
                <th>Победитель</th>
                <th>Запись</th>
              </tr>
            </thead>
            <tbody>
              {nightSnapshots.map((s) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.clockTime != null ? formatClockTime(s.clockTime) : '—'}</td>
                  <td className={`winner-cell ${s.winTeam || ''}`}>{s.winTeam || '—'}</td>
                  <td>{new Date(s.capturedAt).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="empty-state">Нет данных о ночных играх</p>
      )}
    </div>
  );
};

const StatCard: React.FC<{ title: string; value: string | number }> = ({ title, value }) => (
  <div className="stat-card">
    <div className="stat-value">{value}</div>
    <div className="stat-title">{title}</div>
  </div>
);
