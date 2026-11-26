// === WebSocket Overlay State ===

export interface HeroState {
  name: string;
  heroName: string; // npc_dota_hero_axe
  level: number;
  hpPercent: number;
  manaPercent: number;
  alive: boolean;
  respawnTimer: number;
}

export interface OverlayState {
  gameTime: number;
  daytime: boolean;
  radiantScore: number;
  direScore: number;
  radiantEconomy: number;
  direEconomy: number;
  radiantHeroes: HeroState[];
  direHeroes: HeroState[];
}

// === Draft State ===

export interface DraftHeroDto {
  slug: string;
  name: string;
}

export interface DraftTeamData {
  picks: DraftHeroDto[];
  bans: DraftHeroDto[];
}

export interface DraftData {
  activeTeam: number; // 2 = radiant, 3 = dire
  activeTimeRemaining: number;
  radiantBonusTime: number;
  direBonusTime: number;
  radiant: DraftTeamData;
  dire: DraftTeamData;
}

export interface MatchInfo {
  state: string;
  gameTime: number;
}

export interface DraftPayload {
  mode: string;
  match: MatchInfo;
  draft: DraftData;
}

// === REST API Types ===

export interface GameEvent {
  id: number;
  eventType: string;
  gameTime: number;
  payload: string;
  capturedAt: string;
}

export interface GameSnapshot {
  id: number;
  clockTime: number;
  gameTime: number | null;
  daytime: boolean | null;
  winTeam: string | null;
  capturedAt: string;
}

export interface SnapshotStats {
  total: number;
  maxClockTime: number | null;
  avgForRadiant: number | null;
  avgForDire: number | null;
}

export interface EventStats {
  [eventType: string]: number;
}

// === Config ===

export const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';
export const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080';
export const HERO_CDN = 'https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react';
