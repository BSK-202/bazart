import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-aspect-ratio',
  templateUrl: './aspect-ratio.component.html',
  styleUrls: ['./aspect-ratio.component.css'],
})
export class AspectRatioComponent {
  @Input() ratio: number = 1; // 1 par défaut (carré)

  get paddingTop(): string {
    return `${100 / this.ratio}%`;
  }
}
