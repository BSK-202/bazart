import { Component } from '@angular/core';
import {NgClass, NgForOf} from '@angular/common';

@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.component.html',
  imports: [
    NgClass,
    NgForOf
  ],
  styleUrls: ['./calendar.component.css']
})
export class CalendarComponent {
  today: Date = new Date();
  currentMonth: number = this.today.getMonth();
  currentYear: number = this.today.getFullYear();

  selectedDate: Date | null = null;
  rangeStart: Date | null = null;
  rangeEnd: Date | null = null;

  // retourne tous les jours du mois avec les jours vides
  get daysInMonth(): Date[] {
    const days: Date[] = [];
    const firstDay = new Date(this.currentYear, this.currentMonth, 1);
    const lastDay = new Date(this.currentYear, this.currentMonth + 1, 0);

    // 1. Ajouter les jours vides du début (mois précédent)
    const firstDayOfWeek = firstDay.getDay(); // 0=dimanche, 1=lundi...
    // Ajuster pour commencer le lundi (1)
    let emptyDaysBefore = firstDayOfWeek - 1;
    if (emptyDaysBefore < 0) emptyDaysBefore = 6; // Si dimanche, ajouter 6 jours vides

    for (let i = emptyDaysBefore; i > 0; i--) {
      const prevDay = new Date(this.currentYear, this.currentMonth, 1 - i);
      days.push(prevDay);
    }

    // 2. Ajouter les jours du mois courant
    const numberOfDays = lastDay.getDate();
    for (let d = 1; d <= numberOfDays; d++) {
      days.push(new Date(this.currentYear, this.currentMonth, d));
    }

    // 3. Ajouter les jours vides de fin (mois suivant)
    const totalCells = 42; // 6 semaines × 7 jours
    const remainingCells = totalCells - days.length;
    for (let i = 1; i <= remainingCells; i++) {
      const nextDay = new Date(this.currentYear, this.currentMonth + 1, i);
      days.push(nextDay);
    }

    return days;
  }

  // Méthode pour obtenir le nom du mois actuel
  getCurrentMonthName(): string {
    return new Date(this.currentYear, this.currentMonth).toLocaleString('default', { month: 'long' });
  }

  // changer de mois
  prevMonth() {
    if (this.currentMonth === 0) {
      this.currentMonth = 11;
      this.currentYear--;
    } else {
      this.currentMonth--;
    }
  }

  nextMonth() {
    if (this.currentMonth === 11) {
      this.currentMonth = 0;
      this.currentYear++;
    } else {
      this.currentMonth++;
    }
  }

  // sélection simple ou en range
  selectDay(day: Date) {
    // Ne pas sélectionner les jours des autres mois
    if (day.getMonth() !== this.currentMonth) return;

    if (!this.rangeStart || (this.rangeStart && this.rangeEnd)) {
      this.rangeStart = day;
      this.rangeEnd = null;
    } else if (this.rangeStart && !this.rangeEnd) {
      if (day >= this.rangeStart) {
        this.rangeEnd = day;
      } else {
        this.rangeEnd = this.rangeStart;
        this.rangeStart = day;
      }
    }
    this.selectedDate = day;
  }

  // helpers css
  isToday(day: Date): boolean {
    return (
      day.toDateString() === this.today.toDateString()
    );
  }

  isSelected(day: Date): boolean {
    return (
      this.selectedDate?.toDateString() === day.toDateString()
    );
  }

  inRange(day: Date): boolean {
    if (this.rangeStart && this.rangeEnd) {
      return day >= this.rangeStart && day <= this.rangeEnd;
    }
    return false;
  }

  // Vérifier si un jour est d'un autre mois
  isOtherMonth(day: Date): boolean {
    return day.getMonth() !== this.currentMonth;
  }
}
