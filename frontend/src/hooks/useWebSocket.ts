import { useState, useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import type { OverlayState, DraftPayload } from '../types';
import { WS_URL } from '../types';

let stompClient: Client | null = null;
const overlaySubscribers: Set<(state: OverlayState) => void> = new Set();
const draftSubscribers: Set<(payload: DraftPayload) => void> = new Set();

function connect() {
  if (stompClient?.active) return;

  const client = new Client({
    brokerURL: `${WS_URL.replace('http', 'ws')}/ws`,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('[WS] Connected');
      client.subscribe('/topic/overlay', (msg) => {
        try {
          const state: OverlayState = JSON.parse(msg.body);
          overlaySubscribers.forEach((cb) => cb(state));
        } catch (e) {
          console.error('[WS] Failed to parse overlay:', e);
        }
      });
      client.subscribe('/topic/draft', (msg) => {
        try {
          const payload: DraftPayload = JSON.parse(msg.body);
          draftSubscribers.forEach((cb) => cb(payload));
        } catch (e) {
          console.error('[WS] Failed to parse draft:', e);
        }
      });
    },
    onWebSocketError: (e) => console.error('[WS] WebSocket error:', e),
    onStompError: (f) => console.error('[WS] STOMP error:', f.headers?.message),
  });

  client.activate();
  stompClient = client;
}

function disconnect() {
  stompClient?.deactivate();
  stompClient = null;
}

export function useOverlayState(): OverlayState | null {
  const [state, setState] = useState<OverlayState | null>(null);

  useEffect(() => {
    connect();
    const handler = (s: OverlayState) => setState(s);
    overlaySubscribers.add(handler);
    return () => {
      overlaySubscribers.delete(handler);
    };
  }, []);

  return state;
}

export function useDraftState(): DraftPayload | null {
  const [payload, setPayload] = useState<DraftPayload | null>(null);

  useEffect(() => {
    connect();
    const handler = (p: DraftPayload) => setPayload(p);
    draftSubscribers.add(handler);
    return () => {
      draftSubscribers.delete(handler);
    };
  }, []);

  return payload;
}

export function useWebSocket() {
  return { connect, disconnect };
}
