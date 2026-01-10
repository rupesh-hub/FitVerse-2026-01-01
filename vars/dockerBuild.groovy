def call(String repository, List tags, Map buildArgs = [:], String dockerfilePath = 'Dockerfile') {
    // 1. Construct build-args
    def argsString = buildArgs.collect { "--build-arg ${it.key}=${it.value}" }.join(' ')

    // 2. Construct tags
    def tagArgs = tags.collect { "-t ${repository}:${it}" }.join(' ')

    // 3. Build command: specifying the path to the Dockerfile (-f)
    // but using the current directory (.) as context
    sh "docker build ${argsString} ${tagArgs} -f ${dockerfilePath} ."
}

