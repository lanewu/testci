package py.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Configuration for service control flow network.
 * 
 * @author zjm
 *
 */
@Configuration
@PropertySource("classpath:config/network.properties")
public class NetworkConfiguration {
    @Value("${enable.data.depart.from.control:false}")
    private boolean enableDataDepartFromControl = false;

    @Value("${control.flow.subnet:10.0.1.0/24}")
    private String controlFlowSubnet = "10.0.1.0/24";

    @Value("${data.flow.subnet:10.0.1.0/24}")
    private String dataFlowSubnet = "10.0.1.0/24";

    @Value("${monitor.flow.subnet:10.0.1.0/24}")
    private String monitorFlowSubnet = "10.0.1.0/24";

    @Value("${outward.flow.subnet:10.0.1.0/24}")
    private String outwardFlowSubnet = "10.0.1.0/24";

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    public String getControlFlowSubnet() {
        return controlFlowSubnet;
    }

    public void setControlFlowSubnet(String controlFlowSubnet) {
        this.controlFlowSubnet = controlFlowSubnet;
    }

    public boolean isEnableDataDepartFromControl() {
        return enableDataDepartFromControl;
    }

    public void setEnableDataDepartFromControl(boolean enableDataDepartFromControl) {
        this.enableDataDepartFromControl = enableDataDepartFromControl;
    }

    public String getDataFlowSubnet() {
        return dataFlowSubnet;
    }

    public void setDataFlowSubnet(String dataFlowSubnet) {
        this.dataFlowSubnet = dataFlowSubnet;
    }

    public String getMonitorFlowSubnet() {
        return monitorFlowSubnet;
    }

    public void setMonitorFlowSubnet(String monitorFlowSubnet) {
        this.monitorFlowSubnet = monitorFlowSubnet;
    }

    public String getOutwardFlowSubnet() {
        return outwardFlowSubnet;
    }

    public void setOutwardFlowSubnet(String outwardFlowSubnet) {
        this.outwardFlowSubnet = outwardFlowSubnet;
    }
}
