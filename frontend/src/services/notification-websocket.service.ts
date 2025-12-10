import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class NotificationWebSocketService {
  private stompClient: Client | null = null;
  private notificationSubject = new BehaviorSubject<any>(null);
  private isConnected = false;

  constructor() {}

  connect(userId: number) {
    if (this.isConnected) {
      console.log('✅ Déjà connecté pour user:', userId);
      return;
    }

    console.log('🔄 Tentative de connexion WebSocket pour user:', userId);

    // Configuration STOMP
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws-notif'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log('STOMP:', str),
    });

    this.stompClient.onConnect = (frame) => {
      console.log('✅ STOMP connecté avec succès pour user', userId, frame);
      this.isConnected = true;

      // S'abonner au bon topic
      const subscription = this.stompClient!.subscribe(
        `/topic/notifications/${userId}`,
        (message: IMessage) => {
          console.log('📨 Notification reçue:', message.body);
          try {
            const notification = JSON.parse(message.body);
            this.notificationSubject.next(notification);
          } catch (e) {
            console.error('❌ Erreur parsing notification:', e);
          }
        }
      );

      console.log('✅ Abonné au topic:', `/topic/notifications/${userId}`);
    };

    this.stompClient.onStompError = (frame) => {
      console.error('❌ Erreur STOMP:', frame.headers['message'], frame.body);
      this.isConnected = false;
    };

    this.stompClient.onWebSocketError = (event) => {
      console.error('❌ Erreur WebSocket:', event);
      this.isConnected = false;
    };

    this.stompClient.onDisconnect = () => {
      console.log('🔌 Déconnecté WebSocket');
      this.isConnected = false;
    };

    // Activer la connexion
    this.stompClient.activate();
  }

  notifications(): Observable<any> {
    return this.notificationSubject.asObservable();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.isConnected = false;
      console.log('🔌 WebSocket déconnecté');
    }
  }

  // Méthode pour vérifier l'état de la connexion
  isConnected$(): boolean {
    return this.isConnected;
  }
}
