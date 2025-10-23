import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'outline';

@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
  styleUrls: ['./badge.component.css'],
  imports: [
    NgClass
  ]
})
export class BadgeComponent {
  @Input() variant: BadgeVariant = 'default';
  @Input() className: string = '';
}
