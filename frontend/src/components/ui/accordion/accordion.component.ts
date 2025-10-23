import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-accordion',
  template: `<ng-content></ng-content>`,
  styleUrls: ['./accordion.component.css']
})
export class AccordionComponent {}
