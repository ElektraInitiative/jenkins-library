#!/usr/bin/env groovy

/**
 * For overwriting and sharing of PipelineConfig object.
 * Delegates closure body to be executed in context of PipelineConfig object.
 */
def call(Closure body) {
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = PipelineConfig.instance
    body()
}
