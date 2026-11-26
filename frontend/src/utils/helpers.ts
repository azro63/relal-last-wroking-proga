export function formatClockTime(seconds: number): string {
  const abs = Math.abs(Math.round(seconds));
  const mins = Math.floor(abs / 60);
  const secs = abs % 60;
  const sign = seconds < 0 ? '-' : '';
  return `${sign}${mins}:${String(secs).padStart(2, '0')}`;
}

export function formatEconomy(value: number): string {
  return Math.round(value).toLocaleString('en-US');
}

export function getHeroImageUrl(heroName: string): string {
  const slug = heroName.replace('npc_dota_hero_', '');
  return `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/${slug}.png`;
}

export function getHeroIconUrl(heroName: string): string {
  const slug = heroName.replace('npc_dota_hero_', '');
  return `https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/icons/${slug}_logo.png`;
}

export function hpColor(percent: number): string {
  if (percent > 60) return '#4caf50';
  if (percent > 30) return '#ff9800';
  return '#f44336';
}

export function manaColor(percent: number): string {
  if (percent > 60) return '#2196f3';
  if (percent > 30) return '#ff9800';
  return '#f44336';
}
