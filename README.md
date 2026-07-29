# 🏛️ Bazart

**Plateforme de vente aux enchères d’objets artisanaux anciens avec expertise préalable**

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-20-red)](https://angular.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)](https://www.docker.com/)

---

## 📖 Description

**Bazart** est une marketplace spécialisée dans la vente aux enchères d’objets rares et anciens :
montres de collection, bijoux, œuvres d’art, antiquités, pièces de collection et technologies vintage.

Chaque objet est soumis à une **expertise préalable** et les images sont analysées par une **IA de détection** afin d’exclure les contenus générés artificiellement.

---

## ✨ Fonctionnalités

### Côté Client
- Navigation par **domaines** et **catégories**
- Consultation des détails des produits
- Publication d’annonces (authentification requise)
- Portefeuille virtuel (wallet)
- Profil utilisateur
- Commentaires et interactions

### Côté Administrateur
- Modération des publications en attente
- Gestion des domaines et catégories
- Validation / rejet des produits

### Intelligence Artificielle
- Détection d’images générées par IA (SDXL, Midjourney, DALL·E, etc.)
- Rejet automatique des photos artificielles

---

## 🛠️ Stack Technique

| Composant              | Technologie                                                          |
|------------------------|----------------------------------------------------------------------|
| Frontend               | Angular 20, PrimeNG, Tailwind CSS, Firebase, Chart.js, Quill         |
| Backend                | Spring Boot 3.5.6, Java 21, Spring Security, JWT, OAuth2, JPA/Hibernate |
| Base de données        | PostgreSQL 15                                                        |
| Détection d’images IA  | Flask + Hugging Face Transformers (Organika/sdxl-detector)           |
| Conteneurisation       | Docker & Docker Compose                                              |
| CI/CD                  | Jenkins                                                              |

---

## 📁 Structure du projet

```
bazart/
├── backend/                 # API Spring Boot
│   ├── src/main/java/com/marketplace/
│   │   ├── admin/           # Gestion administrative
│   │   ├── auth/            # Authentification JWT / OAuth2
│   │   ├── catalog/         # Domaines, catégories, produits
│   │   ├── interaction/     # Commentaires & interactions
│   │   ├── user/            # Gestion des utilisateurs
│   │   └── wallet/          # Portefeuille
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # Application Angular
│   ├── src/
│   │   ├── components/      # Client & Admin
│   │   ├── services/        # Auth, Wallet, Interactions...
│   │   └── environments/
│   └── Dockerfile
├── Image_Detector/          # Microservice Flask de détection IA
│   └── app.py
├── scripts/
│   └── init-data.sh         # Seed des domaines et catégories
├── docker-compose.yml
├── Jenkinsfile
└── .gitignore
```

---

## 🚀 Démarrage rapide

### Prérequis
- Docker & Docker Compose
- (Optionnel) Java 21, Maven, Node.js 20+ pour le développement local

### Lancement avec Docker (recommandé)

```bash
git clone https://github.com/BSK-202/bazart.git
cd bazart
docker-compose up --build
```

| Service          | URL                                            |
|------------------|------------------------------------------------|
| Frontend         | http://localhost:4200                          |
| Backend API      | http://localhost:8080                          |
| Health check     | http://localhost:8080/actuator/health          |
| Image Detector   | http://localhost:5000 (à lancer manuellement)  |

Les données initiales (domaines + catégories) sont insérées automatiquement via le service `init-data`.

### Développement local

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
ng serve
```

#### Image Detector
```bash
cd Image_Detector
pip install flask transformers torch pillow
python app.py
```

---

## 🔐 Configuration

### Backend (`application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bazart
spring.datasource.username=postgres
spring.datasource.password=admin
app.jwt.secret=MaSuperCleSecreteUltraLongueEtImprevisible_1234567890!@#$%
```

### Frontend (`environment.ts`)
```typescript
apiUrl: 'http://localhost:8080/api'
```

---

## 📊 Domaines & Catégories

| Domaine                    | Catégories exemples                              |
|----------------------------|--------------------------------------------------|
| Accessoires de Luxe        | Montres, Bijoux & Diamants, Pierres précieuses   |
| Art & Antiquités           | Meubles anciens, Peintures, Sculptures           |
| Collections                | Timbres, Pièces de monnaie, Vinyles              |
| Technologies Vintage       | Appareils radio/TV, instruments scientifiques    |

---

## 🔄 CI/CD (Jenkins)

Le pipeline effectue automatiquement :
1. Checkout de la branche `develop`
2. Build & tests du backend (Maven)
3. Build du frontend (npm)
4. Construction des images Docker
5. Déploiement de validation
6. Merge vers `master` + tagging en cas de succès

---

## 🧪 Tests

```bash
# Backend
cd backend && ./mvnw test

# Frontend
cd frontend && ng test
```

---

## 🤝 Contribution

1. Forkez le projet
2. Créez une branche (`git checkout -b feature/ma-fonctionnalite`)
3. Committez (`git commit -m 'feat: ajout de ma fonctionnalité'`)
4. Poussez (`git push origin feature/ma-fonctionnalite`)
5. Ouvrez une Pull Request vers `develop`

---

## 📄 Licence

Ce projet est sous licence privée. Tous droits réservés.

---

## 👥 Auteurs

Développé par l’équipe **BSK-202**.

---

**Bazart** — *L’art de l’authentique, expertisé et sécurisé.*
```
