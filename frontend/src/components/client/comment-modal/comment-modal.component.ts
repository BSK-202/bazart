// comment-modal.component.ts - VERSION CORRIGÉEE
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommentaireService, Commentaire } from '../../../services/commentaire.service';
import { AuthService } from '../../../services/auth.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faTimes, faTrash, faUser } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-comment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, FaIconComponent],
  templateUrl: './comment-modal.component.html',
  styleUrls: ['./comment-modal.component.css']
})
export class CommentModalComponent implements OnInit {
  @Input() produitId!: number;
  @Input() produitNom!: string;
  @Output() close = new EventEmitter<void>();

  commentaires: Commentaire[] = [];
  nouveauCommentaire: string = '';
  isLoading: boolean = false;
  isSubmitting: boolean = false;

  faTimes = faTimes;
  faTrash = faTrash;
  faUser = faUser;

  constructor(
    private commentaireService: CommentaireService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    console.log('🔍 Modal Commentaire - Produit ID:', this.produitId);
    console.log('🔍 Modal Commentaire - Produit Nom:', this.produitNom);
    console.log('🔍 Modal Commentaire - Utilisateur connecté:', this.authService.isLoggedIn());
    console.log('🔍 Modal Commentaire - User ID:', this.authService.getCurrentUserId());

    this.loadCommentaires();
  }

// comment-modal.component.ts - AJOUTER DANS loadCommentaires()
  loadCommentaires() {
    this.isLoading = true;
    console.log('🔄 Chargement des commentaires pour produit:', this.produitId);

    this.commentaireService.getCommentairesByProduit(this.produitId).subscribe({
      next: (commentaires) => {
        console.log('✅ Commentaires bruts reçus:', commentaires);
        console.log('✅ Type de données:', typeof commentaires);
        console.log('✅ Est un array?', Array.isArray(commentaires));

        if (commentaires && Array.isArray(commentaires)) {
          console.log('✅ Nombre de commentaires:', commentaires.length);

          commentaires.forEach((comment, index) => {
            console.log(`📋 Commentaire ${index}:`, {
              id: comment.idcommentaire,
              contenu: comment.contenu,
              client: comment.client,
              date: comment.date,
              hasClient: !!comment.client,
              clientId: comment.client?.idclient,
              clientName: comment.client ? `${comment.client.prenom} ${comment.client.nom}` : 'N/A'
            });
          });
        } else {
          console.warn('⚠️ Commentaires n\'est pas un array:', commentaires);
        }

        this.commentaires = Array.isArray(commentaires) ? commentaires : [];
        this.isLoading = false;
      },
      error: (err) => {
        console.error('❌ Erreur chargement commentaires:', err);
        console.error('Status:', err.status);
        console.error('Message:', err.message);
        console.error('Détails erreur:', err.error);
        this.isLoading = false;
        this.commentaires = [];
      }
    });
  }


  ajouterCommentaire() {
    if (!this.nouveauCommentaire.trim()) return;

    console.log('🔄 Tentative d\'ajout de commentaire...');
    console.log('🔍 User ID:', this.authService.getCurrentUserId());
    console.log('🔍 Token présent:', !!this.authService.getToken());
    console.log('🔍 User data:', this.authService.getUser());

    this.isSubmitting = true;
    this.commentaireService.addCommentaire(this.produitId, this.nouveauCommentaire.trim())
      .subscribe({
        next: (response: any) => {
          console.log('✅ Réponse complète ajout commentaire:', response);

          let nouveauCommentaire: Commentaire;

          // ✅ Gérer les différents formats de réponse
          if (response.commentaire) {
            // Format avec wrapper
            nouveauCommentaire = response.commentaire;
          } else if (response.idcommentaire) {
            // Format direct (commentaire)
            nouveauCommentaire = response;
          } else {
            // Format inattendu
            console.warn('⚠️ Format de réponse inattendu:', response);
            nouveauCommentaire = response;
          }

          console.log('✅ Commentaire à ajouter:', nouveauCommentaire);

          // ✅ Ajouter le commentaire en haut de la liste
          this.commentaires.unshift(nouveauCommentaire);
          this.nouveauCommentaire = '';
          this.isSubmitting = false;

          // ✅ NE PAS fermer le modal - l'utilisateur reste sur le modal
          console.log('✅ Commentaire ajouté, modal reste ouvert');
        },
        error: (err) => {
          console.error('❌ Erreur ajout commentaire:', err);
          console.error('Status:', err.status);
          console.error('Message:', err.message);
          console.error('Détails:', err.error);

          this.isSubmitting = false;

          if (err.status === 401 || err.status === 403) {
            console.log('🔐 Redirection vers login suite à erreur auth');
            this.authService.redirectToLogin('Veuillez vous reconnecter pour commenter');
          } else {
            alert('Erreur lors de l\'ajout du commentaire: ' + (err.error?.error || err.message));
          }
        }
      });
  }

  supprimerCommentaire(commentaire: Commentaire) {
    if (!this.peutSupprimer(commentaire)) {
      alert('Vous n\'êtes pas autorisé à supprimer ce commentaire');
      return;
    }

    if (confirm('Voulez-vous vraiment supprimer ce commentaire ?')) {
      this.commentaireService.deleteCommentaire(commentaire.idcommentaire)
        .subscribe({
          next: () => {
            console.log('✅ Commentaire supprimé avec succès');
            // ✅ Retirer le commentaire de la liste
            this.commentaires = this.commentaires.filter(
              (c: Commentaire) => c.idcommentaire !== commentaire.idcommentaire
            );
            // ✅ NE PAS fermer le modal
            console.log('✅ Commentaire supprimé, modal reste ouvert');
          },
          error: (err) => {
            console.error('❌ Erreur suppression commentaire:', err);
            alert('Erreur lors de la suppression: ' + (err.error?.error || err.message));
          }
        });
    }
  }

  peutSupprimer(commentaire: Commentaire): boolean {
    const currentUserId = this.authService.getCurrentUserId();

    console.log('🔍 Vérification suppression:');
    console.log('   Current User ID:', currentUserId);
    console.log('   Commentaire Client ID:', commentaire.client?.idclient);
    console.log('   Commentaire Client:', commentaire.client);

    if (!commentaire.client || !currentUserId) {
      return false;
    }

    const peutSupprimer = currentUserId === commentaire.client.idclient;
    console.log('   Peut supprimer:', peutSupprimer);

    return peutSupprimer;
  }

  formatDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('Erreur format date:', error);
      return dateString;
    }
  }

  onClose() {
    // ✅ Émettre l'événement pour mettre à jour le compteur de commentaires
    this.close.emit();
  }

  isAuthenticated(): boolean {
    const isAuth = this.authService.isLoggedIn();
    console.log('🔍 isAuthenticated:', isAuth);
    return isAuth;
  }
}
