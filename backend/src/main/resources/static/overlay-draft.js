(() => {
  const WS_URL = `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host || 'localhost:8081'}/ws`;
  const CDN_IMG = 'https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react';
  const LOCAL_VIDEO = '/hero-videos/npc_dota_hero_';
  const LOCAL_IMG = '/hero-images/npc_dota_hero_';

  const DEBUG = new URLSearchParams(location.search).has('debug');
  const S = { ws: null, data: null, config: null };
  const $ = id => document.getElementById(id);

  const fmtTime = s => { if (s == null || isNaN(s)) return '0:00'; s = Math.abs(Math.round(s)); return Math.floor(s / 60) + ':' + String(s % 60).padStart(2, '0'); };
  const normalizeState = s => String(s || '').toUpperCase().replace(/[_\s]/g, '');

  function shouldShow() {
    if (DEBUG && S.data?.draft) return true; // ?debug — показывать всегда, когда пришёл драфт
    if (S.data?.mode !== 'SPECTATING') return false;
    const d = S.data?.draft;
    const m = S.data?.match;
    const st = normalizeState(m?.state);

    if (st.includes('GAMEINPROGRESS') || st.includes('POSTGAME') || st.includes('STRATEGYTIME') || st.includes('PREGAME')) return false;
    if (st.includes('HEROSELECTION')) return !!d;
    if (!d) return false;

    const has = (d.radiant?.picks?.length || 0) + (d.dire?.picks?.length || 0) + (d.radiant?.bans?.length || 0) + (d.dire?.bans?.length || 0);
    if (has === 0 && d.activeTeam !== 2 && d.activeTeam !== 3) return false;
    if (m?.gameTime != null && m.gameTime > 0) return false;
    return true;
  }

  function render() {
    const bar = $('draft-bar');
    if (!shouldShow()) { bar.style.display = 'none'; return; }
    bar.style.display = 'flex';

    const d = S.data.draft;
    const cfg = S.config || {};

    if (cfg.radiant?.color) document.documentElement.style.setProperty('--radiant', cfg.radiant.color);
    if (cfg.dire?.color) document.documentElement.style.setProperty('--dire', cfg.dire.color);

    $('draft-stage').textContent = cfg.stage || 'DRAFT';

    const tRadTag = $('draft-radiant-tag');
    const tDirTag = $('draft-dire-tag');
    const tBox = $('timer-box-left');
    const turnArrow = $('turn-arrow');

    const radName = cfg.radiant?.name || cfg.radiant?.tag || 'RADIANT';
    const dirName = cfg.dire?.name || cfg.dire?.tag || 'DIRE';

    fitTeamName(tRadTag, radName);
    fitTeamName(tDirTag, dirName);

    const bestOf = cfg.series?.bestOf ?? 3;
    $('draft-series-format').textContent = `до ${Math.ceil(bestOf / 2)} побед`;
    $('draft-score').textContent = `${cfg.series?.radiantWins ?? 0} : ${cfg.series?.direWins ?? 0}`;

    $('draft-timer').textContent = fmtTime(d.activeTimeRemaining || 0);

    tBox.classList.remove('active');
    turnArrow.className = 'turn-arrow';
    let reserveSec = 0;

    if (d.activeTeam === 2) {
      tBox.classList.add('active');
      reserveSec = d.radiantBonusTime || 0;
      turnArrow.textContent = '◀';
      turnArrow.classList.add('active-left');
    } else if (d.activeTeam === 3) {
      tBox.classList.add('active');
      reserveSec = d.direBonusTime || 0;
      turnArrow.textContent = '▶';
      turnArrow.classList.add('active-right');
    }

    $('draft-reserve').textContent = fmtTime(reserveSec);

    const radPicks = d.radiant?.picks || [];
    for (let i = 0; i < 5; i++) updateHeroSlot(`rad-pick-${i}`, radPicks[i]);

    const dirPicks = d.dire?.picks || [];
    for (let i = 0; i < 5; i++) updateHeroSlot(`dire-pick-${i}`, dirPicks[i]);

    renderBans('rad-bans', d.radiant?.bans, 7);
    renderBans('dire-bans', d.dire?.bans, 7);
  }

  function fitTeamName(el, text) {
    if (el.textContent === text && el.style.fontSize) return;
    el.textContent = text;
    el.style.fontSize = '30px';

    const temp = document.createElement('span');
    temp.style.font = getComputedStyle(el).font;
    temp.style.visibility = 'hidden';
    temp.style.position = 'absolute';
    temp.style.whiteSpace = 'nowrap';
    temp.textContent = text;
    document.body.appendChild(temp);

    const textWidth = temp.offsetWidth;
    document.body.removeChild(temp);

    const available = el.clientWidth;

    if (textWidth > available) {
      const newSize = Math.max(14, Math.floor(30 * (available / textWidth)));
      el.style.fontSize = newSize + 'px';
    }
  }

  function updateHeroSlot(slotId, hero) {
    const slot = $(slotId);
    const slug = hero?.slug || '';
    const currentSlug = slot.dataset.slug || '';

    if (currentSlug === slug) return;

    slot.dataset.slug = slug;
    slot.title = hero?.name || slug || '';

    if (!slug) {
      slot.innerHTML = '';
      return;
    }

    const cdnSrc = `${CDN_IMG}/heroes/${slug}.png`;
    const localImg = `${LOCAL_IMG}${slug}_png.png`;

    // Запасной вариант: картинка (локальная -> CDN Valve)
    const showImage = () => {
      const img = new Image();
      img.alt = slug;
      img.onerror = () => { img.onerror = null; img.src = cdnSrc; };
      img.src = localImg;
      slot.replaceChildren(img);
    };

    // Основной вариант: локальное видео; если его нет — картинка
    const video = document.createElement('video');
    video.autoplay = true; video.loop = true; video.muted = true; video.playsInline = true;
    video.preload = 'auto';
    video.poster = localImg;
    video.onerror = showImage;
    const source = document.createElement('source');
    source.src = `${LOCAL_VIDEO}${slug}.webm`;
    source.type = 'video/webm';
    source.onerror = showImage; // у <video> ошибка загрузки source приходит на самом <source>
    video.appendChild(source);
    slot.replaceChildren(video);
  }

  function renderBans(containerId, bans, total) {
    const box = $(containerId);
    const arr = bans || [];

    if (box.children.length !== total) {
      box.innerHTML = '';
      for (let i = 0; i < total; i++) {
        const banSlot = document.createElement('div');
        banSlot.className = 'ban-slot';
        box.appendChild(banSlot);
      }
    }

    for (let i = 0; i < total; i++) {
      const banSlot = box.children[i];
      const hero = arr[i];
      const slug = hero?.slug || '';

      if (banSlot.dataset.slug === slug) continue;

      banSlot.dataset.slug = slug;
      banSlot.title = hero?.name || slug || '';

      if (slug) {
        const localImg = `${LOCAL_IMG}${slug}_png.png`;
        const cdnImg = `${CDN_IMG}/heroes/${slug}.png`;

        let img = banSlot.querySelector('img');
        if (!img) {
          img = document.createElement('img');
          banSlot.appendChild(img);
        }
        img.src = localImg;
        img.onerror = () => { img.onerror = null; img.src = cdnImg; };
      } else {
        banSlot.innerHTML = '';
      }
    }
  }

       function connect() {
         // Используем нативный WebSocket (без SockJS)
         const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws';
         const wsUrl = `${wsProtocol}://${location.host || 'localhost:8080'}/ws`;

         const stompClient = new StompJs.Client({
           brokerURL: wsUrl,
           reconnectDelay: 3000
         });

         stompClient.onConnect = () => {
           console.log('[draft] WebSocket подключён:', wsUrl);

           stompClient.subscribe('/topic/draft', (message) => {
             try {
               S.data = JSON.parse(message.body);
               if (DEBUG) console.log('[draft] получено:', S.data, 'показывать:', shouldShow());
               render();
             } catch (e) {
               console.error("Ошибка парсинга JSON:", e);
             }
           });
         };

         stompClient.onWebSocketError = (e) => console.error('[draft] ошибка WebSocket', wsUrl, e);
         stompClient.onStompError = (f) => console.error('[draft] ошибка STOMP', f.headers?.message, f.body);
         stompClient.activate();
       }

       render();
       connect();
     })();