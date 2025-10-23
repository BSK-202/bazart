import { Component } from '@angular/core';

@Component({
  selector: 'app-breadcrumb-ellipsis',
  standalone: true,
  template: `
    <span class="breadcrumb-ellipsis" aria-hidden="true">
      &hellip;
      <span class="sr-only">More</span>
    </span>
  `,
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbEllipsisComponent {}
