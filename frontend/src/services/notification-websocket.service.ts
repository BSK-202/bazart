import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationWebSocketService {
  private stompClient?: Client;
  private notificationSubject = new Subject<any>();
  private connected = false;
  private currentUserId: number | null = null;

  // Expose an observable for components to subscribe
  notifications(): Observable<any> {
    return this.notificationSubject.asObservable();
  }

  connect(userId: number) {
    if (!userId) {
      console.warn('NotificationWebSocketService.connect() called with invalid userId:', userId);
      return;
    }

    // If already connected to same user, do nothing
    if (this.connected && this.currentUserId === userId) {
      console.log('NotificationWebSocketService: already connected for user', userId);
      return;
    }

    // If connecting for another user, disconnect first
    if (this.connected && this.currentUserId !== userId) {
      this.disconnect();
    }

    this.currentUserId = userId;

    // Create STOMP client using SockJS fallback
    this.stompClient = new Client({
      brokerURL: undefined, // must be undefined when using webSocketFactory
      webSocketFactory: () => new SockJS('http://localhost:8080/ws-notif'),
      reconnectDelay: 5000,
      // Optional heartbeat settings:
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20000,
      // debug output to console
      debug: (msg) => console.debug('[STOMP]', msg)
    });

    this.stompClient.onConnect = (frame) => {
      console.log('STOMP connected for user', userId, frame);
      this.connected = true;

      try {
        const topic = `/topic/notifications/${userId}`;
        console.log('Subscribing to', topic);
        this.stompClient?.subscribe(topic, (message: Message) => {
          try {
            const body = message.body ? JSON.parse(message.body) : null;
            console.log('STOMP message received:', body);
            this.notificationSubject.next(body);
          } catch (err) {
            console.error('Failed to parse STOMP message body:', err, 'raw:', message.body);
            this.notificationSubject.next({ raw: message.body });
          }
        });
      } catch (err) {
        console.error('Subscription error:', err);
      }
    };

    this.stompClient.onStompError = (frame) => {
      console.error('STOMP protocol error:', frame && (frame as any).message, frame);
    };

    // websocket-level errors
    (this.stompClient as any).onWebSocketError = (ev: any) => {
      console.error('WebSocket error event:', ev);
    };

    this.stompClient.onDisconnect = (frame) => {
      console.log('STOMP disconnected', frame);
      this.connected = false;
    };

    console.log('Activating STOMP client (userId=', userId, ')');
    this.stompClient.activate();
  }

  disconnect() {
    if (!this.stompClient) return;
    try {
      console.log('Deactivating STOMP client for user', this.currentUserId);
      this.stompClient.deactivate();
    } catch (err) {
      console.error('Error during stompClient.deactivate():', err);
    } finally {
      this.connected = false;
      this.stompClient = undefined;
      this.currentUserId = null;
    }
  }
}
