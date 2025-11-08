import { Component, Input, OnInit, OnDestroy, HostListener, Output, EventEmitter } from "@angular/core"
import { CommonModule } from "@angular/common"
import { trigger, transition, style, animate } from "@angular/animations"

interface Notification {
  id: number
  title: string
  message: string
  type: "info" | "success" | "warning" | "error"
  timestamp: Date
  isRead: boolean
  icon: string
  actionUrl?: string
}

@Component({
  selector: "app-notification",
  standalone: true,
  imports: [CommonModule],
  templateUrl: "./notification.component.html",
  styleUrls: ["./notification.component.css"],
  animations: [
    trigger("slideIn", [
      transition(":enter", [
        style({ transform: "translateX(100%)", opacity: 0 }),
        animate("300ms cubic-bezier(0.4, 0, 0.2, 1)", style({ transform: "translateX(0)", opacity: 1 })),
      ]),
      transition(":leave", [
        animate("250ms cubic-bezier(0.4, 0, 0.2, 1)", style({ transform: "translateX(100%)", opacity: 0 }))
      ]),
    ]),
    trigger("fadeOverlay", [
      transition(":enter", [
        style({ opacity: 0 }),
        animate("300ms cubic-bezier(0.4, 0, 0.2, 1)", style({ opacity: 1 }))
      ]),
      transition(":leave", [
        animate("250ms cubic-bezier(0.4, 0, 0.2, 1)", style({ opacity: 0 }))
      ]),
    ]),
    trigger("notificationItem", [
      transition(":enter", [
        style({ opacity: 0, transform: "translateY(-10px)" }),
        animate("200ms ease-out", style({ opacity: 1, transform: "translateY(0)" }))
      ]),
      transition(":leave", [
        animate("150ms ease-in", style({ opacity: 0, transform: "translateX(100%)" }))
      ])
    ])
  ],
})
export class NotificationComponent implements OnInit, OnDestroy {
  @Input() userId: number | null = null
  @Output() notificationCountChange = new EventEmitter<number>()

  isOpen = false
  notifications: Notification[] = []
  unreadCount = 0
  private escapeListener?: (event: KeyboardEvent) => void

  ngOnInit() {
    this.loadNotifications()
    this.setupEscapeListener()
  }

  ngOnDestroy() {
    this.removeEscapeListener()
  }

  private setupEscapeListener() {
    this.escapeListener = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && this.isOpen) {
        this.closeSidebar()
      }
    }
    document.addEventListener('keydown', this.escapeListener)
  }

  private removeEscapeListener() {
    if (this.escapeListener) {
      document.removeEventListener('keydown', this.escapeListener)
    }
  }

  loadNotifications() {
    // Mock notifications - Remplacez par un appel API réel
    this.notifications = [
      {
        id: 1,
        title: "Enchère remportée 🎉",
        message: 'Félicitations ! Vous avez remporté l\'enchère pour "Vase Antique"',
        type: "success",
        timestamp: new Date(Date.now() - 5 * 60000),
        isRead: false,
        icon: "🎉",
        actionUrl: "/mes-achats"
      },
      {
        id: 2,
        title: "Nouvelle offre reçue",
        message: 'Un acheteur a proposé 250€ pour votre "Tableau Vintage"',
        type: "info",
        timestamp: new Date(Date.now() - 30 * 60000),
        isRead: false,
        icon: "💰",
        actionUrl: "/mes-ventes"
      },
      {
        id: 3,
        title: "Enchère se termine bientôt",
        message: 'L\'enchère pour "Montre de poche" se termine dans 2 heures',
        type: "warning",
        timestamp: new Date(Date.now() - 2 * 60 * 60000),
        isRead: true,
        icon: "⏰",
        actionUrl: "/encheres"
      },
      {
        id: 4,
        title: "Message important",
        message: "Vous avez un nouveau message d'un acheteur potentiel",
        type: "info",
        timestamp: new Date(Date.now() - 24 * 60 * 60000),
        isRead: true,
        icon: "💬",
        actionUrl: "/messages"
      },
    ]
    this.updateUnreadCount()
  }

  toggleSidebar() {
    this.isOpen = !this.isOpen
    if (this.isOpen) {
      this.markAllAsRead()
    }
  }

  closeSidebar() {
    this.isOpen = false
  }

  markAllAsRead() {
    const hadUnread = this.unreadCount > 0
    this.notifications.forEach(notif => notif.isRead = true)
    this.updateUnreadCount()

    if (hadUnread) {
      // Optionnel: Appel API pour marquer comme lu en backend
      console.log('Toutes les notifications marquées comme lues')
    }
  }

  updateUnreadCount() {
    const previousCount = this.unreadCount
    this.unreadCount = this.notifications.filter(n => !n.isRead).length
    this.notificationCountChange.emit(this.unreadCount)

    // Animation du badge quand le compteur change
    if (previousCount !== this.unreadCount) {
      this.animateBadge()
    }
  }

  private animateBadge() {
    // Animation visuelle pour les changements de compteur
    const badge = document.querySelector('.notification-badge')
    if (badge) {
      badge.classList.add('badge-update')
      setTimeout(() => badge.classList.remove('badge-update'), 300)
    }
  }

  getTimeAgo(date: Date): string {
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return "À l'instant"
    if (diffMins < 60) return `il y a ${diffMins} min`
    if (diffHours < 24) return `il y a ${diffHours} h`
    if (diffDays < 7) return `il y a ${diffDays} j`
    if (diffDays < 30) return `il y a ${Math.floor(diffDays / 7)} sem`
    return date.toLocaleDateString("fr-FR", { day: 'numeric', month: 'short' })
  }

  deleteNotification(id: number, event?: Event) {
    if (event) {
      event.stopPropagation()
    }

    this.notifications = this.notifications.filter(n => n.id !== id)
    this.updateUnreadCount()
  }

  clearAllNotifications() {
    this.notifications = []
    this.updateUnreadCount()
  }

  onNotificationClick(notification: Notification) {
    if (!notification.isRead) {
      notification.isRead = true
      this.updateUnreadCount()
    }

    // Navigation ou action
    if (notification.actionUrl) {
      // this.router.navigate([notification.actionUrl])
      console.log('Navigation vers:', notification.actionUrl)
    }

    this.closeSidebar()
  }

  @HostListener("document:click", ["$event"])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement
    if (!target.closest(".notification-container") && this.isOpen) {
      this.closeSidebar()
    }
  }
}
