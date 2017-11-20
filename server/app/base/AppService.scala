package base

/**
  * Base class for a service class.
  *
  * @param cc an injected instance of AppComponents
  */
abstract class AppService(private[base] val cc: AppComponents) extends AppComponents.Implicits
