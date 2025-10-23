# ============================
# 🌱 Étape 1 : Build de l'application
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Définir le dossier de travail
WORKDIR /app

# Copier uniquement le fichier pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# COPIER LES ASSETS/IMAGES AVANT LE BUILD
COPY assets ./assets

# Copier le reste du code source
COPY src ./src

# Compiler et packager l'application
RUN mvn clean package -DskipTests

# ============================
# 🚀 Étape 2 : Image finale (légère)
# ============================
FROM eclipse-temurin:21-jdk-jammy

# Définir le dossier de travail dans le conteneur
WORKDIR /app

# Copier le jar depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

# COPIER LES ASSETS/IMAGES DANS L'IMAGE FINALE
COPY --from=builder /app/assets ./assets

# Exposer le port utilisé par ton Spring Boot (par défaut 8080)
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]