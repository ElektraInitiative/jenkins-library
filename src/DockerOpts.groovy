// Define DockerOpts enum which is used in withDockerEnv
enum DockerOpts {
  MOUNT_MIRROR,       // Mount the git repository mirror into the container
  PTRACE              // Allow ptraces in the container
  public DockerOpts()  {} // WORKAROUND see TEST
}
