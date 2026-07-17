pipeline {
    agent any

    environment {
        DOCKER_REPO = "sariiryadav1323/node-app"
        CLUSTER_NAME = "demo-saritaekscluster"
        REGION = "ap-south-1"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/saritaRyadav/node-app.git'
            }
        }

        stage('Verify Environment') {
            steps {
                sh '''
                echo "Node Version:"
                node -v

                echo "NPM Version:"
                npm -v

                echo "Docker Version:"
                docker --version
                '''
            }
        }

        stage('Install Dependencies') {
            steps {
                sh 'npm install'
            }
        }

        stage('Run Tests') {
            steps {
                sh 'npm test'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t ${DOCKER_REPO}:${BUILD_NUMBER} .'
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'new-docker-cred',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin'
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                sh 'docker push ${DOCKER_REPO}:${BUILD_NUMBER}'
            }
        }

        stage('Update Deployment Manifest') {
            steps {
                sh '''
                sed -i "s|image: .*|image: ${DOCKER_REPO}:${BUILD_NUMBER}|g" k8s/deployment.yaml
                cat k8s/deployment.yaml
                '''
            }
        }

        stage('Deploy to Cluster') {
            steps {
                withCredentials([aws(credentialsId: 'aws_creds', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                    sh '''
                    aws eks update-kubeconfig --name ${CLUSTER_NAME} --region ${REGION}
                    kubectl apply -f k8s/deployment.yaml
                    kubectl apply -f k8s/service.yaml
                    kubectl get pods
                    kubectl get deployment
                    kubectl get svc
                    '''
                }
            }
        }
    }
}





