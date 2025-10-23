#!/bin/bash

echo "Waiting for backend to be ready..."
sleep 10

echo "Inserting domaines and categories data..."

# Utiliser l'hôte "db" au lieu de "localhost"
PGPASSWORD=admin psql -h db -U postgres -d bazart -p 5432 << EOF
-- Insertion des domaines
INSERT INTO domaine (iddomaine, description, image, nomdomaine) VALUES
(1, 'Objets de luxe et de prestige destinés à valoriser le style personnel. Inclut montres de collection, diamants, pierres précieuses et autres bijoux rares très recherchés dans les ventes aux enchères.', 'accessoires.jpg', 'Accessoires de Luxe'),
(2, 'Œuvres et objets artistiques uniques allant des meubles anciens aux peintures, sculptures, décorations raffinées et matériaux antiques. Ces pièces suscitent un fort intérêt pour leur valeur historique.', 'art.jpg', 'Art & Antiquités'),
(3, 'Objets rares et recherchés par les passionnés : timbres anciens, pièces de monnaie, cartes rares, vinyles de collection. Chaque pièce raconte une histoire et attire les collectionneurs.', 'collection.jpg', 'Collections'),
(4, 'Appareils technologiques de première génération, prototypes, instruments scientifiques anciens ou éditions limitées modernes. Mélange entre innovation et histoire.', 'technologies.jpg', 'Technologies Vintage')
ON CONFLICT (iddomaine) DO NOTHING;

-- Insertion des catégories (avec le bon ordre: idcategorie, description, image, nomcategorie, iddomaine)
INSERT INTO categorie (idcategorie, description, image, nomcategorie, iddomaine) VALUES
(1, 'Montres suisses, modèles rares et éditions limitées très prisées par les collectionneurs.', 'montres.jpg', 'Montres de Collection', 1),
(2, 'Bagues, colliers, diamants bruts ou taillés, et pièces joaillières d''exception.', 'bijoux.jpg', 'Bijoux & Diamants', 1),
(3, 'Saphirs, rubis, émeraudes et autres pierres rares pour les connaisseurs.', 'pierres.jpg', 'Pierres Précieuses', 1),
(4, 'Meubles rares de différentes époques : Louis XIV, Empire, Art Déco.', 'meubles.jpg', 'Meubles Anciens', 2),
(5, 'Œuvres de maîtres, peintres modernes et contemporains.', 'peintures.jpg', 'Peintures & Tableaux', 2),
(6, 'Sculptures anciennes et contemporaines, objets décoratifs rares.', 'sculptures.jpg', 'Sculptures & Décoration', 2),
(7, 'Collections philatéliques, timbres anciens et éditions limitées.', 'timbres.jpg', 'Timbres Rares', 3),
(8, 'Pièces historiques, rares et en or ou argent.', 'monnaie.jpg', 'Pièces de Monnaie', 3),
(9, 'Disques vinyles rares, éditions limitées et objets musicaux vintage.', 'vinyles.jpg', 'Vinyles & Musique', 3),
(10, 'Radios, téléviseurs et instruments scientifiques anciens.', 'appareils.jpg', 'Appareils Vintage', 4)
ON CONFLICT (idcategorie) DO NOTHING;
EOF

echo "Data insertion completed!"