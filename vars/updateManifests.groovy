def call(Map config = [:]) {
    // Dynamic parameters
    def serviceName      = config.serviceName // 'backend' or 'frontend'
    def version          = config.version
    def dockerUser       = config.dockerUser ?: 'rupesh1997'
    def gitCredentialsId = config.gitCredentialsId ?: 'git-creds'

    // Derived variables
    def fullServiceName  = "fitverse-${serviceName}"
    def imageName        = "${dockerUser}/${fullServiceName}:${version}"
    def k8sFilePath      = "kubernetes/${serviceName}-deployment.yaml"

    echo "--- Dynamic CD Update: ${fullServiceName} to version ${version} ---"

    sshagent([gitCredentialsId]) {
        // 1. Git Setup
        sh """
            git config user.name "jenkins-cd-bot"
            git config user.email "jenkins-cd@alfarays.com"
        """

        // 2. Dynamic YAML Updates using yq
        sh """
            # Update all Docker Compose variations dynamically based on serviceName
            find docker/docker-compose -name "docker-compose.yaml" | xargs -I {} \
                yq -i '.services."${fullServiceName}".image = "${imageName}"' {}

            # Update Kubernetes Deployment (Select container by name)
            if [ -f "${k8sFilePath}" ]; then
                yq -i '(.spec.template.spec.containers[] | select(.name == "${fullServiceName}") | .image) = "${imageName}"' ${k8sFilePath}
            fi

            # Update Helm Values (Targets .backend.image.tag or .frontend.image.tag)
            if [ -f "helm/values.yaml" ]; then
                yq -i '.${serviceName}.image.tag = "${version}"' helm/values.yaml
            fi
        """

        // 3. Robust Git Push
        sh """
            git add .
            if git diff --staged --quiet; then
                echo "No changes detected for ${fullServiceName}."
            else
                git commit -m "chore(${serviceName}): deploy version ${version} [skip ci]"
                git pull --rebase origin main
                git push origin main
            fi
        """
    }
}