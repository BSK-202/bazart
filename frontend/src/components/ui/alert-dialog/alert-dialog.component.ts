import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-alert-dialog',
  standalone: true,
  imports: [CommonModule], // <-- obligatoire pour *ngIf et *ngFor
  templateUrl: './alert-dialog.component.html',
  styleUrls: ['./alert-dialog.component.css']
})
export class AlertDialogComponent {
  @Input() title: string = "Titre de la boîte de dialogue";
  @Input() description: string = "Description du message";

  isOpen: boolean = false;

  open() {
    this.isOpen = true;
  }

  close() {
    this.isOpen = false;
  }

  confirm() {
    console.log("Confirmé !");
    this.isOpen = false;
  }
}
