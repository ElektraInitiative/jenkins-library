#!/usr/bin/env groovy
/**
 * dockerUtils.groovy
 *
 * This file contains all docker related methods
 */

import java.text.SimpleDateFormat

/* Generate Stages to pull all docker images */
def generateDockerPullStages(dockerImages) {
  def tasks = [:]
  dockerImages.each { key, image ->
    if(image.autobuild) {
      tasks << pullImageStage(image)
    }
  }
  return tasks
}

def generateDockerBuildStages(dockerImages) {
  def tasks = [:]
  dockerImages.each { key, image ->
    if(image.autobuild && !image.exists) {
      tasks << buildImageStage(image)
    }
  }
  return tasks
}

/* Create a new Docker Image description
 *
 * @param name Name of the image, will be extended with registry, a common
 *             prefix and a tag
 * @param idFun Closure describing how the image id should be formatted
 *                      (see idTesting() / idWebsite())
 * @param context Build context for the docker build (base directory that will
 *                be sent to the docker agent). Relative to the current working
 *                directory.
 * @param file Path to Dockerfile relative to the current working directory.
 * @param autobuild If it should be automatically build at the start of the
 *                  Jenkins run. If false it can be build manually
 *                  (see buildImageStage()).
 */
def createDockerImageDesc(name, idFun, context, file, autobuild=true) {
  def prefix = 'build-elektra'
  def fullName = "${PipelineConfig.instance.registry}/${prefix}-${name}"
  def map = [
    name: fullName,
    context: context,
    file: file,
    autobuild: autobuild,
    exists: false
  ]
  return idFun(map)
}

/* Returns a stage that tries to pull an image
 *
 * Also sets IMAGES_TO_BUILD to true if an image can not be found
 * to indicated that rebuilds are needed
 * @param image Map identifying which image to pull
 */
def pullImageStage(image) {
  def taskname = "pull/${image.id}/"
  return [(taskname): {
    stage(taskname) {
      node(PipelineConfig.instance.dockerNodeLabel) {
        echo "Starting ${env.STAGE_NAME} on ${env.NODE_NAME}"
        docker.withRegistry("https://${PipelineConfig.instance.registry}",
                            'docker-hub-elektra-jenkins') {
          try {
            docker.image(image.id).pull()
            echo "Found existing image"
            image.exists = true
          } catch(e) {
            echo "Detected changes"
            image.exists = false
          }
        }
      }
    }
  }]
}

/* Returns a map with a closure that builds image
 *
 * @param image Image that needs to be build
 */
def buildImageStage(image) {
  def taskname = "build/${image.id}/"
  return [(taskname): {
    stage(taskname) {
      node(PipelineConfig.instance.dockerNodeLabel) {
        echo "Starting ${env.STAGE_NAME} on ${env.NODE_NAME}"
        checkout scm
        buildImage(image)
      }
    }
  }]
}

def buildImage(image) {
  docker.withRegistry("https://${PipelineConfig.instance.registry}",
                  'docker-hub-elektra-jenkins') {
    def uid = getUid()
    def gid = getGid()
    def cpus = cpuCount()
    def i = docker.build(
      image.id,"""\
--pull \
--build-arg JENKINS_GROUPID=${gid} \
--build-arg JENKINS_USERID=${uid} \
--build-arg PARALLEL=${cpus} \
-f ${image.file} ${image.context}"""
    )
    i.push()
  }
}

/* Build image ID of docker images used for tests
 *
 * We use identifiers in the form of name:yyyyMM-hash
 * The hash is build from reading the Dockerfile. Hence it needs to be
 * checked out before it can be calculated.
 * @param imageMap Map identifying an docker image (see DOCKER_IMAGES)
 * @param now current date
 */
def idTesting(imageMap) {
  def cs = checksum(imageMap.file)
  def dateString = dateFormatter(PipelineConfig.instance.now)
  imageMap.id = "${imageMap.name}:${dateString}-${cs}"
  return imageMap
}

/* Build id for artifact images (website, webui, images with installed elektra)
 *
 * @param imageMap Map identifying an docker image
 */
def idArtifact(imageMap) {
  imageMap.id = "${imageMap.name}:${env.JOB_NAME}_${env.BUILD_NUMBER}"
  return imageMap
}

/* Format the date input
 * @param date Date to format
 */
def dateFormatter(date) {
  df = new SimpleDateFormat("yyyyMM")
  return df.format(date)
}

/* Generate the checksum of a file
 * @param file File to generate a checksum for
 */
def checksum(file) {
  // Used to identify if a Dockerfile changed
  // TODO expand to use more than one file if Dockerfile ever depends on
  //      external files
  return sh(returnStdout: true,
            script: "cat $file | sha256sum | dd bs=1 count=64 status=none")
         .trim()
}

/* Get the current users uid
 */
def getUid() {
  return sh(returnStdout: true, script: 'id -u').trim()
}

/* Get the current users gid
 */
def getGid() {
  return sh(returnStdout: true, script: 'id -g').trim()
}

/* Get cpu count
 */
def cpuCount() {
  return sh(returnStdout: true,
            script: 'grep -c ^processor /proc/cpuinfo').trim() as Integer
}
