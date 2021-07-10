/* Define DockerOpts enum which is used in withDockerEnv */
enum DockerOpts {

  MOUNT_MIRROR,            // Mount the git repository mirror into the container
  PTRACE,                  // Allow ptraces in the container
  NO_CHECKOUT,             // Do not checkout from SCM
  NO_CLEANUP               // Do not cleanup the Jenkins workspace after the build
  public DockerOpts()  { } // WORKAROUND see TEST

}
