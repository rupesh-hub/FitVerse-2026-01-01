def call(String[] images) {
    withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'password', usernameVariable: 'username')]) {
        sh "docker login -u ${username} -p ${password}"
    }

    for (String image : images) {
        sh "docker push ${image}"
    }
}