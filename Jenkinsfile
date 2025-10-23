pipeline {
    agent any
    
    environment {
        PROJECT_NAME = 'marketplace-auctions'
        FRONTEND_DIR = 'frontend'
        BACKEND_DIR = 'marketplace-auctions'
        GIT_DEVELOP_BRANCH = 'develop'
        GIT_MASTER_BRANCH = 'master'
    }
    
    stages {
        // Étape 1: Checkout depuis develop
        stage('Checkout Develop') {
            steps {
                git branch: "${env.GIT_DEVELOP_BRANCH}", 
                    url: 'https://github.com/Chebila/marketplace-auctions.git',
                    credentialsId: 'github-token-marketplace'
                script {
                    currentBuild.displayName = "SPRINT-RELEASE #${env.BUILD_NUMBER}"
                    currentBuild.description = "Validation ${env.GIT_DEVELOP_BRANCH} → ${env.GIT_MASTER_BRANCH}"
                    
                    // Récupérer le dernier commit message
                    sh 'git log -1 --pretty=%B > commit_message.txt'
                    env.COMMIT_MESSAGE = readFile('commit_message.txt').trim()
                }
            }
        }
        
        // Étape 2: Build Backend Spring Boot
        stage('Build Backend') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh '''
                        echo "🔨 Construction du Backend Spring Boot..."
                        mvn clean compile
                        mvn package -DskipTests
                    '''
                }
            }
            post {
                success {
                    echo "✅ Backend construit avec succès"
                    archiveArtifacts artifacts: "${env.BACKEND_DIR}/target/*.jar", fingerprint: true
                }
            }
        }
        
        // Étape 3: Tests Backend
        stage('Test Backend') {
            steps {
                dir(env.BACKEND_DIR) {
                    sh '''
                        echo "🧪 Exécution des tests Backend..."
                        mvn test
                    '''
                }
            }
            post {
                always {
                    junit "${env.BACKEND_DIR}/target/surefire-reports/*.xml"
                }
            }
        }
        
        // Étape 4: Build Frontend Angular
        stage('Build Frontend') {
            steps {
                dir(env.FRONTEND_DIR) {
                    sh '''
                        echo "🔨 Construction du Frontend Angular..."
                        npm ci --silent
                        npm run build
                        echo "✅ Build Frontend terminé"
                    '''
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: "${env.FRONTEND_DIR}/dist/**/*", fingerprint: true
                }
            }
        }
        
        // Étape 5: Build Docker Images
        stage('Build Docker Images') {
            steps {
                script {
                    echo "🐳 Construction des images Docker..."
                    
                    // Build Backend
                    dir(env.BACKEND_DIR) {
                        sh "docker build -t marketplace-backend:sprint-${env.BUILD_NUMBER} ."
                    }
                    
                    // Build Frontend
                    dir(env.FRONTEND_DIR) {
                        sh "docker build -t marketplace-frontend:sprint-${env.BUILD_NUMBER} ."
                    }
                }
            }
        }
        
        // Étape 6: Déploiement de Validation
        stage('Validation Deployment') {
            steps {
                script {
                    echo "🚀 Déploiement de validation..."
                    sh 'docker-compose down --remove-orphans || true'
                    sh 'docker-compose up -d --build'
                    
                    sh '''
                        echo "⏳ Attente du démarrage..."
                        sleep 30
                        curl -f http://localhost:8080/actuator/health && echo "✅ Backend OK"
                    '''
                }
            }
        }
        
        // Étape 7: Merge Vers Master
        stage('Merge to Master') {
            when {
                expression { 
                    currentBuild.result == null || currentBuild.result == 'SUCCESS' 
                }
            }
            steps {
                script {
                    echo "🔄 Fusion vers Master..."
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'github-token-marketplace',
                        usernameVariable: 'GIT_USERNAME',
                        passwordVariable: 'GIT_PASSWORD'
                    )]) {
                        sh """
                            git config --global user.email "jenkins@marketplace.com"
                            git config --global user.name "Jenkins CI"
                            
                            git checkout ${env.GIT_MASTER_BRANCH}
                            git pull origin ${env.GIT_MASTER_BRANCH}
                            git merge origin/${env.GIT_DEVELOP_BRANCH} --no-ff -m "🚀 RELEASE: Sprint #${env.BUILD_NUMBER}"
                            git push origin ${env.GIT_MASTER_BRANCH}
                            
                            git tag -a "sprint-${env.BUILD_NUMBER}" -m "Release ${env.BUILD_NUMBER}"
                            git push origin "sprint-${env.BUILD_NUMBER}"
                        """
                    }
                }
            }
        }
    }
    
    post {
        always {
            sh 'docker-compose down --remove-orphans || true'
        }
        success {
            echo "🎉 SPRINT VALIDÉ ET MERGÉ VERS MASTER!"
        }
        failure {
            echo "💥 VALIDATION ÉCHOUÉE"
        }
    }
    
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    
    tools {
        maven 'M3'
        jdk 'JDK21'
    }
}