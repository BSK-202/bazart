import { Component, Input,  OnDestroy,  OnInit,  OnChanges,  SimpleChanges } from "@angular/core"
import  { NotificationWebSocketService } from "../../services/notification-websocket.service"
import  { HttpClient } from "@angular/common/http"
import { CommonModule } from "@angular/common"
import  { Subscription } from "rxjs"

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

  constructor(
    private notifWebSocketService: NotificationWebSocketService,
    private http: HttpClient,
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
    if (this.userId == null) return

    this.http.get<any>(`/api/user-notification-preference/${this.userId}`).subscribe({
      next: (pref) => {
        this.inAppEnabled = pref?.inAppEnabled ?? true

        this.http.get<any[]>(`/api/notifications/${this.userId}`).subscribe({
          next: (n) => {
            this.notifications = (n || []).map((notif) => ({
              ...notif,
              createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
            }))
          },
          error: (err) => console.error("Failed to load notifications:", err),
        })

        this.notifWebSocketService.connect((this.userId ?? 0) as number)

        this.wsSub?.unsubscribe()
        this.wsSub = this.notifWebSocketService.notifications().subscribe({
          next: (notif: any) => {
            if (notif.id && this.notifications.some((n) => n.id === notif.id)) return

            this.notifications.unshift({
              ...notif,
              message: notif.message ?? JSON.stringify(notif),
              createdAt: notif.createdAt ? new Date(notif.createdAt) : new Date(),
              read: notif.read ?? false,
              id: notif.id ?? Math.random(),
            })

            if (this.inAppEnabled) {
              this.toastMessage = notif.message
              this.showToast = true
              setTimeout(() => (this.showToast = false), 5500)
            }
          },
          error: (err) => console.error("WebSocket subscription error:", err),
        })
      },
      error: (err) => {
        console.error("Failed to load notification preferences:", err)
        this.inAppEnabled = true
        this.notifWebSocketService.connect((this.userId ?? 0) as number)
      },
    })
  }

  toggleNotificationsSidebar() {
    this.showNotificationsSidebar = !this.showNotificationsSidebar
    if (this.showNotificationsSidebar) {
      this.selectedNotification = null
    }
  }

  openNotificationDetail(notification: any) {
    this.selectedNotification = notification
    this.markAsRead(notification)
  }

  closeDetailSidebar() {
    this.selectedNotification = null
  }

  closeSidebar() {
    this.showNotificationsSidebar = false
    setTimeout(() => {
      this.selectedNotification = null
    }, 300)
  }

  markAsRead(notification: any) {
    if (!notification?.id || notification.read) return
    this.http.post(`/api/notifications/read/${notification.id}`, null).subscribe({
      next: () => (notification.read = true),
      error: (err) => console.error("Failed to mark as read:", err),
    })
  }

  unreadCount() {
    return this.notifications.filter((n) => !n.read).length
  }

  ngOnDestroy() {
    this.wsSub?.unsubscribe()
    this.notifWebSocketService.disconnect()
  }
}
