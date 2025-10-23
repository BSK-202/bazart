import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-avatar',
  standalone: true,  // important si c'est standalone
  imports: [CommonModule], // <-- ajoute ça pour ngIf, ngFor, etc.
  templateUrl: './avatar.component.html',
  styleUrls: ['./avatar.component.css'],
})
export class AvatarComponent {
  @Input() src?: string;
  @Input() fallback?: string;
  @Input() size: number = 32;
}
