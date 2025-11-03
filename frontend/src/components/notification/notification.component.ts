import { Component, Input, OnDestroy, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { NotificationWebSocketService } from '../../services/notification-websocket.service';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification',
  templateUrl: './notification.component.html',
  styleUrls: ['./notification.component.scss'],
  standalone: true,
  imports: [CommonModule],
})
export class NotificationComponent implements OnInit, OnDestroy, OnChanges {
  @Input() userId: number | null = null;
  @Input() inAppEnabled: boolean = true;
  notifications: any[] = [];
  showDropdown = false;
  showToast = false;
  toastMessage = '';
  private wsSub?: Subscription;

  constructor(
    private notifWebSocketService: NotificationWebSocketService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.tryInitialize();
  }

  ngOnChanges(changes: SimpleChanges) {
    // Si userId change, on réinitialise tout
    if (changes['userId'] && changes['userId'].currentValue !== changes['userId'].previousValue) {
      this.ngOnDestroy(); // Déconnecte l'ancien abonnement
      this.tryInitialize();
    }
  }

  private tryInitialize() {
    if (this.userId == null) return;

    // 1. Charger les préférences (inAppEnabled)
    this.http.get<any>(`/api/user-notification-preference/${this.userId}`).subscribe({
      next: pref => {
        this.inAppEnabled = pref?.inAppEnabled ?? true;

        // 2. Charger les notifications (historique)
        this.http.get<any[]>(`/api/notifications/${this.userId}`).subscribe({
          next: n => {
            this.notifications = (n || []).map(notif => ({
              ...notif,
              createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date()
            }));
          },
          error: err => console.error('Failed to load notifications:', err)
        });

        // 3. WebSocket : abonnement live (synchronisé avec la préférence)
        this.notifWebSocketService.connect((this.userId ?? 0) as number);

        this.wsSub?.unsubscribe();
        this.wsSub = this.notifWebSocketService.notifications().subscribe({
          next: (notif: any) => {
            // Eviter les doublons si la notif existe déjà (même id)
            if (notif.id && this.notifications.some(n => n.id === notif.id)) return;

            // Ajoute la notification en haut
            this.notifications.unshift({
              ...notif,
              message: notif.message ?? JSON.stringify(notif),
              createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
              read: notif.read ?? false,
              id: notif.id ?? Math.random()
            });

            // Affiche le toast uniquement si l'in-app est activé
            if (this.inAppEnabled) {
              this.toastMessage = notif.message;
              this.showToast = true;
              setTimeout(() => this.showToast = false, 5500);
            }
          },
          error: err => console.error('WebSocket subscription error:', err)
        });
      },
      error: err => {
        console.error('Failed to load notification preferences:', err);
        // On continue quand même mais inAppEnabled à true
        this.inAppEnabled = true;
        this.notifWebSocketService.connect((this.userId ?? 0) as number);
      }
    });
  }

  toggleDropdown() {
    this.showDropdown = !this.showDropdown;
  }

  onNotificationClick(notification: any) {
    this.markAsRead(notification);
    // Tu peux ajouter une redirection ou une action ici selon le type de notif
  }

  markAsRead(notification: any) {
    if (!notification?.id || notification.read) return; // Déjà lu
    this.http.post(`/api/notifications/read/${notification.id}`, null).subscribe({
      next: () => notification.read = true,
      error: err => console.error('Failed to mark as read:', err)
    });
  }

  unreadCount() {
    return this.notifications.filter(n => !n.read).length;
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
    this.notifWebSocketService.disconnect();
  }
}
