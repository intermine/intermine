pipeline {
    agent any
    tools {
        gradle "GRADLE_LATEST"
    }
    stages {
        stage('Gradle') {
            steps {
                sh 'gradle --version'
            }
        }
        
        stage('Build') {
            steps {
                echo 'Running build automation'
                sh './gradlew build --no-daemon'
                archiveArtifacts artifacts: 'dist/intermine.zip'
            }
        }
    }
}
