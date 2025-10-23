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
                                                                                     (1, 'Informatique', 'Catégorie pour l''informatique et les ordinateurs', 'info.jpg', 1),
                                                                                     (2, 'Smartphones', 'Catégorie pour les téléphones intelligents', 'phone.jpg', 1),
                                                                                     (3, 'Gaming', 'Catégorie pour le gaming et les jeux vidéo', 'gaming.jpg', 1),
                                                                                     (4, 'Médecine générale', 'Catégorie pour la médecine générale', 'medecine.jpg', 2),
                                                                                     (5, 'Nutrition', 'Catégorie pour la nutrition et diététique', 'nutrition.jpg', 2),
                                                                                     (6, 'Cours en ligne', 'Catégorie pour les cours en ligne', 'cours.jpg', 3),
                                                                                     (7, 'Livres', 'Catégorie pour les livres éducatifs', 'livres.jpg', 3);