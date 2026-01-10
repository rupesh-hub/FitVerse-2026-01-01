def call(Map config = [:]) {
    // 1. Defaults & Parameter Handling
    def apiEnv = config.apiEnv ?: 'SonarQube' // The name set in Jenkins System Config
    def projectName = config.projectName
    def projectKey = config.projectKey
    def extraArgs = config.extraArgs ?: ""

    // 2. Dynamic Tool Resolution
    // Avoid hardcoding $SONAR_HOME; use the Jenkins Tool name instead
    def scannerHome = tool 'SonarScanner'

    // 3. Analysis Execution
    withSonarQubeEnv(apiEnv) {
        sh """
            ${scannerHome}/bin/sonar-scanner \
                -Dsonar.projectName=${projectName} \
                -Dsonar.projectKey=${projectKey} \
                -Dsonar.sources=. \
                -Dsonar.java.binaries=**/target/classes \
                ${extraArgs}
        """
    }

    // 4. The "Industry Standard" Quality Gate
    // This pauses the pipeline until SonarQube returns a PASS/FAIL status
    timeout(time: 5, unit: 'MINUTES') {
        def qg = waitForQualityGate()
        if (qg.status != 'OK') {
            error "Pipeline aborted due to Quality Gate failure: ${qg.status}"
        }
    }
}
