#!/usr/bin/env groovy
/**
 * utils.groovy
 *
 * This file contains shared methods which
 * are not docker and release related
 */

/* Run cmake
 * @param directory Basedir for cmake
 * @param argsMap Map of arguments for cmake
 */
def cmake(String directory, Map argsMap) {
  def argsStr = ""
  argsMap.each { key, value ->
    argsStr += "-D$key=\"$value\" "
  }
  sh("cmake $argsStr $directory")
}


/* Archives files located in paths
 *
 * Automatically prefixes with the current STAGE_NAME to identify where the
 * file was created.
 * @param paths List of paths to be archived
 */
def archive(paths) {
  echo "Start archivation"
  if (paths) {
    def prefix = "artifacts/"
    def dest = "${prefix}${env.STAGE_NAME}/"
    sh "mkdir -p ${dest}"
    paths.each { path ->
        sh "cp -v ${path} ${dest} || true"
    }
    archiveArtifacts artifacts: "${prefix}**",
                     fingerprint: true,
                     allowEmptyArchive: true
  } else {
    echo "No Artifacts to archive"
  }
  echo "Finish archivation"
}
