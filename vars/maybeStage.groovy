#!/usr/bin/env groovy

/** Generate a Stage
 *
 * If `expression` evaluates to TRUE, a stage(`name`) with `body` is run
 * @param name Name of the stage
 * @param expression If True, run body
 * @param body Closure representing stage body
 */
def call(String name, boolean expression, Closure body) {
  if(expression) {
    stage(name, body)
  } else {
    stage(name) {
      echo "Stage skipped: ${name}"
    }
  }
}
