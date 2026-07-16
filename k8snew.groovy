pipeline {
    agent any

    environment {
        DOCKER_REPO = "saritaRyadav"
        DOCKER_USER = "node-app"
        IMAGE_NAME = "node-app"
        CONTAINER_NAME = "node-container"
        AWS_REGION = "ap-south-1"
        CLUSTER_NAME = "demo-saritaekscluster"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/saritaRyadav/jenkins.git'
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
                sh "docker build -t ${DOCKER_REPO}:${BUILD_NUMBER} ."
            }
        }

        stage('Docker login') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'new-docker-cred',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}'
                }
            }
        }

        stage('Docker push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'new-docker-cred',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh '''
                        docker tag ${DOCKER_REPO}:${BUILD_NUMBER} ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}
                        docker push ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}
                    '''
                }
            }
        }

        stage('Update Manifest') {
            steps {
                sh "sed -i 's|image: .*|image: ${IMAGE_NAME}:${BUILD_NUMBER}|' jenkins/deployment.yaml"
                sh 'cat jenkins/deployment.yaml'
            }
        }

        stage('Configure EKS') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws_creds']]) {
                    sh """
                        aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}
                        kubectl apply -f jenkins/*
                    """
                }
            }
        }
    }
}