pipeline{
    agent any
    environment {
        DOCKER_REPO = "saritaryadav"
        DOCKER_USER = "node-app"
        IMAGE_NAME = "node-app"
        CONTAINER_NAME = "node-container"
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
                sh '''
                docker build -t ${DOCKER_REPO}:${BUILD_NUMBER} .
                ''' 
            }
        }
        stage('Docker login'){
            steps{
               withCredentials([
                        usernamePassword(
                            credentialsId: 'new-docker-cred',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) 
                    {
                        sh 'docker login -u ${DOCKER_USERNAME} -p ${DOCKER_PASSWORD}'
                    }
                }
            }
        stage('Docker push'){
            steps{
               withCredentials([
                        usernamePassword(
                            credentialsId: 'new-docker-cred',
                            usernameVariable: 'DOCKER_USERNAME',
                            passwordVariable: 'DOCKER_PASSWORD'
                        )
                    ]) 
                    {
                        sh '''
                            docker tag ${DOCKER_REPO}:${BUILD_NUMBER} \
                            ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}

                            docker push ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}
                        '''
                    }
                }
            }
            stage('Image-Name-change'){
                steps {
          
                    sh '''
                     sed -i "s|saritaryadav/node-app:latest|${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}|g" k8s/deployment.yaml
                    '''
                    sh 'cat k8s/deployment.yaml'
                }
            }
            stage('Deploy to cluster'){
                steps{
                    withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'aws_creds', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                         sh '''
                            aws eks update-kubeconfig --name ${CLUSTER_NAME} --region ${REGION}
                            
                            kubectl get nodes
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
        post {

                success {
                    echo "Application deployed successfully to Amazon EKS."
                }

                failure {
                    echo "Pipeline failed. Please check the logs."
                }

                always {
                    cleanWs()
                }
            }

    }





