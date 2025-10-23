import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-breadcrumb-separator',
  standalone: true,
  template: `<li class="breadcrumb-separator"><ng-content>&gt;</ng-content></li>`,
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbSeparatorComponent {}
