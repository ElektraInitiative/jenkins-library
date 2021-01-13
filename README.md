# Jenkins shared library

This is the jenkins shared library used by Elektra.

## Project Setup

To add this library to Jenkins, go to `Manage Jenkins » Configure System » Global Pipeline Libraries` and configure it as follows:
```ini
Name: libelektra-shared # this name is used in the Jenkinsfiles
Default version: master # set the default version to the master branch
Load implicitly: false # we do not want the library to be loaded implicitly, only with @Library
Allow default version to be overridden: true # necessary so that branches or forks can test their version
Include @Library changes in job recent changes: false # if this would be set to true, all open PR's would be restartet
```

## Usage
If you want to use this library in a Jenkinsfile, import the Library with the `@Library` annotion.
Following imports the default version (master branch) of this library:

```groovy
@Library('libelektra-shared') _
```

The configuration of the pipeline parameters and constants is done with the step named `pipelineConfig`.
Default values exist for `dockerNodeLabel` and `registry`. The current date `now` must be set explicitly.

```groovy
pipelineConfig {
// uncomment if you you want to overwrite these values
// dockerNodeLabel = 'docker'
// registry = 'hub.libelektra.org'
  now = new Date()
}
```

### Available functions

Functions that contain a closure as parameter and the `sequential` directive are available as custom steps. They can be called directly with their names. E.g. `withDockerEnv {...}`.
All other function are split up into files and can be called like follows: `<filename>.<functionname>` E.g: `dockerUtils.buildImage(image)`.

The `src/` directory contains the `DockerOpts` enum and a singleton object `PipelineConfig` which is used by the `pipelineConfig` step to configure the pipeline parameter and constants.


## Contributing

The easiest way of modifying and testing this shared library with our Jenkinsfiles is to either create a branch or fork and a Pull Request.
To use your branch of this shared library in your Jenkinsfile, simply append the branch name to the `@Library` annotation.
E.g. if you have a branch with the name "develop" then the annotation must have following form: `@Library('libelektra-shared@develop') _`. 
This also applies to PR's where instead of the branch name the PR number must be appended in following form: `PR-#`.


