/**
 * Singleton object that stores pipeline configuration
 */
@Singleton
class PipelineConfig {

  String dockerNodeLabel = 'docker'
  String registry = 'hub.libelektra.org'
  Date now

}
