import type { WsSeatEvent } from '@/types';

type SeatEventHandler = (event: WsSeatEvent) => void;

const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/ws';

class WebSocketManager {
  private socket: WebSocket | null = null;
  private handlers: Map<string, Set<SeatEventHandler>> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectDelay = 1000;
  private eventId: string | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;

  connect(eventId: string) {
    if (this.socket?.readyState === WebSocket.OPEN && this.eventId === eventId) {
      return;
    }

    this.disconnect();
    this.eventId = eventId;

    const token = typeof window !== 'undefined' ? localStorage.getItem('access_token') : null;
    const url = `${WS_BASE_URL}/seats/${eventId}${token ? `?token=${token}` : ''}`;

    try {
      this.socket = new WebSocket(url);

      this.socket.onopen = () => {
        this.reconnectAttempts = 0;
        this.startHeartbeat();
        console.log(`[WS] Connected to seat updates for event ${eventId}`);
      };

      this.socket.onmessage = (event) => {
        try {
          const data: WsSeatEvent = JSON.parse(event.data);
          this.notifyHandlers(data.type, data);
          this.notifyHandlers('*', data);
        } catch (e) {
          console.error('[WS] Failed to parse message:', e);
        }
      };

      this.socket.onclose = (event) => {
        this.stopHeartbeat();
        if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
          console.log(`[WS] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})`);
          setTimeout(() => {
            if (this.eventId) this.connect(this.eventId);
          }, delay);
        }
      };

      this.socket.onerror = (error) => {
        console.error('[WS] Error:', error);
      };
    } catch (error) {
      console.error('[WS] Failed to connect:', error);
    }
  }

  disconnect() {
    this.stopHeartbeat();
    if (this.socket) {
      this.socket.close(1000, 'Client disconnect');
      this.socket = null;
    }
    this.eventId = null;
    this.reconnectAttempts = 0;
  }

  subscribe(eventType: string, handler: SeatEventHandler) {
    if (!this.handlers.has(eventType)) {
      this.handlers.set(eventType, new Set());
    }
    this.handlers.get(eventType)!.add(handler);

    return () => {
      this.handlers.get(eventType)?.delete(handler);
    };
  }

  private notifyHandlers(eventType: string, data: WsSeatEvent) {
    this.handlers.get(eventType)?.forEach((handler) => handler(data));
  }

  private startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: 'PING' }));
      }
    }, 30000);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  get isConnected() {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}

export const wsManager = new WebSocketManager();
