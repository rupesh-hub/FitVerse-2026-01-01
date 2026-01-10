def call(String url, String branch, String credentialsId = 'git-creds') {
    checkout([$class: 'GitSCM',
              branches: [[name: "*/${branch}"]],
              userRemoteConfigs: [[url: url, credentialsId: credentialsId]]
    ])
}