def call(String goals = 'clean package', String projectPath = '.') {
    dir(projectPath) {
        echo "--- Executing Maven: ${goals} ---"
        sh "./mvnw ${goals}"
    }
}