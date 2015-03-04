package spatutorial.client

package object logger {
  private val defaultLogger = LoggerFactory.getLogger("Log")

  def log = defaultLogger
}
