import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-alert',
  standalone: true,
  template: `
    <div class="alert" [ngClass]="type">
      {{ message }}
    </div>
  `,
  imports: [
    NgClass
  ],
  styles: [`
    .alert {
      padding: 12px;
      border-radius: 6px;
      margin: 8px 0;
    }

    .success {
      background: #d1fae5;
      color: #065f46;
    }

    .error {
      background: #fee2e2;
      color: #991b1b;
    }

    .warning {
      background: #fef3c7;
      color: #92400e;
    }
  `]
})
export class AlertComponent {
  @Input() type: 'success' | 'error' | 'warning' = 'success';
  @Input() message = '';
}
