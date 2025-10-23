import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface Category {
  name: string;
  slug: string;
  image: string;
  count: number;
}

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css']
})
export class CategoriesComponent {
  categories: Category[] = [
    {
      name: "Tapis & Textiles",
      slug: "tapis-textiles",
      image: "/moroccan-berber-rug-geometric-pattern.jpg",
      count: 124,
    },
    {
      name: "Poterie & Céramique",
      slug: "poterie-ceramique",
      image: "/moroccan-ceramic-pottery-tagine-colorful.jpg",
      count: 89,
    },
    {
      name: "Bijoux",
      slug: "bijoux",
      image: "/moroccan-silver-jewelry-traditional-berber.jpg",
      count: 156,
    },
    {
      name: "Lanternes & Luminaires",
      slug: "lanternes-luminaires",
      image: "/moroccan-brass-lantern-intricate-pattern.jpg",
      count: 67,
    },
  ];
}
