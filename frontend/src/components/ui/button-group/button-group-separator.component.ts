import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

type SeparatorOrientation = 'horizontal' | 'vertical';

@Component({
  selector: 'app-button-group-separator',
  standalone: true,
  template: `
    <div class="button-group-separator" [ngClass]="orientation"></div>`,
  imports: [
    NgClass
  ],
  styleUrls: ['./button-group.component.css']
})
export class ButtonGroupSeparatorComponent {
  @Input() orientation: SeparatorOrientation = 'vertical';
}
