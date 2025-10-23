import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-button-group-text',
  standalone: true,
  template: `<div class="button-group-text"><ng-content></ng-content></div>`,
  styleUrls: ['./button-group.component.css']
})
export class ButtonGroupTextComponent {}
