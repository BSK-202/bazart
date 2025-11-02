-- Supprimer d'abord les données dépendantes (dans l'ordre inverse des dépendances)
DELETE FROM produit_image;
DELETE FROM interaction;
DELETE FROM commentaire;
DELETE FROM produit;
DELETE FROM categorie;
DELETE FROM domaine;
-- Insertion des domaines
INSERT INTO domaine (iddomaine, nomdomaine, description, image) VALUES
                                                                    (1, 'Technologie', 'Domaines liés à la technologie et l''innovation', 'tech.jpg'),
                                                                    (2, 'Santé', 'Domaines liés à la santé et bien-être', 'sante.jpg'),
                                                                    (3, 'Éducation', 'Domaines liés à l''éducation et formation', 'education.jpg'),
                                                                    (4, 'Sport', 'Domaines liés aux activités sportives', 'sport.jpg');

-- Insertion des catégories
INSERT INTO categorie (idcategorie, nomcategorie, description, image, iddomaine) VALUES
                                                                                     (1, 'Informatique', 'Ordinateurs et accessoires', 'info.jpg', 1),
                                                                                     (2, 'Téléphonie', 'Smartphones et tablettes', 'phone.jpg', 1),
                                                                                     (3, 'Médecine', 'Équipements médicaux', 'medecine.jpg', 2),
                                                                                     (4, 'Football', 'Équipements de football', 'football.jpg', 4);