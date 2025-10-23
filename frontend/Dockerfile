# Étape de build
FROM node:20-bullseye AS build

WORKDIR /app

# Copier les fichiers de configuration
COPY package*.json ./
COPY angular.json ./
COPY tsconfig*.json ./

# Installer les dépendances
RUN npm ci

# Copier le code source
COPY . .

# Build simple - utilisera la configuration "development" de angular.json
RUN npm run build

# Étape de production
FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
