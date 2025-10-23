import { Component, Input } from '@angular/core';
import {NgClass} from '@angular/common';

@Component({
  selector: 'app-accordion-item',
  templateUrl: './accordion-item.component.html',
  imports: [
    NgClass
  ],
  styleUrls: ['./accordion-item.component.css']
})
export class AccordionItemComponent {
  isOpen = false;

  toggle() {
    this.isOpen = !this.isOpen;
  }
}
