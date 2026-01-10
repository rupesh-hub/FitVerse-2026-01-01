def call(String projectPath = 'frontend') {
    dir(projectPath) {
        if (fileExists('package.json')) {
            // Extract version from package.json using jq
            return sh(script: "jq -r '.version' package.json", returnStdout: true).trim()
        } else if (fileExists('pom.xml')) {
            // Extract version from pom.xml
            return sh(script: "./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true).trim()
        }
        error "No version file found in ${projectPath}"
    }
}