package py.monitor.jmx.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySource({ "classpath:config/jmxagent.properties" })
public class JmxAgentConfiguration {
    @Value("${jmx.agent.port}")
    private int jmxAgentPort;

    @Value("${jmx.agent.switcher}")
    private String jmxAgentSwitcher;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public int getJmxAgentPort() {
        return jmxAgentPort;
    }

    public void setJmxAgentPort(int jmxAgentPort) {
        this.jmxAgentPort = jmxAgentPort;
    }

    public String getJmxAgentSwitcher() {
        return jmxAgentSwitcher;
    }

    public void setJmxAgentSwitcher(String jmxAgentSwitcher) {
        this.jmxAgentSwitcher = jmxAgentSwitcher;
    }
}
