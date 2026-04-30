package se.alipsa.matrix.sql.config

import se.alipsa.matrix.core.util.Logger

import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.Configuration

/**
 * Loads a default JAAS (Java Authentication and Authorization Service) Kerberos
 * configuration for the SQL JDBC driver if one is not already present.
 *
 * <p>This ensures that Kerberos-based authentication works out of the box when
 * no explicit JAAS configuration has been provided by the environment.</p>
 */
class JaasConfigLoader {

  private static final Logger log = Logger.getLogger(JaasConfigLoader)
  static final String JAAS_CONFIG_NAME = 'SQLJDBCDriver'

  static void loadDefaultKerberosConfigIfNeeded() {
    try {
      // 1. Attempt to retrieve the configuration entry for the SQL Server driver.
      // If the system has a configuration loaded, it will return the entries (or null/throw error for a missing named entry).
      // If no configuration is loaded at all, it usually throws a specific exception (or returns null depending on JDK).
      Configuration currentConfig = Configuration.getConfiguration()

      AppConfigurationEntry[] entries = currentConfig.getAppConfigurationEntry(JAAS_CONFIG_NAME)

      if (entries == null || entries.length == 0) {
        // If entries are null or empty, it means the named configuration is missing
        // *or* no configuration is loaded. Set our default.
        log.info("No JAAS configuration found for $JAAS_CONFIG_NAME. Setting default Kerberos config.")

        // 2. Create and set the custom Configuration
        Configuration.setConfiguration(new KerberosJaasConfiguration(currentConfig))
      } else {
        log.info("Existing JAAS configuration for $JAAS_CONFIG_NAME found. Skipping default load.")
      }
    } catch (SecurityException e) {
      // This is the typical exception thrown when a security policy prevents reading the default config,
      // or when no configuration has been loaded at all. Treat this as "not configured" and set our default.
      log.error('Security exception retrieving JAAS config. Assuming none set and registering default.', e)
      Configuration.setConfiguration(new KerberosJaasConfiguration(null))
    } catch (IllegalStateException e) {
      // Intentional fail-fast: for any other unexpected state exceptions we log and rethrow
      // to avoid continuing application startup with a potentially broken JAAS/Kerberos configuration.
      // This may prevent the application from starting if JAAS initialization fails.
      log.error("Unexpected error during JAAS check: ${e.message}", e)
      throw e
    }
  }

}

/**
 * Custom {@link Configuration} that provides a default Kerberos login module
 * entry for the SQL JDBC driver ({@code SQLJDBCDriver}).
 *
 * <p>Delegates to a previously loaded configuration for all other entry names,
 * so existing JAAS settings are preserved.</p>
 */
class KerberosJaasConfiguration extends Configuration {

  private static final String BOOLEAN_TRUE = 'true'

  private final Configuration delegate

  /** Delegate allows us to respect other configurations loaded previously. */
  KerberosJaasConfiguration(Configuration delegate) {
    this.delegate = delegate
  }

  @Override
  AppConfigurationEntry[] getAppConfigurationEntry(String name) {
    if (name == JaasConfigLoader.JAAS_CONFIG_NAME) {
      // This is the default Kerberos login module provided by the JDK
      Map<String, String> options = [:]
      options.put('useTicketCache', BOOLEAN_TRUE)
      options.put('doNotPrompt', BOOLEAN_TRUE)

      return new AppConfigurationEntry[] {
          new AppConfigurationEntry(
              'com.sun.security.auth.module.Krb5LoginModule',
              AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
              options
          )
      }
    }

    // Delegate to the previously loaded configuration for all other names
    (delegate != null) ? delegate.getAppConfigurationEntry(name) : new AppConfigurationEntry[0]
  }

}
