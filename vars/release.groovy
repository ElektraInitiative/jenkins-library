#!/usr/bin/env groovy
/**
 * release.groovy
 *
 * This file contains shared methods for package releases
 */

/** Publish all rpm packages and updates the repo
 *
 * Also deletes older versions from the repo.
 * @param remote where the repository is located
 * @param remotedir must match with the directory tree inside the remote
 * @param repoName name of the repository
 * @param repoPrefix prefix of the repository
 * @param packageRevision revision number of the package
 */
def publishDebPackages(remote, remotedir, repoName, repoPrefix,
                       releaseVersion, packageRevision='1') {
  sshPublisher(
    publishers: [
      sshPublisherDesc(
        verbose: true,
        configName: remote,
        transfers: [
          sshTransfer(
            sourceFiles: '*.d*eb',
            remoteDirectory: remotedir
          ),
          sshTransfer(
            sourceFiles: '*.tar.gz',
            remoteDirectory: remotedir
          )
        ]
      )
    ],
    failOnError: true
  )

  // This step is necessary since running the commands to update
  // the aptly repo directly in the `exeecCommand` results in
  // invalid interpolation behaviour of `$Version` which is
  // caused by Grovvys GString.
  // Escaping could not solve this issue.
  dir("${WORKSPACE}/scripts/release/") {
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

/** Publish all rpm packages and updates the repo
 *
 * Also deletes older versions from the repo.
 * @param remote where the repository is located
 * @param remotedir must match with the directory tree inside the remote
 * @param repoName name of the repository
 * @param packageRevision revision number of the package
 */
def publishRpmPackages(remote, remotedir, repoName, packageRevision='1') {
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
rm -rf /srv/rpm-repo/${repoName}/packages/*.rpm && \
mv /srv/libelektra/packaging/incoming/${repoName}/*.rpm /srv/rpm-repo/${repoName}/packages/ && \
createrepo /srv/rpm-repo/${repoName}/ && \
rm -rf /srv/rpm-repo/${repoName}/repodata/repomd.xml.asc && \
gpg2 --detach-sign --armor -u A9A25CC1CC83E839 --pinentry-mode=loopback --batch --passphrase-file \
/home/jenkins/.aptly/secret /srv/rpm-repo/${repoName}/repodata/repomd.xml"""
          ),
          sshTransfer(
            sourceFiles: '*.tar.gz',
            remoteDirectory: remotedir
          )
        ]
      )
    ],
    failOnError: true
  )
}
