-- data-test.sql
-- Réinitialiser complètement les séquences H2
ALTER TABLE produit ALTER COLUMN idproduit RESTART WITH 100;
ALTER TABLE categorie ALTER COLUMN idcategorie RESTART WITH 100;
ALTER TABLE client ALTER COLUMN idclient RESTART WITH 100;
ALTER TABLE domaine ALTER COLUMN iddomaine RESTART WITH 100;

DELETE FROM produit_image;
DELETE FROM produit;
DELETE FROM categorie;
DELETE FROM domaine;
DELETE FROM client;

-- Insertion des domaines
INSERT INTO domaine (iddomaine, nomdomaine, description, image) VALUES
                                                                    (1, 'Technologie', 'Domaines liés à la technologie et l''innovation', 'tech.jpg'),
                                                                    (2, 'Santé', 'Domaines liés à la santé et bien-être', 'sante.jpg'),
                                                                    (3, 'Éducation', 'Domaines liés à l''éducation et formation', 'education.jpg'),
                                                                    (4, 'Sport', 'Domaines liés aux activités sportives', 'sport.jpg');

-- Catégories
INSERT INTO categorie (idcategorie, nomcategorie, description, image, iddomaine) VALUES
                                                                                     (1, 'Informatique', 'Ordinateurs et accessoires', 'info.jpg', 1),
                                                                                     (2, 'Téléphonie', 'Smartphones et tablettes', 'phone.jpg', 1),
                                                                                     (3, 'Médecine', 'Équipements médicaux', 'medecine.jpg', 2);

-- Clients (avec tous les champs)
INSERT INTO client (idclient, nom, prenom, email, tel, pays, ville, motdepasse, enabled, dateinscription) VALUES
                                                                                                              (1, 'Dupont', 'Jean', 'jean@example.com', '0123456789', 'France', 'Paris', 'mdp123', true, NOW()),
                                                                                                              (2, 'Martin', 'Marie', 'marie@example.com', '0987654321', 'France', 'Lyon', 'mdp456', true, NOW());

-- Produits (avec la structure exacte)
INSERT INTO produit (idproduit, nom, description, prixdebut, prixfin, etat, datepublication, a_expertise, idcategorie, idclient) VALUES
                                                                                                                                     (1, 'Laptop Gaming', 'Ordinateur portable gaming', 1500.0, 2000.0, 'accepte', NOW(), false, 1, 1),
                                                                                                                                     (2, 'Smartphone Pro', 'Smartphone haut de gamme', 800.0, 1000.0, 'en_attente', NOW(), true, 2, 2),
                                                                                                                                     (3, 'Tablette', 'Tablette tactile', 300.0, 400.0, 'accepte', NOW(), false, 2, 1);

-- Images des produits
INSERT INTO produit_image (idimage, url, produit_idproduit) VALUES
                                                                (1, 'laptop1.jpg', 1),
                                                                (2, 'laptop2.jpg', 1),
                                                                (3, 'phone1.jpg', 2),
                                                                (4, 'tablette1.jpg', 3);

-- Réinitialiser les séquences pour les nouveaux inserts
ALTER TABLE produit ALTER COLUMN idproduit RESTART WITH 4;
ALTER TABLE produit_image ALTER COLUMN idimage RESTART WITH 5;