# 🏛️ Bazart

### Marketplace d'enchères dédiée aux objets rares, anciens et de collection

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge\&logo=openjdk\&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=for-the-badge\&logo=springboot\&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-20-DD0031?style=for-the-badge\&logo=angular\&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge\&logo=postgresql\&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.10+-3776AB?style=for-the-badge\&logo=python\&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-3.x-000000?style=for-the-badge\&logo=flask\&logoColor=white)

**L'art de l'authentique, expertisé et sécurisé.**

</div>

---

## 📌 Présentation

**Bazart** est une plateforme web full-stack dédiée à la publication, à la découverte et à la vente aux enchères d'objets rares, anciens et de collection.

La plateforme couvre plusieurs catégories :

* ⌚ Montres de collection
* 💎 Bijoux et pierres précieuses
* 🖼️ Œuvres d'art et antiquités
* 🪑 Mobilier ancien
* 🪙 Pièces et objets de collection
* 🎵 Vinyles et objets vintage
* 🔬 Instruments et technologies anciennes

L'objectif est de proposer un environnement permettant aux utilisateurs de publier leurs objets, de consulter les annonces et de participer aux enchères, tout en intégrant un processus de **modération et d'expertise des publications**.

Une composante d'intelligence artificielle est également intégrée afin d'analyser les images soumises et de détecter les images potentiellement générées artificiellement.

---

# 🎯 Objectifs du projet

Bazart a été conçu autour de plusieurs objectifs :

* Centraliser les objets rares et anciens sur une plateforme spécialisée.
* Permettre aux utilisateurs de publier leurs objets.
* Organiser les annonces par domaines et catégories.
* Mettre en place un système d'enchères.
* Sécuriser l'accès aux différentes fonctionnalités.
* Permettre aux administrateurs de modérer les publications.
* Intégrer un système de portefeuille virtuel.
* Ajouter une analyse automatisée des images.
* Séparer les responsabilités entre frontend, backend et service d'intelligence artificielle.

---

# ✨ Fonctionnalités

## 👤 Espace utilisateur

### Catalogue

* Navigation par domaines.
* Navigation par catégories.
* Consultation des objets disponibles.
* Affichage détaillé d'un objet.
* Galerie d'images.
* Informations sur le vendeur.
* Prix et informations liées à l'annonce.

### Publications

* Création d'une annonce.
* Upload de plusieurs images.
* Gestion des informations de l'objet.
* Soumission de l'annonce pour validation.
* Modification des publications.

### Interactions

* ❤️ Like / unlike d'un produit.
* 💬 Commentaires.
* Consultation des commentaires.
* Gestion du profil utilisateur.

### Portefeuille

* Consultation du solde.
* Recharge du portefeuille.
* Débit du portefeuille.
* Historique des opérations.

---

# 👨‍💼 Espace administrateur

L'administration permet notamment de gérer le contenu publié sur la plateforme.

### Modération

* Consultation des publications en attente.
* Vérification des informations d'une publication.
* Acceptation ou refus d'une publication.
* Suivi du nombre de publications en attente.

### Gestion du catalogue

* Création des domaines.
* Modification des domaines.
* Création des catégories.
* Modification des catégories.
* Suppression des catégories.
* Organisation du catalogue.

---

# 🤖 Intelligence artificielle

Bazart intègre un microservice indépendant dédié à l'analyse des images.

Le service utilise **Hugging Face Transformers** et le modèle `Organika/sdxl-detector` afin d'estimer si une image semble avoir été générée artificiellement.

### Pipeline

```text
Utilisateur
    │
    ▼
Publication d'un objet
    │
    ▼
Upload des images
    │
    ▼
Microservice Image Detector
    │
    ▼
Analyse de l'image
    │
    ├── Image probablement réelle
    │
    └── Image potentiellement générée par IA
    │
    ▼
Décision / traitement de la publication
```

### Technologies utilisées

* Python
* Flask
* Hugging Face Transformers
* PyTorch
* Pillow
* `Organika/sdxl-detector`

Le microservice expose notamment :

| Méthode | Endpoint        | Description             |
| ------- | --------------- | ----------------------- |
| `GET`   | `/`             | Informations du service |
| `GET`   | `/health`       | Vérification de l'état  |
| `GET`   | `/models`       | Modèles disponibles     |
| `POST`  | `/verify-image` | Analyse d'une image     |

---

# 🏗️ Architecture

Bazart adopte une architecture séparant clairement les différentes responsabilités applicatives.

```text
                         ┌──────────────────────┐
                         │       Angular        │
                         │      Frontend        │
                         └──────────┬───────────┘
                                    │
                               HTTP / REST
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     Spring Boot      │
                         │       Backend        │
                         ├──────────────────────┤
                         │ Authentication       │
                         │ Catalog              │
                         │ Products             │
                         │ Users                │
                         │ Wallet               │
                         │ Interactions         │
                         │ Administration       │
                         └───────┬─────────┬────┘
                                 │         │
                                 │         │ HTTP
                                 ▼         ▼
                        ┌────────────┐  ┌─────────────────┐
                        │ PostgreSQL │  │ Image Detector  │
                        │            │  │     Flask       │
                        └────────────┘  └────────┬────────┘
                                                │
                                                ▼
                                      Hugging Face Model
```

---

# 📁 Structure du projet

```text
bazart/
│
├── backend/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/
│   │               └── marketplace/
│   │                   ├── admin/
│   │                   ├── auth/
│   │                   ├── catalog/
│   │                   ├── interaction/
│   │                   ├── user/
│   │                   ├── wallet/
│   │                   └── core/
│   │
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── client/
│   │   │   ├── admin/
│   │   │   └── ui/
│   │   ├── services/
│   │   └── environments/
│   │
│   └── package.json
│
├── Image_Detector/
│   ├── app.py
│   └── requirements.txt
│
├── scripts/
│   └── init-data.sh
│
├── docker-compose.yml
├── Jenkinsfile
└── README.md
```

---

# 🛠️ Stack technique

## Backend

| Technologie           | Utilisation              |
| --------------------- | ------------------------ |
| **Java 21**           | Langage principal        |
| **Spring Boot 3.5.6** | Framework backend        |
| **Spring Security**   | Sécurité et autorisation |
| **JWT**               | Authentification         |
| **Spring Data JPA**   | Accès aux données        |
| **Hibernate**         | ORM                      |
| **PostgreSQL 15**     | Base de données          |
| **OAuth2**            | Authentification externe |
| **Lombok**            | Réduction du boilerplate |
| **JUnit 5 / Mockito** | Tests                    |

## Frontend

| Technologie      | Utilisation               |
| ---------------- | ------------------------- |
| **Angular 20**   | Application web           |
| **PrimeNG 20**   | Composants UI             |
| **Tailwind CSS** | Styling                   |
| **AngularFire**  | Intégration Firebase      |
| **FontAwesome**  | Icônes                    |
| **Chart.js**     | Visualisation des données |
| **Quill**        | Édition de contenu        |

## Intelligence artificielle

| Technologie                   | Utilisation                 |
| ----------------------------- | --------------------------- |
| **Python**                    | Langage                     |
| **Flask**                     | API du microservice         |
| **Hugging Face Transformers** | Inférence                   |
| **PyTorch**                   | Machine Learning            |
| **Pillow**                    | Traitement d'images         |
| **SDXL Detector**             | Détection d'images générées |

---

# 🔐 Authentification et sécurité

Le backend utilise **Spring Security** pour sécuriser les ressources de l'application.

L'authentification repose notamment sur :

* JWT
* Spring Security
* OAuth2 / Google selon la configuration
* Gestion des rôles
* Protection des endpoints REST

Exemples d'opérations :

```text
POST /api/auth/register
POST /api/auth/login
POST /api/admins/login
```

---

# 📡 API REST

## 🔐 Authentication

| Méthode | Endpoint             | Description                     |
| ------- | -------------------- | ------------------------------- |
| `POST`  | `/api/auth/register` | Créer un compte                 |
| `POST`  | `/api/auth/login`    | Authentification utilisateur    |
| `POST`  | `/api/admins/login`  | Authentification administrateur |

## 📚 Domaines & catégories

| Méthode  | Endpoint                               | Description                        |
| -------- | -------------------------------------- | ---------------------------------- |
| `GET`    | `/api/domaines`                        | Liste des domaines                 |
| `GET`    | `/api/domaines/{id}`                   | Détails d'un domaine               |
| `POST`   | `/api/domaines/create-with-categories` | Créer un domaine et ses catégories |
| `PUT`    | `/api/domaines/{id}`                   | Modifier un domaine                |
| `GET`    | `/api/categories/domaine/{idDomaine}`  | Catégories d'un domaine            |
| `POST`   | `/api/categories`                      | Créer une catégorie                |
| `PUT`    | `/api/categories/{id}`                 | Modifier une catégorie             |
| `DELETE` | `/api/categories/{id}`                 | Supprimer une catégorie            |

## 🏺 Produits

| Méthode | Endpoint                                         | Description                       |
| ------- | ------------------------------------------------ | --------------------------------- |
| `GET`   | `/api/produits/{id}`                             | Détails d'un produit              |
| `GET`   | `/api/produits/categorie/{idCategorie}/acceptes` | Produits acceptés                 |
| `GET`   | `/api/produits/en-attente`                       | Publications en attente           |
| `GET`   | `/api/produits/en-attente/count`                 | Nombre de publications en attente |
| `GET`   | `/api/produits/vendeur/{vendeurId}`              | Produits d'un vendeur             |
| `POST`  | `/api/produits`                                  | Créer un produit                  |
| `PUT`   | `/api/produits/{id}`                             | Modifier un produit               |
| `PUT`   | `/api/produits/{id}/etat`                        | Modifier l'état d'un produit      |
| `POST`  | `/api/produits/{id}/start-auction`               | Démarrer une enchère              |

## 💬 Interactions

| Méthode  | Endpoint                                | Description                |
| -------- | --------------------------------------- | -------------------------- |
| `POST`   | `/api/interactions/toggle/{produitId}`  | Like / unlike              |
| `GET`    | `/api/interactions/check/{produitId}`   | Vérifier un like           |
| `GET`    | `/api/commentaires/produit/{produitId}` | Récupérer les commentaires |
| `POST`   | `/api/commentaires/produit/{produitId}` | Ajouter un commentaire     |
| `DELETE` | `/api/commentaires/{commentaireId}`     | Supprimer un commentaire   |

## 💰 Wallet

| Méthode | Endpoint               | Description        |
| ------- | ---------------------- | ------------------ |
| `GET`   | `/api/wallet/balance`  | Consulter le solde |
| `POST`  | `/api/wallet/recharge` | Recharger          |
| `POST`  | `/api/wallet/debit`    | Débiter            |
| `GET`   | `/api/wallet/history`  | Historique         |

---

# 🗄️ Modèle de données

La persistance des données repose sur **PostgreSQL**.

Les principales entités couvrent :

```text
User
 │
 ├── Profile
 ├── Products
 ├── Comments
 ├── Likes
 └── Wallet
       │
       └── Transactions

Product
 │
 ├── Category
 │      └── Domain
 │
 ├── Images
 ├── Comments
 └── Auction
```

---

# 🚀 Installation

## Prérequis

Pour exécuter le projet localement :

* Java 21
* Maven
* Node.js 20+
* Angular CLI
* PostgreSQL 15+
* Python 3.10+
* Un projet Firebase configuré

---

## 1. Cloner le projet

```bash
git clone https://github.com/BSK-202/bazart.git

cd bazart
```

---

## 2. Configurer PostgreSQL

Créer une base de données :

```sql
CREATE DATABASE bazart;
```

Configurer ensuite les paramètres de connexion dans la configuration du backend.

---

## 3. Lancer le backend

```bash
cd backend

./mvnw spring-boot:run
```

Sous Windows :

```bash
mvnw.cmd spring-boot:run
```

Le backend sera accessible sur :

```text
http://localhost:8080
```

---

## 4. Lancer le frontend

```bash
cd frontend

npm install

ng serve
```

Application disponible sur :

```text
http://localhost:4200
```

---

## 5. Lancer le microservice IA

```bash
cd Image_Detector

python -m venv venv
```

### Windows

```bash
venv\Scripts\activate
```

### Linux / macOS

```bash
source venv/bin/activate
```

Installer les dépendances :

```bash
pip install -r requirements.txt
```

Puis lancer le service :

```bash
python app.py
```

Service disponible sur :

```text
http://localhost:5000
```

---

# ⚙️ Variables d'environnement

Les informations sensibles ne doivent pas être stockées directement dans le dépôt.

Exemple :

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bazart
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

APP_JWT_SECRET=your_secure_secret
APP_JWT_EXPIRATION_MS=86400000

SPRING_PROFILES_ACTIVE=development
```

Pour Firebase :

```typescript
export const environment = {
  production: false,

  firebase: {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_STORAGE_BUCKET",
    messagingSenderId: "YOUR_SENDER_ID",
    appId: "YOUR_APP_ID"
  },

  apiUrl: "http://localhost:8080/api"
};
```

> ⚠️ Les secrets, mots de passe, tokens et clés privées ne doivent jamais être commités dans Git.

---

# 🧪 Tests

## Backend

```bash
cd backend

./mvnw test
```

Pour la vérification complète :

```bash
./mvnw verify
```

Avec JaCoCo :

```bash
./mvnw test jacoco:report
```

## Frontend

```bash
cd frontend

ng test
```

Build production :

```bash
ng build --configuration production
```

---

# 🐳 Docker

Le projet contient également une configuration Docker permettant de regrouper les différents services.

```bash
docker-compose up --build
```

Services principaux :

| Service         |   Port |
| --------------- | -----: |
| Angular / Nginx | `4200` |
| Spring Boot     | `8080` |
| PostgreSQL      | `5432` |
| Image Detector  | `5000` |

Commandes utiles :

```bash
# Démarrer
docker-compose up -d

# Voir les logs
docker-compose logs -f

# Arrêter
docker-compose down

# Reconstruire
docker-compose up --build
```

---

# 🖥️ Captures d'écran

Ajoute tes captures dans :

```text
docs/
└── screenshots/
    ├── home.png
    ├── product-details.png
    ├── auction.png
    ├── wallet.png
    ├── profile.png
    └── admin-moderation.png
```

Puis présente-les dans le README :

| Accueil                               | Détail d'un objet                                |
| ------------------------------------- | ------------------------------------------------ |
| ![Accueil](docs/screenshots/home.png) | ![Produit](docs/screenshots/product-details.png) |

| Portefeuille                           | Administration                                  |
| -------------------------------------- | ----------------------------------------------- |
| ![Wallet](docs/screenshots/wallet.png) | ![Admin](docs/screenshots/admin-moderation.png) |

---

# 🗺️ Roadmap

Les évolutions envisagées comprennent notamment :

* [ ] Enchères en temps réel avec WebSocket
* [ ] Notifications en temps réel
* [ ] Paiement en ligne
* [ ] Notifications push
* [ ] Application mobile
* [ ] Système d'évaluation des vendeurs
* [ ] Génération de factures PDF
* [ ] Internationalisation FR / AR / EN
* [ ] Tests End-to-End

---

# 🔄 CI/CD

Le projet prévoit une chaîne CI/CD basée sur Jenkins permettant notamment de :

```text
Git Push
   │
   ▼
Jenkins
   │
   ├── Checkout
   │
   ├── Backend Build
   │
   ├── Tests
   │
   ├── Frontend Build
   │
   ├── Docker Build
   │
   └── Validation / Déploiement
```

---

# 📊 Domaines disponibles

| Domaine                  | Catégories                                                     |
| ------------------------ | -------------------------------------------------------------- |
| **Accessoires de Luxe**  | Montres de collection, bijoux & diamants, pierres précieuses   |
| **Art & Antiquités**     | Meubles anciens, peintures & tableaux, sculptures & décoration |
| **Collections**          | Timbres rares, pièces de monnaie, vinyles & musique            |
| **Technologies Vintage** | Appareils vintage, instruments scientifiques                   |

---

# 🤝 Contribution

Les contributions sont les bienvenues.

```bash
# Fork du projet

# Créer une branche
git checkout -b feature/my-feature

# Développer et tester

# Commit
git commit -m "feat: add my feature"

# Push
git push origin feature/my-feature
```

Puis ouvrir une Pull Request.

### Conventional Commits

Le projet utilise notamment :

```text
feat:      nouvelle fonctionnalité
fix:       correction
docs:      documentation
style:     formatage
refactor:  refactorisation
test:      tests
chore:     maintenance
```

---

# 📄 Licence

Projet privé.

© 2026 **BSK-202** — Tous droits réservés.

---

# 👩‍💻 Équipe


<div align="center">

### BSK-202

[![GitHub](https://img.shields.io/badge/GitHub-BSK--202-181717?style=for-the-badge\&logo=github)](https://github.com/BSK-202)

### fadmajadda

[![GitHub](https://img.shields.io/badge/GitHub-fadmajadda-181717?style=for-the-badge\&logo=github)](https://github.com/fadmajadda)

### ImanSah

[![GitHub](https://img.shields.io/badge/GitHub-imanesah-181717?style=for-the-badge\&logo=github)](https://github.com/imanesah)

### Chebila

[![GitHub](https://img.shields.io/badge/GitHub-Chebila-181717?style=for-the-badge\&logo=github)](https://github.com/Chebila)

</div>

---

<div align="center">
### 🏛️ Bazart

**L'art de l'authentique, expertisé et sécurisé.**

</div>
