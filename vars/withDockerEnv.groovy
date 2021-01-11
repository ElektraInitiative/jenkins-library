#!/usr/bin/env groovy

/** Run the passed closure in a docker environment
 *
 * Automatically takes care of docker registry authentication,
 * selecting a docker capable node,
 * checkout of scm and setting of useful Environment variables
 * @param image Docker image that should be used
 * @param opts List of DOCKER_OPTS that should be passed to docker
 * @param cl A closure that should be run inside the docker image
 */
def call(image, opts=[], cl) {
  node(PipelineConfig.instance.dockerNodeLabel) {
    def dockerArgs = ""
    if (opts.contains(DockerOpts.MOUNT_MIRROR)) {
      dockerArgs += "-v ${env.HOME}/git_mirrors:/home/jenkins/git_mirrors "
    }
    if (opts.contains(DockerOpts.PTRACE)) {
      dockerArgs += "--cap-add SYS_PTRACE "
    }
    docker.withRegistry("https://${PipelineConfig.instance.registry}",
                        'docker-hub-elektra-jenkins') {
      timeout(activity: true, time: 40, unit: 'MINUTES') {
        def cpu_count = dockerUtils.cpuCount()
        withEnv(["MAKEFLAGS='-j${cpu_count+2} -l${cpu_count*2}'",
                 "CTEST_PARALLEL_LEVEL='${cpu_count+2}'",
                 "XDG_CONFIG_HOME=${WORKSPACE}/xdg/user",
                 "XDG_CONFIG_DIRS=${WORKSPACE}/xdg/system"]) {
          echo "Starting ${STAGE_NAME} on ${NODE_NAME} using ${image.id}"
          checkout scm
          docker.image(image.id)
                .inside(dockerArgs) { cl() }
        }
      }
    }
  }
}
