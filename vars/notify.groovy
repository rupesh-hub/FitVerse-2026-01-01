def call(String status, String recipient) {
    // 1. Determine color and emoji based on status
    def subject = "${status}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
    def summary = "Build ${status} at ${env.BUILD_URL}"

    // 2. Capture the failure cause (if any)
    def cause = ""
    if (status == 'FAILURE') {
        // Gets the last few lines of the log to identify why it failed
        cause = "Possible Failure Cause: " + currentBuild.rawBuild.getLog(20).join('\n')
    }

    // 3. Send the Email
    // Requires the 'Email Extension Plugin' (standard in most Jenkins setups)
    emailext (
            to: recipient,
            subject: subject,
            body: """
                <h3>${subject}</h3>
                <p><strong>Status:</strong> ${status}</p>
                <p><strong>Summary:</strong> ${summary}</p>
                <p><strong>Project:</strong> ${env.JOB_NAME}</p>
                <p><strong>Build Number:</strong> ${env.BUILD_NUMBER}</p>
                <p><strong>Console Output:</strong> <a href="${env.BUILD_URL}/console">${env.BUILD_URL}</a></p>
                <hr/>
                <pre style='font-family: monospace;'>${cause}</pre>
             """,
            mimeType: 'text/html'
    )
}

/**
 post {
     success {
        archiveArtifacts artifacts: '*.xml', followSymlinks: false

        // Use your library for notification
        notifyStatus('SUCCESS', 'dev-team@example.com')

        // Trigger CD Job
        build job: "BankApp-CD", parameters: [
            string(name: 'DOCKER_TAG', value: "${params.DOCKER_TAG}")
        ]
     }
     failure {
        // Use your library to alert on failure with log causes
        notifyStatus('FAILURE', 'dev-ops-alerts@example.com')
     }
 }
 */