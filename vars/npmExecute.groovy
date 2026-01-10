def call(String command = 'install', String projectPath = '.') {
    dir(projectPath) {
        echo "--- Executing NPM: ${command} ---"
        // Use --no-audit for speed if you are doing a separate security scan
        sh "npm ${command}"
    }
}