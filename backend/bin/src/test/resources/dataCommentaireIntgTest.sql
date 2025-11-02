-- Nettoyage des données
DELETE FROM commentaire;
DELETE FROM produit_image;
DELETE FROM produit;
DELETE FROM categorie;
DELETE FROM client;
DELETE FROM domaine;

-- Insertion des domaines
INSERT INTO domaine (iddomaine, nomdomaine, description, image) VALUES
                                                                    (1, 'Technologie', 'Domaines liés à la technologie et l''innovation', 'tech.jpg'),
                                                                    (2, 'Électronique', 'Produits électroniques et high-tech', 'electronique.jpg');

-- Insertion des clients
INSERT INTO client (idclient, nom, prenom, email, motdepasse, enabled, dateinscription, ville, pays) VALUES
                                                                                                         (1, 'Dupont', 'Jean', 'jean.dupont@email.com', 'password123', true, '2024-01-15 10:00:00', 'Paris', 'France'),
                                                                                                         (2, 'Martin', 'Marie', 'marie.martin@email.com', 'password456', true, '2024-01-16 11:30:00', 'Lyon', 'France'),
                                                                                                         (3, 'Bernard', 'Pierre', 'pierre.bernard@email.com', 'password789', true, '2024-01-17 09:15:00', 'Marseille', 'France');

-- Insertion des catégories
INSERT INTO categorie (idcategorie, nomcategorie, description, image, iddomaine) VALUES
                                                                                     (1, 'Smartphones', 'Téléphones intelligents et mobiles', 'smartphones.jpg', 1),
                                                                                     (2, 'Ordinateurs', 'Ordinateurs portables et de bureau', 'ordinateurs.jpg', 1),
                                                                                     (3, 'Accessoires', 'Accessoires électroniques', 'accessoires.jpg', 2);

-- Insertion des produits
INSERT INTO produit (idproduit, nom, description, prixdebut, prixfin, a_expertise, datepublication, etat, idclient, idcategorie) VALUES
                                                                                                                                     (1, 'Smartphone Android', 'Smartphone Android dernier cri', 299.99, 399.99, false, '2024-01-20 14:00:00', 'DISPONIBLE', 1, 1),
                                                                                                                                     (2, 'Laptop Gaming', 'Ordinateur portable pour gaming', 899.99, 1199.99, true, '2024-01-21 15:30:00', 'DISPONIBLE', 2, 2),
                                                                                                                                     (3, 'Tablette Tactile', 'Tablette tactile 10 pouces', 199.99, 299.99, false, '2024-01-22 16:45:00', 'DISPONIBLE', 3, 3),
                                                                                                                                     (4, 'Casque Bluetooth', 'Casque audio sans fil', 79.99, 129.99, false, '2024-01-23 10:20:00', 'DISPONIBLE', 1, 3);

-- Insertion des commentaires initiaux
INSERT INTO commentaire (contenu, date, idclient, idproduit) VALUES
                                                                 ('Excellent produit, je recommande !', '2024-01-25 10:00:00', 1, 2),  -- Client 1 commente Produit 2
                                                                 ('Bon rapport qualité-prix', '2024-01-25 11:00:00', 2, 3),           -- Client 2 commente Produit 3
                                                                 ('Livraison rapide, produit conforme', '2024-01-25 12:00:00', 3, 3), -- Client 3 commente Produit 3
                                                                 ('Très satisfait de mon achat', '2024-01-25 13:00:00', 1, 3);        -- Client 1 commente Produit 3