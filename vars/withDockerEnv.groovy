#!/usr/bin/env groovy

/**
 * Run the passed closure in a docker environment
 *
 * Calls `withDockerEnvWithoutNode` on a docker capable node
 *
 * @param image Docker image that should be used
 * @param opts List of DOCKER_OPTS that should be passed to docker
 * @param postCl Closure that will be run outside the docker image
 *               after the Closure `cl` is run inside the docker image.
 * @param cl A closure that should be run inside the docker image
 */
def call(image, opts=[], postCl= { }, cl) {
  node(PipelineConfig.instance.dockerNodeLabel) {
    withDockerEnvWithoutNode(image, opts, postCl, cl)
  }
}

/**
 * Run the passed closure in a docker environment on a docker
 * capable node.
 *
 * Automatically takes care of docker registry authentication,
 * checkout of scm and setting of useful Environment variables
 * @param image Docker image that should be used
 * @param opts List of DOCKER_OPTS that should be passed to docker
 * @param postCl Closure that will be run outside the docker image
 *               after the Closure `cl` is run inside the docker image.
 * @param cl A closure that should be run inside the docker image
 */
def withDockerEnvWithoutNode(image, opts=[], postCl= { }, cl) {
  def dockerArgs = ''
  if (opts.contains(DockerOpts.MOUNT_MIRROR)) {
    dockerArgs += "-v ${env.HOME}/git_mirrors:/home/jenkins/git_mirrors "
  }
  if (opts.contains(DockerOpts.PTRACE)) {
    dockerArgs += '--cap-add SYS_PTRACE '
  }
  def uid = dockerUtils.getUid()
  def gid = dockerUtils.getGid()
  dockerArgs += " --mount type=tmpfs,destination=${WORKSPACE}/xdg,uid=${uid},gid=${gid} "
  dockerArgs += " --mount type=tmpfs,destination=${WORKSPACE}/config,uid=${uid},gid=${gid} "
  docker.withRegistry("https://${PipelineConfig.instance.registry}",
                      'docker-hub-elektra-jenkins') {
    timeout(activity: true, time: 60, unit: 'MINUTES') {
      def cpu_count = dockerUtils.cpuCount()
      withEnv(["MAKEFLAGS='-j${cpu_count+2} -l${cpu_count*2}'",
               "CTEST_PARALLEL_LEVEL='${cpu_count+2}'",
               "XDG_CONFIG_HOME=${WORKSPACE}/xdg/user",
               "XDG_CONFIG_DIRS=${WORKSPACE}/xdg/system"]) {
        echo "Starting ${STAGE_NAME} on ${NODE_NAME} using ${image.id}"
        if (!opts.contains(DockerOpts.NO_CHECKOUT)) {
            checkout scm
        }
        try {
            // Try pre-pulling the image once to avoid temporary docker hub errors
            docker.image(image.id).pull()
        } catch (Exception ex) {
            sleep 30
        }
        docker.image(image.id)
              .inside(dockerArgs) { cl() }
      }
    }
  }
  postCl()
  cleanWs()
}
