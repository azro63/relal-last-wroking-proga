import { API_BASE } from '../types';

export async function fetchLatestEvents(limit = 20) {
  const res = await fetch(`${API_BASE}/api/events/latest?n=${limit}`);
  if (!res.ok) throw new Error(`Failed to fetch events: ${res.status}`);
  return res.json();
}

export async function fetchEventTypes() {
  const res = await fetch(`${API_BASE}/api/events/types`);
  if (!res.ok) throw new Error(`Failed to fetch event types: ${res.status}`);
  return res.json();
}

export async function fetchEventsByType(type: string, limit = 50) {
  const res = await fetch(`${API_BASE}/api/events/${type}?limit=${limit}`);
  if (!res.ok) throw new Error(`Failed to fetch events: ${res.status}`);
  return res.json();
}

export async function fetchEventStats() {
  const res = await fetch(`${API_BASE}/api/events/stats`);
  if (!res.ok) throw new Error(`Failed to fetch event stats: ${res.status}`);
  return res.json();
}

export async function fetchLatestSnapshots(limit = 10) {
  const res = await fetch(`${API_BASE}/api/snapshots/latest?n=${limit}`);
  if (!res.ok) throw new Error(`Failed to fetch snapshots: ${res.status}`);
  return res.json();
}

export async function fetchSnapshotStats() {
  const res = await fetch(`${API_BASE}/api/snapshots/stats`);
  if (!res.ok) throw new Error(`Failed to fetch snapshot stats: ${res.status}`);
  return res.json();
}

export async function fetchNightSnapshots() {
  const res = await fetch(`${API_BASE}/api/snapshots/night`);
  if (!res.ok) throw new Error(`Failed to fetch night snapshots: ${res.status}`);
  return res.json();
}

export async function fetchSnapshotsByTeam(team: string) {
  const res = await fetch(`${API_BASE}/api/snapshots/by-team?team=${team}`);
  if (!res.ok) throw new Error(`Failed to fetch snapshots: ${res.status}`);
  return res.json();
}
