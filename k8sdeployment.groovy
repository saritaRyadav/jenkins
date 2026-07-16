pipeline {
    agent    {
        
        label 'node-agent'
    }

    environment {
        DOCKER_REPO = "saritaRyadav"
        DOCKER_USER = "node-app"
        IMAGE_NAME = "node-app"
        CONTAINER_NAME = "node-container"

        AWS_REGION = "ap-south-1"
        CLUSTER_NAME = "demo-saritaekscluster"
        NODEGROUP_NAME = "demo-workers"
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
                node -v
                npm -v
                docker --version
                aws --version
                kubectl version --client
                eksctl version
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
                docker build -t ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Docker Login') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'new-docker-cred' ,
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sh '''
                    echo "$DOCKER_PASSWORD" | docker login \
                    -u "$DOCKER_USERNAME" --password-stdin
                    '''
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                sh '''
                docker push ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}
                '''
            }
        }

        stage('Update Manifest') {
            steps {
                sh """
                sed -i 's|image: .*|image: ${DOCKER_REPO}/${DOCKER_USER}:${BUILD_NUMBER}|' k8s/deployment.yaml
                """

                sh "cat k8s/deployment.yaml"
            }
        }

        // -----------------------------
        // Create EKS Cluster
        // -----------------------------
        stage('Create EKS Cluster') {
            steps {
                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'aws_creds']
                ]) {

                    sh '''
                    if aws eks describe-cluster \
                        --name ${CLUSTER_NAME} \
                        --region ${AWS_REGION} >/dev/null 2>&1
                    then
                        echo "EKS Cluster already exists."
                    else
                        echo "Creating EKS Cluster..."

                        eksctl create cluster \
                            --name ${CLUSTER_NAME} \
                            --region ${AWS_REGION} \
                            --nodegroup-name ${NODEGROUP_NAME} \
                            --node-type c7i-flex.large \
                            --nodes 2 \
                            --nodes-min 1 \
                            --nodes-max 3 \
                            --managed

                        echo "Cluster Created Successfully."
                    fi
                    '''
                }
            }
        }

        stage('Configure EKS & Deploy') {
            steps {

                withCredentials([
                    [$class: 'AmazonWebServicesCredentialsBinding',
                    credentialsId: 'aws_creds']
                ]) {

                    sh """
                    aws eks update-kubeconfig \
                        --region ${AWS_REGION} \
                        --name ${CLUSTER_NAME}

                    kubectl apply -f k8s/

                    kubectl rollout status deployment/node-app

                    kubectl get nodes
                    kubectl get pods
                    kubectl get svc
                    """
                }
            }
        }
    }

    post {
        always {
            sh '''
            docker logout || true
            docker image prune -f || true
            '''
        }

        success {
            echo "Pipeline completed successfully."
        }

        failure {
            echo "Pipeline failed."
        }
    }
}