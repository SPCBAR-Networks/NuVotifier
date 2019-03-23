pipeline {
    agent {
        docker { image 'openjdk:12-jdk' }
    }

    environment {
        SNAPSHOT_REPO   = credentials('ibj-nexus-snapshot-repo')
        RELEASE_REPO    = credentials('ibj-nexus-release-repo')
        RAW_UPLOAD_PATH = credentials('ibj-nexus-raw-path')
        REPO            = credentials('ibj-nexus-access')
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build --no-daemon'            
            }
        }
        
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'            
            }
        }
        
        stage('Publish') {
            when {
                branch "master"
            }
            environment {
                PUBLISH = 'true'

            }
            steps {
                sh './gradlew publishVersionedPublicationToIbjRepository'
                sh './gradlew publishLatestToIbjBlockRaw'
            }
        }
    }
}