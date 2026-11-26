import { useState } from 'react';
import { GameOverlay } from './components/GameOverlay/GameOverlay';
import { StatsDashboard } from './components/StatsDashboard/StatsDashboard';
import { DraftOverlay } from './components/DraftOverlay/DraftOverlay';
import './App.css';

type Page = 'overlay' | 'dashboard' | 'draft';

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('overlay');

  return (
    <div className="app">
      <nav className="app-nav">
        <button
          className={`nav-btn ${currentPage === 'overlay' ? 'active' : ''}`}
          onClick={() => setCurrentPage('overlay')}
        >
          🎮 Overlay
        </button>
        <button
          className={`nav-btn ${currentPage === 'draft' ? 'active' : ''}`}
          onClick={() => setCurrentPage('draft')}
        >
          📋 Draft
        </button>
        <button
          className={`nav-btn ${currentPage === 'dashboard' ? 'active' : ''}`}
          onClick={() => setCurrentPage('dashboard')}
        >
          📊 Dashboard
        </button>
      </nav>

      <main className="app-main">
        {currentPage === 'overlay' && <GameOverlay />}
        {currentPage === 'draft' && <DraftOverlay />}
        {currentPage === 'dashboard' && <StatsDashboard />}
      </main>
    </div>
  );
}

export default App;
