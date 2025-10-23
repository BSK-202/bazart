import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

type ButtonGroupOrientation = 'horizontal' | 'vertical';

@Component({
  selector: 'app-button-group',
  standalone: true,
  templateUrl: './button-group.component.html',
  styleUrls: ['./button-group.component.css'],
  imports: [
    NgClass
  ]
})
export class ButtonGroupComponent {
  @Input() orientation: ButtonGroupOrientation = 'horizontal';
}
