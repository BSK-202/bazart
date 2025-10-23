import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-breadcrumb-link',
  standalone: true,
  template: `<a [href]="href" class="breadcrumb-link"><ng-content></ng-content></a>`,
  styleUrls: ['./breadcrumb.component.css']
})
export class BreadcrumbLinkComponent {
  @Input() href: string = '#';
}
