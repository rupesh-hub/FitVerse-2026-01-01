def call(String scanPath = '.', String severity = 'HIGH,CRITICAL', int exitCode = 0) {
    echo "--- Scanning Filesystem: ${scanPath} ---"
    sh "trivy fs ${scanPath} --severity ${severity} --exit-code ${exitCode} --format table"
}