import { NotificationComponent } from './notification.component';

describe('NotificationComponent', () => {
  let component: NotificationComponent;

  beforeEach(() => {
    component = new NotificationComponent(
      // Mocks: pass mock NotificationWebSocketService and HttpClient here
      { connect: () => {}, notifications: () => ({ subscribe: () => {} }), disconnect: () => {} } as any,
      { get: () => ({ subscribe: () => {} }), post: () => ({ subscribe: () => {} }) } as any
    );
    component.userId = 1;
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle dropdown', () => {
    expect(component.showDropdown).toBe(false);
    component.toggleDropdown();
    expect(component.showDropdown).toBe(true);
    component.toggleDropdown();
    expect(component.showDropdown).toBe(false);
  });

  it('should count unread notifications', () => {
    component.notifications = [
      { read: false },
      { read: true },
      { read: false },
    ];
    expect(component.unreadCount()).toBe(2);
  });
});