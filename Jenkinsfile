pipeline {
    agent {
        docker {
            image 'maven:3-jdk-8-slim'
            args '-v ${JENKINS_HOME}/.m2:${JENKINS_HOME}/.m2'
        }
    }
    options {
        skipStagesAfterUnstable()
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -s ${MAVEN_SETTINGS} -DskipTests clean package'
            }
        }
        stage('Deploy') {
            steps {
                sh 'mvn -B -s ${MAVEN_SETTINGS} -Dmaven.test.skip=true clean package'
            } 
            post {
            	success {
                	dir('target') {
                    	sh 'cp *.zip /var/forge_repository'
                	}
            	}
            }
        }
        stage('SonarQube analysis') {
            steps {
                sh 'mvn -B -s ${MAVEN_SETTINGS} clean verify sonar:sonar'
            }
        }
    }
    post {
        success {
            emailext (
                subject: "Success Jenkins pipeline - ${env.JOB_NAME}",
                body: "${env.BUILD_URL}",
                to: "$ADMIN_EMAIL",
                from: "$ADMIN_EMAIL"
            )
        }
        failure {
            emailext (
                subject: "Failure Jenkins pipeline - ${env.JOB_NAME}",
                body: "${env.BUILD_URL}",
                to: "$ADMIN_EMAIL",
                from: "$ADMIN_EMAIL"
            )
        }
    }
}
