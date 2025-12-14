import { Component, Input, OnDestroy, OnInit, OnChanges, SimpleChanges } from "@angular/core"
import { NotificationWebSocketService } from "../../services/notification-websocket.service"
import { HttpClient } from "@angular/common/http"
import { CommonModule } from "@angular/common"
import { Subscription } from "rxjs"
import {Router} from '@angular/router';

import { HttpHeaders } from '@angular/common/http';
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
  @Input() isAdmin: boolean = false; // 🆕 Ajouter cette propriété

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
      this.ngOnDestroy();
      this.tryInitialize();
    }
    if (changes["isAdmin"]) {
      this.ngOnDestroy();
      this.tryInitialize();
    }
  }


  private tryInitialize() {
    if (this.userId == null) {
      console.log('❌ UserId est null');
      return;
    }

    console.log('🔄 Initialisation pour user:', this.userId, 'admin:', this.isAdmin);

    // 🆕 URLs DIFFÉRENTES POUR ADMIN/CLIENT
    const baseUrl = this.isAdmin
      ? 'http://localhost:8080/api/admin/notifications'
      : 'http://localhost:8080/api/notifications';

    const notificationsUrl = `${baseUrl}/${this.userId}`;

    // Charger les notifications
    this.loadExistingNotifications(notificationsUrl);

    // Toujours activer WebSocket pour les admins
    this.initializeWebSocket();
  }

  private getHeaders(): HttpHeaders {
    let headers = new HttpHeaders();

    if (this.isAdmin) {
      // Pour admin, utilisez adminToken
      const adminToken = localStorage.getItem('adminToken');
      if (adminToken) {
        headers = headers.set('Authorization', `Bearer ${adminToken}`);
      }
    } else {
      // Pour utilisateur normal, utilisez authToken
      const authToken = localStorage.getItem('authToken');
      if (authToken) {
        headers = headers.set('Authorization', `Bearer ${authToken}`);
      }
    }

    return headers;
  }


  private loadExistingNotifications(notificationsUrl: string) {
    console.log('📡 Chargement depuis:', notificationsUrl);

    // ✅ Utilisez les headers d'authentification
    this.http.get<any[]>(notificationsUrl, {
      headers: this.getHeaders()
    }).subscribe({
      next: (notifications) => {
        console.log('📋 Notifications reçues:', notifications?.length || 0);
        this.notifications = (notifications || []).map((notif) => ({
          ...notif,
          message: notif.message ?? JSON.stringify(notif),
          createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
          read: notif.read ?? false,
          id: notif.id ?? Math.random(),
        }));
        console.log('📊 Notifications traitées:', this.notifications.length);
      },
      error: (err) => {
        console.error('❌ Erreur chargement notifications:', err);
        console.error('❌ Status:', err.status);
        console.error('❌ Message:', err.message);

        // ✅ Ajoutez un fallback pour déboguer
        if (err.status === 403) {
          console.warn('⚠️ Accès interdit - Vérifiez le token admin');
          this.checkAdminToken();
        }
        this.notifications = [];
      },
    });
  }

  // ✅ Méthode pour vérifier le token admin
  private checkAdminToken(): void {
    const adminToken = localStorage.getItem('adminToken');
    const adminData = localStorage.getItem('adminData');

    console.log('🔍 Vérification token admin:', {
      hasToken: !!adminToken,
      hasData: !!adminData,
      token: adminToken ? 'présent' : 'absent',
      data: adminData ? JSON.parse(adminData) : 'absent'
    });
  }


  private initializeWebSocket() {
    if (!this.userId) return;

    console.log('🔌 Initialisation WebSocket pour:', this.userId);

    this.notifWebSocketService.connect(this.userId);

    this.wsSub = this.notifWebSocketService.notifications().subscribe({
      next: (notif: any) => {
        console.log('🎯 Nouvelle notification reçue:', notif);

        if (!notif) {
          console.warn('⚠️ Notification null ignorée');
          return;
        }

        const notifId = notif.id ?? Math.random();

        if (this.notifications.some(n => n.id === notifId)) {
          console.log('⚠️ Notification déjà présente');
          return;
        }

        const newNotification = {
          ...notif,
          message: notif.message ?? JSON.stringify(notif),
          createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
          read: notif.read ?? false,
          id: notifId,
        };

        this.notifications.unshift(newNotification);
        console.log('✅ Notification ajoutée');

        // Afficher toast
        this.toastMessage = notif.message || 'Nouvelle notification';
        this.showToast = true;
        setTimeout(() => {
          this.showToast = false;
        }, 5500);
      },
      error: (err) => {
        console.error('❌ Erreur WebSocket:', err);
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

    const baseUrl = this.isAdmin
      ? 'http://localhost:8080/api/admin/notifications'
      : 'http://localhost:8080/api/notifications';

    const url = `${baseUrl}/read/${notification.id}`;

    this.http.post(url, null, {
      headers: this.getHeaders()
    }).subscribe({
      next: () => {
        notification.read = true;
        console.log('✅ Notification marquée comme lue');
      },
      error: (err) => {
        console.error('❌ Erreur:', err);
        console.error('❌ Status:', err.status);
        if (err.status === 403) {
          console.warn('⚠️ Accès interdit pour marquer comme lu');
        }
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

  goToPublishedProductsForRelance(): void {
    console.log('🔄 Redirection vers les produits publiés pour relance');
    this.closeSidebar();
    this.closeDetailSidebar();

    // Rediriger vers le profil avec l'onglet "published"
    this.router.navigate(['/profil'], {
      queryParams: { tab: 'published' },
      state: { activeTab: 'published' }
    });
  }

}
