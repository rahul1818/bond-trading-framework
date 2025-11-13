pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    echo "Building project: ${env.JOB_NAME}"
                    echo "Build number: ${env.BUILD_NUMBER}"
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "Compiling Java source code..."
                    mvn clean compile -B
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    echo "Running Robot Framework tests..."
                    mvn verify -B
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/robotframework/*.html, target/robotframework/*.xml', 
                                      allowEmptyArchive: true
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                sh '''
                    echo "Generating Allure report..."
                    mvn allure:report -B
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/allure-report/**/*', 
                                      allowEmptyArchive: true
                    
                    script {
                        if (fileExists('target/allure-report/index.html')) {
                            publishHTML([
                                reportDir: 'target/allure-report',
                                reportFiles: 'index.html',
                                reportName: 'Allure Test Report',
                                keepAll: true
                            ])
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (fileExists('target/robotframework/TEST-tests.xml')) {
                    junit 'target/robotframework/TEST-tests.xml'
                }
            }
            
            archiveArtifacts artifacts: '''
                target/robotframework/*.html,
                target/robotframework/*.xml,
                target/allure-report/**/*,
                target/robotframework/allure-results/**/*
            ''', 
            allowEmptyArchive: true
        }
        
        success {
            echo "✅ Build successful! All tests passed."
            echo "📊 View Allure report in the build artifacts"
        }
        
        failure {
            echo "❌ Build failed! Check the logs for details."
        }
        
        unstable {
            echo "⚠️ Build unstable! Some tests may have failed."
        }
    }
}
