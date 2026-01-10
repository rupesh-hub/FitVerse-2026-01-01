def call(String[] images, String severity = 'HIGH,CRITICAL', Boolean failBuild = true) {
    echo "--- Starting Trivy Scan for ${images.size()} images ---"

    // 0 = report only, 1 = fail pipeline if vulnerabilities are found
    def exitCode = failBuild ? "1" : "0"

    for (String image : images) {
        // Create a unique filename for the report by replacing ':' and '/' with '-'
        def reportName = "trivy-report-${image.replaceAll(/[:\/]/, '-')}.txt"

        echo "Scanning: ${image}"

        // We use 'tee' to show results in the console AND save to a file
        sh """
            trivy image \
                --severity ${severity} \
                --exit-code ${exitCode} \
                --format table \
                ${image} | tee ${reportName}
        """

        // Archive the individual report so it appears in the Jenkins Build artifacts
        archiveArtifacts artifacts: reportName, allowEmptyArchive: true
    }
}