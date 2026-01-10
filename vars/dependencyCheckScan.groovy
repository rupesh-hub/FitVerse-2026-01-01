def call(Map config = [:]) {
    // 1. Sensible Defaults
    def scanPath = config.scanPath ?: './'
    def failThreshold = config.failThreshold ?: '7.0' // CVSS score (7.0+ is HIGH)
    def odcInstallation = config.odcInstallation ?: 'OWASP'

    echo "--- Starting OWASP Dependency-Check Scan ---"

    // 2. Execution with Quality Gate
    // 'stopBuild: true' makes the step fail if the threshold is met
    dependencyCheck(
            odcInstallation: odcInstallation,
            additionalArguments: """
            --scan ${scanPath} 
            --failOnCVSS ${failThreshold} 
            --format HTML 
            --format XML
            --suppression dependency-check-suppression.xml
        """,
            stopBuild: true
    )

    // 3. Reliable Reporting
    // The publisher should ALWAYS run, even if the scan failed (hence outside the stopBuild logic in Scripted)
    // pattern: '**/dependency-check-report.xml' is the standard
    dependencyCheckPublisher(pattern: '**/dependency-check-report.xml')
}