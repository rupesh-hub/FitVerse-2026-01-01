def call(Object images, String credentialsId = 'dockerhub') {
    // 1. Normalize input: convert a single String to a List so the loop works for both
    def imageList = (images instanceof String) ? [images] : images

    withCredentials([usernamePassword(credentialsId: credentialsId,
            passwordVariable: 'DOCKER_PASSWD',
            usernameVariable: 'DOCKER_USER')]) {

        echo "--- Logging into Docker Hub ---"
        // Use stdin to prevent password leaking in logs/process list
        sh "echo '${DOCKER_PASSWD}' | docker login -u '${DOCKER_USER}' --password-stdin"

        try {
            for (String img : imageList) {
                echo "Processing image: ${img}"

                // Professional practice: don't hardcode 'springboot-application'.
                // If the image isn't already prefixed with the username, tag it.
                if (!img.contains("/")) {
                    def fullImageName = "${DOCKER_USER}/${img}"
                    sh "docker tag ${img} ${fullImageName}"
                    sh "docker push ${fullImageName}"
                } else {
                    sh "docker push ${img}"
                }
            }
        } finally {
            echo "--- Logging out ---"
            sh "docker logout"
        }
    }
}

/**
 pushImages("my-cool-app:v1.0")
 pushImages(["web-app:1.2.3", "web-app:latest"])
 */