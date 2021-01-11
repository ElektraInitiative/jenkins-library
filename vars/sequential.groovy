#!/usr/bin/env groovy

/**
 * Run set of tasks sequentially. Similar to `parallel` directive.
 * @param stages List of stages that will be run sequentially
 */
def call(stages) {
  for (stage in stages) {
    stage.value.call()
  }
}
