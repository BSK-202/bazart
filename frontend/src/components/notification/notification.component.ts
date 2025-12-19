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

        this.notifications = (notifications || []).map((notif) => {
          // Debug: afficher le contenu de chaque notification
          console.log('🔍 Notification brute reçue:', notif);

          // Extraire alertType depuis le message
          const alertType = this.extractAlertTypeFromMessage(notif.message || '');
          console.log('🎯 alertType extrait:', alertType);

          // Extraire productId
          const productId = this.extractProductIdFromMessage(notif.message || '');
          if (productId) {
            console.log('🔍 productId extrait du message:', productId);
          }

          const processedNotification = {
            ...notif,
            message: notif.message ?? JSON.stringify(notif),
            createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
            read: notif.read ?? false,
            id: notif.id ?? Math.random(),
            // Extraire productId depuis différentes sources
            productId: productId || this.extractProductIdFromNotification(notif),
            type: notif.type, // S'assurer que le type est présent
            data: notif.data, // Conserver les données brutes
            alertType: alertType // 🆕 AJOUTER alertType ici
          };

          console.log('📝 Notification traitée:', {
            type: processedNotification.type,
            alertType: processedNotification.alertType,
            productId: processedNotification.productId,
            message: processedNotification.message?.substring(0, 80) + '...'
          });

          return processedNotification;
        });

        console.log('📊 Notifications traitées:', this.notifications.length);

        // Debug: afficher toutes les notifications avec leurs alertType
        this.notifications.forEach((notif, index) => {
          console.log(`📦 Notification ${index + 1}:`, {
            type: notif.type,
            alertType: notif.alertType,
            message: notif.message?.substring(0, 50),
            productId: notif.productId,
            hasData: !!notif.data
          });
        });
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

  private extractAlertTypeFromMessage(message: string): string {
    if (!message) {
      console.log('⚠️ Message vide pour extractAlertTypeFromMessage');
      return 'PUBLICATION';
    }

    console.log('🔍 Extraction alertType depuis message:', message.substring(0, 100) + '...');

    // 1. Chercher le tag [ALERT_TYPE:XXX]
    const tagMatch = message.match(/\[ALERT_TYPE:(\w+)\]/);
    if (tagMatch && tagMatch[1]) {
      console.log('✅ Tag [ALERT_TYPE] trouvé:', tagMatch[1]);
      return tagMatch[1];
    }

    // 2. Vérifier si c'est une notification d'achat (plus précis)
    const isAchat =
      message.includes('🛒 Validation d\'achat requise') ||
      (message.includes('Validation d\'achat requise') && message.includes('Acheteur:')) ||
      message.includes('Nouvelle demande de validation d\'achat') ||
      (message.includes('Acheteur:') && message.includes('Montant:')) ||
      (message.includes('🛒') && message.includes('Validation'));

    console.log('🔍 Détection achat:', isAchat ? 'ACHAT' : 'PUBLICATION');
    return isAchat ? 'ACHAT' : 'PUBLICATION';
  }

  private extractProductIdFromMessage(message: string): number | null {
    if (!message) return null;

    // 1. Chercher productId dans un format spécifique si présent
    // Ex: "productId:123" ou "ID:123"
    const idPatterns = [
      /productId[:\s]*(\d+)/i,
      /produitId[:\s]*(\d+)/i,
      /ID[:\s]*(\d+)/i
    ];

    for (const pattern of idPatterns) {
      const match = message.match(pattern);
      if (match && match[1]) {
        const id = parseInt(match[1], 10);
        if (!isNaN(id)) {
          console.log('🔍 productId extrait via pattern:', id);
          return id;
        }
      }
    }

    // 2. Sinon, chercher le nom du produit
    const nameMatch = message.match(/Produit:\s*"([^"]+)"/);
    if (nameMatch && nameMatch[1]) {
      console.log('🔍 Nom produit trouvé:', nameMatch[1]);
      // Note: Ici vous pourriez appeler un service pour convertir nom → ID
      // Pour l'instant, retournez null
    }

    return null;
  }

// Ajouter cette méthode pour extraire le productId
  private extractProductIdFromNotification(notif: any): number | null {
    // Essayer différentes sources possibles
    if (notif.productId) return notif.productId;
    if (notif.data?.productId) return notif.data.productId;
    if (notif.data?.produitId) return notif.data.produitId;

    // Pour les notifications de type NEW_BID, essayer d'extraire l'ID du message
    if (notif.type === 'NEW_BID' && notif.message) {
      const match = notif.message.match(/produit\s*["']?([^"'\s]+)["']?/i);
      if (match) {
        console.log('🔍 ProductId extrait du message:', match[1]);
      }
    }

    return null;
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
        console.log('🎯 Nouvelle notification WebSocket reçue:', notif);

        if (!notif) {
          console.warn('⚠️ Notification null ignorée');
          return;
        }

        const notifId = notif.id ?? Math.random();

        if (this.notifications.some(n => n.id === notifId)) {
          console.log('⚠️ Notification déjà présente');
          return;
        }

        // 🆕 Extraire alertType pour les notifications WebSocket aussi
        const alertType = this.extractAlertTypeFromMessage(notif.message || '');
        console.log('🎯 alertType extrait (WebSocket):', alertType);

        const newNotification = {
          ...notif,
          message: notif.message ?? JSON.stringify(notif),
          createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
          read: notif.read ?? false,
          id: notifId,
          productId: notif.productId || notif.data?.productId,
          alertType: alertType // 🆕 Ajouter alertType
        };

        this.notifications.unshift(newNotification);
        console.log('✅ Notification ajoutée avec alertType:', alertType);

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
    console.log('📖 Ouverture détail notification:');
    this.logNotificationDetails(notification);
    // Si l'API envoie alertType, on l'utilise directement
    // Sinon on le détecte à partir du message
    if (!notification.alertType) {
      notification.alertType = this.detectAlertTypeFromMessageForNotification(notification.message);
    }

    this.selectedNotification = notification;
    this.markAsRead(notification);
  }

  private detectAlertTypeFromMessageForNotification(message: string): string {
    if (message.includes('Validation d\'achat requise') ||
      message.includes('🛒') ||
      message.includes('achat') ||
      message.includes('Acheteur:') ||
      message.includes('Vendeur:')) {
      return 'ACHAT';
    }
    return 'PUBLICATION';
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


  goToProductDetails(productId?: number): void {
    console.log('🔍 Méthode goToProductDetails appelée');
    console.log('📌 Paramètre productId:', productId);
    console.log('📌 selectedNotification AVANT:', this.selectedNotification);

    // Sauvegarder l'ID AVANT de fermer
    const currentNotification = this.selectedNotification;

    this.closeSidebar();
    this.closeDetailSidebar();

    // Utiliser la notification sauvegardée
    let finalProductId = productId;

    if (!finalProductId && currentNotification) {
      // Essayer différentes sources depuis la notification sauvegardée
      finalProductId = currentNotification.productId ||
        currentNotification.data?.productId ||
        currentNotification.data?.produitId;

      console.log('🔍 ProductId extrait de la notification:', finalProductId);
      console.log('🔍 Détails de la notification:', {
        productIdDirect: currentNotification.productId,
        dataProductId: currentNotification.data?.productId,
        data: currentNotification.data
      });
    }

    // Si on a un ID valide, rediriger
    if (finalProductId && finalProductId > 0) {
      console.log(`📍 Redirection vers /produit/${finalProductId}`);
      this.router.navigate([`/produit/${finalProductId}`]);
    } else {
      console.error('❌ ID produit non disponible ou invalide:', finalProductId);
      console.error('❌ Notification complète:', currentNotification);

      // Fallback vers la liste des enchères
      console.log('⚠️ Fallback vers la liste des enchères');
      this.router.navigate(['/encheres']);
    }
  }

// Modifier la méthode goToDelailProducts pour utiliser la nouvelle méthode générique
  goToDelailProducts(): void {
    const productId = this.selectedNotification?.productId;
    if (productId) {
      this.goToProductDetails(productId);
    } else {
      console.log('⚠️ Aucun ID produit trouvé, utilisation du fallback');
      this.router.navigate(['/produit']);
    }
  }

// Remplacer la méthode goToAdminAlertAction() par cette version améliorée
  goToAdminAlertAction(): void {
    console.log('🔄 Redirection selon le type d\'alerte admin');

    // Extraire alertType depuis la notification
    const notification = this.selectedNotification;

    // 1. Vérifier alertType dans la notification
    let alertType = notification?.alertType;

    // 2. Si non trouvé, extraire depuis le message
    if (!alertType && notification?.message) {
      alertType = this.extractAlertTypeFromMessageForNotification(notification.message);
      console.log('🔍 alertType extrait depuis message:', alertType);
    }

    // 3. Si toujours non trouvé, utiliser la détection générique
    if (!alertType) {
      alertType = this.detectAlertTypeFromMessage();
    }

    console.log('🎯 Type d\'alerte final:', alertType);

    this.closeSidebar();
    this.closeDetailSidebar();

    // Redirection basée sur alertType
    if (alertType === 'ACHAT') {
      console.log('🛒 Redirection vers validation d\'achat');
      this.router.navigate(['/admin/validation-achat']);
    } else {
      console.log('📄 Redirection vers publications');
      this.router.navigate(['/admin/publications']);
    }
  }

// Ajouter cette méthode pour extraire alertType depuis le message
  private extractAlertTypeFromMessageForNotification(message: string): string {
    console.log('🔍 Extraction alertType depuis message:', message.substring(0, 100));

    // Chercher le tag [ALERT_TYPE:XXX]
    const tagMatch = message.match(/\[ALERT_TYPE:(\w+)\]/);
    if (tagMatch && tagMatch[1]) {
      console.log('✅ Tag [ALERT_TYPE] trouvé:', tagMatch[1]);
      return tagMatch[1];
    }

    // Fallback sur la détection par mots-clés
    return this.detectAlertTypeFromMessage();
  }

// Améliorer la méthode de détection
  private detectAlertTypeFromMessage(): string {
    const message = this.selectedNotification?.message || '';
    console.log('📝 Analyse du message pour détection:', message.substring(0, 100));

    // Mots-clés pour ACHAT
    const achatKeywords = [
      'Validation d\'achat requise',
      '🛒',
      'achat',
      'Acheteur:',
      'Vendeur:',
      'montant',
      'DH'
    ];

    // Vérifier si le message contient des mots-clés d'achat
    const isAchat = achatKeywords.some(keyword =>
      message.toLowerCase().includes(keyword.toLowerCase())
    );

    if (isAchat) {
      console.log('✅ Détection ACHAT réussie');
      return 'ACHAT';
    }

    console.log('📄 Par défaut: PUBLICATION');
    return 'PUBLICATION';
  }

  logNotificationDetails(notification: any): void {
    console.log('🔍 DEBUG Notification details:');
    console.log('Type:', notification.type);
    console.log('AlertType:', notification.alertType);
    console.log('Data object:', notification.data);
    console.log('Message:', notification.message);
    console.log('Full notification:', JSON.stringify(notification, null, 2));

    // Vérifier si alertType existe dans data
    if (notification.data && notification.data.alertType) {
      console.log('⚠️ alertType trouvé dans data:', notification.data.alertType);
      // Si nécessaire, copier alertType depuis data vers la racine
      if (!notification.alertType) {
        notification.alertType = notification.data.alertType;
        console.log('📝 alertType copié depuis data');
      }
    }
  }


  getAdminAlertButtonText(): string {
    if (!this.selectedNotification) return 'Traiter la demande';

    const alertType = this.selectedNotification.alertType ||
      this.extractAlertTypeFromMessageForNotification(this.selectedNotification.message || '');

    return alertType === 'ACHAT' ? 'Valider l\'achat' : 'Voir publications';
  }


}
