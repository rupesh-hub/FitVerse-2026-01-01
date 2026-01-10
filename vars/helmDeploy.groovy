def call(Map config = [:]) {
    def releaseName = config.releaseName ?: 'fitverse-backend'
    def chartPath   = config.chartPath   ?: './helm'
    def namespace   = config.namespace   ?: 'default'
    def version     = config.version

    echo "--- Deploying to Helm: ${releaseName} in Namespace: ${namespace} ---"

    // Added --create-namespace to ensure the target namespace exists
    sh """
        helm upgrade --install ${releaseName} ${chartPath} \
            --namespace ${namespace} \
            --create-namespace \
            --set backend.image.tag=${version} \
            --wait --timeout 5m0s
    """
}