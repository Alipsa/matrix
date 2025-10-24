package se.alipsa.matrix.sql.config

import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry;
class JaasConfigLoader {

  private static final String JAAS_CONFIG_NAME = "SQLJDBCDriver";

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
        System.out.println("No JAAS configuration found for " + JAAS_CONFIG_NAME + ". Setting default Kerberos config.")

        // 2. Create and set the custom Configuration
        Configuration.setConfiguration(new KerberosJaasConfiguration(currentConfig))
      } else {
        System.out.println("Existing JAAS configuration for " + JAAS_CONFIG_NAME + " found. Skipping default load.")
      }

    } catch (SecurityException e) {
      // This is the typical exception thrown when a security policy prevents reading the default config,
      // or when no configuration has been loaded at all. Treat this as "not configured" and set our default.
      System.err.println("Security exception retrieving JAAS config. Assuming none set and registering default.")
      Configuration.setConfiguration(new KerberosJaasConfiguration(null))
    } catch (Exception e) {
      // Handle other unexpected exceptions
      System.err.println("Unexpected error during JAAS check: " + e.getMessage())
    }
  }
}

// Custom Configuration class to provide the SQLJDBCDriver entry
class KerberosJaasConfiguration extends Configuration {
  private final Configuration delegate;

  // Delegate allows us to respect other configurations loaded previously
  public KerberosJaasConfiguration(Configuration delegate) {
    this.delegate = delegate;
  }

  @Override
  AppConfigurationEntry[] getAppConfigurationEntry(String name) {
    if (name.equals(JaasConfigLoader.JAAS_CONFIG_NAME)) {
      // This is the default Kerberos login module provided by the JDK
      HashMap<String, String> options = new HashMap<>()
      options.put("useTicketCache", "true")
      options.put("doNotPrompt", "true")

      return new AppConfigurationEntry[] {
          new AppConfigurationEntry(
              "com.sun.security.auth.module.Krb5LoginModule",
              AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
              options
          )
      }
    }

    // Delegate to the previously loaded configuration for all other names
    return (delegate != null) ? delegate.getAppConfigurationEntry(name) : null
  }
}
