#!/usr/bin/env groovy
/**
 * release.groovy
 *
 * This file contains shared methods for package releases
 */

/** Publish all DEB packages and updates the repo
 *
 * Also deletes older versions from the repo.
 * @param remote where the repository is located
 * @param remotedir must match with the directory tree inside the remote
 * @param repoName name of the repository
 * @param repoPrefix prefix of the repository
 * @param releaseVersion version of the package without the revision postfix
 * @param packageRevision revision number of the package
 * @param projectBaseDirPrefix directory after ${WORKSPACE} until project directory
 *                             Defaults to "" for the case that project is checked out
 *                             in current directory
 */
def publishDebPackages(remote, remotedir, repoName, repoPrefix,
                       releaseVersion, packageRevision='1',
                       projectBaseDirPrefix='') {
  sshPublisher(
    publishers: [
      sshPublisherDesc(
        verbose: true,
        configName: remote,
        transfers: [
          sshTransfer(
            sourceFiles: '*.deb',
            remoteDirectory: remotedir
          ),
          sshTransfer(
            sourceFiles: '*.ddeb',
            remoteDirectory: remotedir
          )
        ]
      )
    ],
    failOnError: true
  )

  // This step is necessary since running the commands to update
  // the aptly repo directly in the `execCommand` results in
  // invalid interpolation behaviour of `$Version` which is
  // caused by Grovvys GString.
  // Escaping could not solve this issue.
  dir("${WORKSPACE}/${projectBaseDirPrefix}/scripts/release/") {
    sshPublisher(
      publishers: [
        sshPublisherDesc(
          verbose: true,
          configName: remote,
          transfers: [
            sshTransfer(
              sourceFiles: 'update-aptly-repo.sh',
              remoteDirectory: 'packaging/scripts/',
              execCommand: """\
sh /srv/libelektra/packaging/scripts/update-aptly-repo.sh \
${repoName} ${repoPrefix} ${releaseVersion} ${packageRevision}"""
            ),
          ]
        )
      ],
      failOnError: true
    )
  }
}

/** Publish all RPM packages and updates the repo
 *
 * Also deletes older versions from the repo.
 * @param remote where the repository is located
 * @param remotedir must match with the directory tree inside the remote
 * @param repoName name of the repository
 * @param repoPrefix prefix of the repository
 * @param releaseVersion version of the package without the revision postfix
 * @param packageRevision revision number of the package
 * @param projectBaseDirPrefix directory after ${WORKSPACE} until project directory
 */
def publishRpmPackages(remote, remotedir, repoName, repoPrefix=null,
                       releaseVersion=null, packageRevision='1',
                       projectBaseDirPrefix='') {
  sshPublisher(
    publishers: [
      sshPublisherDesc(
        verbose: true,
        configName: remote,
        transfers: [
          sshTransfer(
            sourceFiles: '*.rpm',
            remoteDirectory: remotedir,
            execCommand: """\
rm -rf /srv/rpm-repo/${repoName}/packages/*.rpm && mkdir -p /srv/rpm-repo/${repoName}/packages/ && \
mv /srv/libelektra/packaging/incoming/${repoName}/*.rpm /srv/rpm-repo/${repoName}/packages/ && \
createrepo /srv/rpm-repo/${repoName}/ && \
rm -rf /srv/rpm-repo/${repoName}/repodata/repomd.xml.asc && \
gpg2 --detach-sign --armor -u A9A25CC1CC83E839 --pinentry-mode=loopback --batch --passphrase-file \
/home/jenkins/.aptly/secret /srv/rpm-repo/${repoName}/repodata/repomd.xml"""
          )
        ]
      )
    ],
    failOnError: true
  )
}
