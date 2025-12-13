import { Component, Input, OnDestroy, OnInit, OnChanges, SimpleChanges } from "@angular/core"
import { NotificationWebSocketService } from "../../services/notification-websocket.service"
import { HttpClient } from "@angular/common/http"
import { CommonModule } from "@angular/common"
import { Subscription } from "rxjs"
import {Router} from '@angular/router';

@Component({
  selector: "app-notification",
  templateUrl: "./notification.component.html",
  styleUrls: ["./notification.component.scss"],
  standalone: true,
  imports: [CommonModule],
})
export class NotificationComponent implements OnInit, OnDestroy, OnChanges {
  @Input() userId: number | null = null
  @Input() inAppEnabled = true
  notifications: any[] = []
  showToast = false
  toastMessage = ""
  selectedNotification: any = null
  showNotificationsSidebar = false
  private wsSub?: Subscription
  private preferencesLoaded = false

  constructor(
    private notifWebSocketService: NotificationWebSocketService,
    private http: HttpClient,
    private router: Router, // ← Ajouter cette ligne
  ) {}

  ngOnInit() {
    this.tryInitialize()
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes["userId"] && changes["userId"].currentValue !== changes["userId"].previousValue) {
      this.ngOnDestroy()
      this.tryInitialize()
    }
  }

  private tryInitialize() {
    if (this.userId == null) {
      console.log('❌ UserId est null, impossible d\'initialiser les notifications');
      return;
    }

    console.log('🔄 Initialisation des notifications pour user:', this.userId);

    // Charger les préférences utilisateur
    this.http.get<any>(`http://localhost:8080/api/user-notification-preference/${this.userId}`).subscribe({
      next: (pref) => {
        console.log('✅ Préférences chargées:', pref);
        this.inAppEnabled = pref?.inAppEnabled ?? true;
        this.preferencesLoaded = true;

        // Charger les notifications existantes
        this.loadExistingNotifications();

        // Initialiser WebSocket seulement si inApp est activé
        if (this.inAppEnabled) {
          this.initializeWebSocket();
        }
      },
      error: (err) => {
        console.error('❌ Erreur chargement préférences:', err);
        // Utiliser les valeurs par défaut et continuer
        this.inAppEnabled = true;
        this.preferencesLoaded = true;

        this.loadExistingNotifications();
        this.initializeWebSocket();
      },
    });
  }

  private loadExistingNotifications() {
    if (!this.userId) return;

    this.http.get<any[]>(`http://localhost:8080/api/notifications/${this.userId}`).subscribe({
      next: (notifications) => {
        console.log('📋 Notifications existantes chargées:', notifications?.length || 0);
        this.notifications = (notifications || []).map((notif) => ({
          ...notif,
          message: notif.message ?? JSON.stringify(notif),
          createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
          read: notif.read ?? false,
          id: notif.id ?? Math.random(),
        }));

        console.log('📊 Notifications après traitement:', this.notifications);
      },
      error: (err) => {
        console.error('❌ Erreur chargement notifications:', err);
        this.notifications = [];
      },
    });
  }

  private initializeWebSocket() {
    if (!this.userId) return;

    console.log('🔌 Initialisation WebSocket pour user:', this.userId);

    // Se connecter au WebSocket
    this.notifWebSocketService.connect(this.userId);

    // S'abonner aux nouvelles notifications
    this.wsSub = this.notifWebSocketService.notifications().subscribe({
      next: (notif: any) => {
        console.log('🎯 Nouvelle notification reçue:', notif);

        // Vérifier si la notification existe déjà
        if (notif.id && this.notifications.some(n => n.id === notif.id)) {
          console.log('⚠️ Notification déjà présente, ignorée');
          return;
        }

        // Ajouter la nouvelle notification
        const newNotification = {
          ...notif,
          message: notif.message ?? JSON.stringify(notif),
          createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
          read: notif.read ?? false,
          id: notif.id ?? Math.random(),
        };

        this.notifications.unshift(newNotification);
        console.log('✅ Notification ajoutée:', newNotification);

        // Afficher le toast si inApp est activé
        if (this.inAppEnabled) {
          this.toastMessage = notif.message || 'Nouvelle notification';
          this.showToast = true;
          setTimeout(() => {
            this.showToast = false;
          }, 5500);
        }
      },
      error: (err) => {
        console.error('❌ Erreur subscription WebSocket:', err);
      },
    });
  }

  toggleNotificationsSidebar() {
    this.showNotificationsSidebar = !this.showNotificationsSidebar;
    if (this.showNotificationsSidebar) {
      this.selectedNotification = null;
      console.log('📂 Sidebar ouvert, notifications:', this.notifications.length);
    }
  }

  openNotificationDetail(notification: any) {
    console.log('📖 Ouverture détail notification:', notification);
    this.selectedNotification = notification;
    this.markAsRead(notification);
  }

  closeDetailSidebar() {
    this.selectedNotification = null;
  }

  closeSidebar() {
    this.showNotificationsSidebar = false;
    setTimeout(() => {
      this.selectedNotification = null;
    }, 300);
  }

  markAsRead(notification: any) {
    if (!notification?.id || notification.read) return;

    console.log('📝 Marquer comme lu:', notification.id);
    this.http.post(`http://localhost:8080/api/notifications/read/${notification.id}`, null).subscribe({
      next: () => {
        notification.read = true;
        console.log('✅ Notification marquée comme lue');
      },
      error: (err) => {
        console.error('❌ Erreur marquer comme lu:', err);
      },
    });
  }

  unreadCount() {
    const count = this.notifications.filter(n => !n.read).length;
    console.log('🔔 Nombre de notifications non lues:', count);
    return count;
  }

  ngOnDestroy() {
    console.log('🧹 Nettoyage composant notifications');
    this.wsSub?.unsubscribe();
    this.notifWebSocketService.disconnect();
  }

  // Méthode pour rediriger vers les produits vendus (pour le vendeur)
  goToSoldProducts(): void {
    console.log('📤 Redirection vers les produits vendus');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers le profil avec l'onglet "sold"
    this.router.navigate(['/profil'], {
      queryParams: { tab: 'sold' },
      state: { activeTab: 'sold' }
    });
  }

// Méthode pour rediriger vers les enchères gagnées (pour l'acheteur)
  goToWonProducts(): void {
    console.log('🏆 Redirection vers les enchères gagnées');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers le profil avec l'onglet "won"
    this.router.navigate(['/profil'], {
      queryParams: { tab: 'won' },
      state: { activeTab: 'won' }
    });
  }

// Méthode pour rediriger vers les enchères
  goToAuctions(): void {
    console.log('🔨 Redirection vers les enchères');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers la page des enchères
    this.router.navigate(['/encheres']);
  }

// Méthode pour rediriger vers les enchères actives
  goToActiveAuctions(): void {
    console.log('🚀 Redirection vers les enchères actives');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers le profil avec l'onglet "bids"
    this.router.navigate(['/profil'], {
      queryParams: { tab: 'bids' },
      state: { activeTab: 'bids' }
    });
  }

// Méthode pour rediriger vers les produits publiés
  goToPublishedProducts(): void {
    console.log('📄 Redirection vers les produits publiés');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers le profil avec l'onglet "published"
    this.router.navigate(['/profil'], {
      queryParams: { tab: 'published' },
      state: { activeTab: 'published' }
    });
  }

  // Dans la classe NotificationComponent, après les autres méthodes de redirection
// Méthode pour rediriger vers le portefeuille
  goToWallet(): void {
    console.log('💰 Redirection vers le portefeuille');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers la page du portefeuille
    this.router.navigate(['/wallet']);
  }

}
