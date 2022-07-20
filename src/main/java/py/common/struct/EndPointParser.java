package py.common.struct;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.common.CIDRUtils;
import py.common.OSCMDExecutor;
import py.common.OSCMDExecutor.OSCMDStreamConsumer;

/**
 * 
 * @author zjm
 *
 */
public class EndPointParser {
    private static final Logger logger = LoggerFactory.getLogger(EndPointParser.class);

    public static class UnknownIPV4HostException extends UnknownHostException {
        public UnknownIPV4HostException() {

        }

        public UnknownIPV4HostException(String host) {
            super(host);
        }
    }

    public static class UnknownIPV6HostException extends UnknownHostException {
        public UnknownIPV6HostException() {
        }

        public UnknownIPV6HostException(String host) {
            super(host);
        }
    }

    static class InetAddressComparator implements Comparator<InetAddress> {
        @Override
        public int compare(InetAddress o1, InetAddress o2) {
            return o1.getHostAddress().compareTo(o2.getHostAddress());
        }
    }

    static class IfaceComparator implements Comparator<NetworkInterface> {
        @Override
        public int compare(NetworkInterface o1, NetworkInterface o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static final int MIN_PORT = 1;
    public static final int MAX_PORT = 65535;
    public static final int IPV4_MAX_MASK_LEN = 32;
    public static final String IPV4_PATTERN = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
    private static final Pattern addressPattern = Pattern.compile(IPV4_PATTERN);
    
//    public static final Set<String> SECONDARY_IPS;
    public static final Set<String> MAIN_IPS;
    
    static {
//        SECONDARY_IPS = secondaryIPs();
        MAIN_IPS = mainIPs();
    }

    /**
     * 
     * @param port
     * @param localIp
     * @return
     */
    public static EndPoint parseLocalEndPoint(int port, String localIp) {
        validatePort(port);
        return new EndPoint(localIp, port);
    }

    /**
     * 
     * @param endpoint
     * @param localIp
     * @return
     */
    public static EndPoint parseLocalEndPoint(String endpoint, String localIp) {
        Validate.notNull(endpoint, "Endpoint to be parsed cannot be null");
        Validate.notEmpty(endpoint, "Endpoint to be parsed cannot be empty");

        String[] parts = endpoint.split(":");
        Validate.isTrue(0 < parts.length && parts.length < 3, "Format of endpoint must be 'port' or 'hostname:port'");

        logger.debug("Parse endpoint {}", endpoint);
        if (parts.length == 1) {
            int port = Integer.parseInt(parts[0]);

            return parseLocalEndPoint(port, localIp);
        } else {
            String hostName = parts[0];
            int port = Integer.parseInt(parts[1]);

            if (hostName.isEmpty()) {
                return parseLocalEndPoint(port, localIp);
            } else {
                validatePort(port);
                return new EndPoint(hostName, port);
            }
        }
    }

    /**
     * Parse instance of {@link EndPoint} from a given string and sub-net.
     * <p>
     * Almost services of py belong to sub-net of control flow. But there is such service 'datanode' who is enable to
     * separate data flow from control flow. That means data flow belongs to different sub-net of control flow. This
     * function parse end-point belongs to sub-net of the given "subnet". Data flow host address could parse by this
     * function.
     * 
     * @param endpoint
     * @param subnet
     * @return
     */
    public static EndPoint parseInSubnet(String endpoint, String subnet) {
        Validate.notNull(endpoint, "Endpoint to be parsed cannot be null");
        Validate.notEmpty(endpoint, "Endpoint to be parsed cannot be empty");

        String[] parts = endpoint.split(":");
        Validate.isTrue(0 < parts.length && parts.length < 3, "Format of endpoint must be 'port' or 'hostname:port'");

        logger.debug("Parse endpoint {}", endpoint);
        if (parts.length == 1) {
            int port = Integer.parseInt(parts[0]);

            return parseInSubnet(port, subnet);
        } else {
            int port = Integer.parseInt(parts[1]);
            validatePort(port);

            try {
                String hostName = (parts[0].isEmpty()) ? getLocalHostLANAddress(subnet).getHostAddress() : parts[0];
                return new EndPoint(hostName, port);
            } catch (UnknownHostException e) {
                logger.error("Caught an exception", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Parse instance of {@link EndPoint} from a given port and sub-net.
     * <p>
     * Almost services of py belong to sub-net of control flow. But there is such service 'datanode' who is enable to
     * separate data flow from control flow. That means data flow belongs to different sub-net of control flow. This
     * function parse end-point belongs to sub-net of the given "subnet". Data flow host address could parse by this
     * function.
     * 
     * @param port
     * @param subnet
     * @return
     */
    public static EndPoint parseInSubnet(int port, String subnet) {
        validatePort(port);

        try {
            logger.debug("Current port: {}, subnet:{}", port, subnet);
            String hostName = getLocalHostLANAddress(subnet).getHostAddress();

            return new EndPoint(hostName, port);
        } catch (UnknownHostException e) {
            logger.error("Caught an exception", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get network interface info to which an IPV4 address belongs to the given subnet binding.
     * 
     * @param subnet
     * @return network interface info to which an IPV4 address belongs to the given subnet binding.
     * @throws UnknownHostException
     *             if no network interface being found
     */
    public static NetworkInterface getLocalHostLANInterface(String subnet) throws UnknownHostException {
        NetworkInterface netInterface;
        Enumeration<NetworkInterface> interfaceEnumeration;
        // use tree set to sort network interfaces to prevent disagree result among many-times invocation.
        TreeSet<NetworkInterface> ifaces = new TreeSet<>(new IfaceComparator());

        try {
            interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            while (interfaceEnumeration.hasMoreElements()) {
                InetAddress address;
                Enumeration<InetAddress> addrEnumeration;

                netInterface = interfaceEnumeration.nextElement();
                addrEnumeration = netInterface.getInetAddresses();
                while (addrEnumeration.hasMoreElements()) {
                    address = addrEnumeration.nextElement();
                    try {
                        if (isInSubnet(address.getHostAddress(), subnet)
                                && MAIN_IPS.contains(address.getHostAddress())) {
                            logger.info("Parsed ip {} binding interface {} in subnet {}", address.getHostAddress(),
                                    netInterface.getName(), subnet);
                            ifaces.add(netInterface);
                        } else {
                            logger.info("Address {} is not in subnet {}", address.getHostAddress(), subnet);
                        }
                    } catch (Exception e) {
                        logger.info("Unable to check if address {} is in subnet {}, maybe it is IPV6",
                                address.getHostAddress(), subnet);
                        continue;
                    }
                }
            }

            if (ifaces.size() > 0) {
                logger.info("Select one network iface from parsed ifaces: {}", ifaces.first());
                return ifaces.first();
            }

            logger.error(
                    "Unable to find proper IP address after goning through all ifaces, try to use java default binding ip address");
            throw new UnknownHostException();
        } catch (Exception e) {
            logger.error("Caught an exception when parse network interface in subnet {}", subnet, e);
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN interface: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    /**
     * Get one of IPV4 addresses belongs to the given network binding to the given network interface.
     * 
     * @param subnet
     *            cidr-notation of network
     * @param networkIface
     *            network interface
     * @return one of IPV4 addresses binding to the given network interface.
     * @throws UnknownIPV4HostException
     *             if no address found from the given network interface
     */
    public static InetAddress getIPV4Addr(String subnet, NetworkInterface networkIface) throws UnknownIPV4HostException {
        InetAddress address;
        Enumeration<InetAddress> addrEnumeration;
        // use tree set to sort network interfaces to prevent disagree result among many-times invocation.
        TreeSet<InetAddress> addresses = new TreeSet<>(new InetAddressComparator());

        networkIface.getSubInterfaces();
        addrEnumeration = networkIface.getInetAddresses();
        while (addrEnumeration.hasMoreElements()) {
            address = addrEnumeration.nextElement();
            try {
                if (isInSubnet(address.getHostAddress(), subnet)
                        && MAIN_IPS.contains(address.getHostAddress())) {
                    logger.info("address : {}", address.toString());
                    addresses.add(address);
                }
            } catch (Exception e) {
                logger.info("Unable to check if address {} is in subnet {}, maybe it is IPV6", address.getHostAddress(),
                        subnet);
                continue;
            }
        }

        if (addresses.size() > 0) {
            logger.info("Select one address from parsed addresses: {}", addresses.first());
            return addresses.first();
        }

        logger.error("Unable to find proper IP address after goning through all addresses of network interface {}",
                networkIface.getName());
        throw new UnknownIPV4HostException();
    }

    /**
     * Get one of IPV6 addresses binding to the given network interface.
     * 
     * @param subnet
     *            cidr-notation of ipv6 network
     * @param networkIface
     *            network interface
     * @return one of IPV6 addresses binding to the given network interface.
     * @throws UnknownIPV6HostException
     *             if no address found from the given network interface
     */
    public static InetAddress getIPV6Addr(String subnet, NetworkInterface networkIface) throws UnknownIPV6HostException {
        CIDRUtils cidrUtils = null;
        InetAddress address;
        Enumeration<InetAddress> addrEnumeration;
        // use tree set to sort network interfaces to prevent disagree result among many-times invocation.
        TreeSet<InetAddress> addresses = new TreeSet<>(new InetAddressComparator());

        if (subnet != null) {
            try {
                cidrUtils = new CIDRUtils(subnet);
            } catch (UnknownHostException e) {
                throw new UnknownIPV6HostException();
            }
        }

        addrEnumeration = networkIface.getInetAddresses();
        while (addrEnumeration.hasMoreElements()) {
            address = addrEnumeration.nextElement();
            if (address instanceof Inet6Address) {
                try {
                    if (subnet == null || cidrUtils.isInRange(address.getHostAddress())) {
                        logger.warn("Parsed an IPV6 address {} in subnet {}", address.getHostAddress(), subnet);
                        addresses.add(address);
                    }
                } catch (UnknownHostException e) {
                    throw new UnknownIPV6HostException();
                }
            }
        }

        if (addresses.size() > 0) {
            logger.warn("Select one address from parsed addresses: {}", addresses.first());
            return addresses.first();
        }

        logger.error("Unable to find proper IP address after goning through all addresses of network interface {}",
                networkIface.getName());
        throw new UnknownIPV6HostException();
    }

    /**
     * Go through all NICs of local host and select one NIC whose binding address belongs to the specified sub-net. If
     * there is no such address, return default jdk supplied address as local host address.
     * 
     * @param subnet
     * @return
     * @throws UnknownHostException
     */
    public static InetAddress getLocalHostLANAddress(String subnet) throws UnknownHostException {
        NetworkInterface networkIface;

        networkIface = getLocalHostLANInterface(subnet);
        return getIPV4Addr(subnet, networkIface);
    }

    /**
     * Check if a IP given in string is in specified subnet in a CIDR-notation string e.g. "192.168.0.1/16".
     * 
     * @param ipInStr
     * @param subnetInStr
     * @return
     * @throws UnknownHostException
     */
    public static boolean isInSubnet(String ipInStr, String subnetInStr) throws UnknownHostException {
        ipInStr = ipInStr.trim();

        validateIP(ipInStr);

        int subnet = getSubnet(subnetInStr);
        int maskLen = getMaskLen(subnetInStr);
        int ip = getIp(ipInStr);

        // signed integer left shift 32 bits is negative, but we want zero
        int mask = (maskLen == 0) ? 0 : 0xFFFFFFFF << (32 - maskLen);

        return ((subnet & mask) == (ip & mask));
    }

    /**
     * Get network address from a CIDR-notation string e.g. "192.168.0.1/16".
     * 
     * @param subnetInStr
     * @return
     * @throws UnknownHostException
     */
    public static int getSubnet(String subnetInStr) throws UnknownHostException {
        subnetInStr = subnetInStr.trim();

        String ipInStr = subnetInStr.split("/")[0];

        return getIp(ipInStr);
    }

    /**
     * Get subnet mask len from a CIDR-notation string e.g. "192.168.0.1/16".
     * 
     * @param subnetInStr
     * @return
     * @throws UnknownHostException
     */
    public static int getMaskLen(String subnetInStr) throws UnknownHostException {
        subnetInStr = subnetInStr.trim();

        int maskLen = Integer.valueOf(subnetInStr.split("/")[1]);
        if (maskLen < 0 || maskLen > IPV4_MAX_MASK_LEN) {
            throw new UnknownHostException("Invalid subnet " + subnetInStr);
        }

        return maskLen;
    }

    /**
     * Get IP integer value from its string.
     * 
     * @param ipInStr
     * @return
     * @throws UnknownHostException
     */
    public static int getIp(String ipInStr) throws UnknownHostException {
        ipInStr = ipInStr.trim();

        validateIP(ipInStr);

        InetAddress inetAddr = InetAddress.getByName(ipInStr);
        ByteBuffer inetAddrBuffer = ByteBuffer.wrap(inetAddr.getAddress());
        inetAddrBuffer.clear();

        // return ((inetAddrBytes[0] & 0xFF) << 24) | ((inetAddrBytes[1] & 0xFF) << 16) | ((inetAddrBytes[2] & 0xFF) <<
        // 8)
        // | ((inetAddrBytes[3] & 0xFF) << 0);
        return inetAddrBuffer.getInt();
    }

    /**
     * Get IP string from given integer number.
     * 
     * @param ip
     * @return
     * @throws UnknownHostException
     */
    public static String getIpInStr(int ip) throws UnknownHostException {
        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        buf.putInt(ip);
        buf.clear();

        StringBuffer sb = new StringBuffer();
        for (byte ipByte : buf.array()) {
            // transfer negative byte to positive
            sb.append(ipByte & 0x00FF);
            sb.append('.');
        }

        return sb.substring(0, sb.length() - 1);
    }

    private static void validatePort(int port) {
        Validate.isTrue(MIN_PORT <= port && port <= MAX_PORT,
                "Port " + port + " should be between " + MIN_PORT + " and " + MAX_PORT);
    }

    private static void validateIP(String ipInStr) throws UnknownHostException {
        Matcher matcher = addressPattern.matcher(ipInStr);
        if (!matcher.matches()) {
            throw new UnknownHostException("Unknow IP " + ipInStr);
        }
    }
    
    public static class SecondaryIPsParser implements OSCMDStreamConsumer {
        private final Logger logger = LoggerFactory.getLogger(SecondaryIPsParser.class);
        private static final String secondaryIPRegex = "inet[6]*\\s+([^\\s]+)/\\d+.*\\s+secondary\\s+";

        private Set<String> secondaryIPs = new HashSet<>();

        @Override
        public void consume(InputStream stream) throws IOException {
            String line;
            BufferedReader br;
            Pattern secondaryIPPat = Pattern.compile(secondaryIPRegex);

            br = new BufferedReader(new InputStreamReader(stream));
            while ((line = br.readLine()) != null) {
                Matcher matcher = secondaryIPPat.matcher(line);

                if (matcher.find()) {
                    secondaryIPs.add(matcher.group(1));
                }
            }
        }

        public Set<String> getSecondaryIPs() {
            for (String ip: secondaryIPs) {
                logger.warn("secondary ips : {}", ip);
            }
            return secondaryIPs;
        }

        public void setSecondaryIPs(Set<String> secondaryIPs) {
            this.secondaryIPs = secondaryIPs;
        }
    }
    
    public static Set<String> secondaryIPs() {
        final String cmdIPAll = "ip a";

        SecondaryIPsParser secondaryIPsParser;
        OSCMDExecutor.OSCMDOutputLogger errorOutput;

        secondaryIPsParser = new SecondaryIPsParser();

        errorOutput = new OSCMDExecutor.OSCMDOutputLogger(cmdIPAll);
        errorOutput.setErrorStream(true);

        try {
            OSCMDExecutor.exec(cmdIPAll, secondaryIPsParser, errorOutput);
        } catch (Exception e) {
            logger.error("Unable to parse secondary IPs", e);
        }

        return secondaryIPsParser.getSecondaryIPs();
    }

    public static class MainIPsParser implements OSCMDStreamConsumer {
        private final Logger logger = LoggerFactory.getLogger(MainIPsParser.class);
        private static final String nicRegex = "^([^\\s^:]+):?\\s+.*";
        private static final String mainIPRegex = "inet6?\\s+(addr:\\s*)?([^\\s]+)\\s+.*";

        private Set<String> mainIPs = new HashSet<>();

        @Override
        public void consume(InputStream stream) throws IOException {
            String line;
            String nicName = null;
            BufferedReader br;
            Pattern mainIPPat = Pattern.compile(mainIPRegex);
            Pattern nicPat = Pattern.compile(nicRegex);


            br = new BufferedReader(new InputStreamReader(stream));
            while ((line = br.readLine()) != null) {
                Matcher matcher;

                if (nicName == null) { // parse nic name
                    matcher = nicPat.matcher(line);
                } else {
                    matcher = mainIPPat.matcher(line);
                }

                if (nicName == null && matcher.find()) {
                    nicName = matcher.group(1);
                    nicName = (nicName.contains(":")) ? null: nicName;
                }

                if (nicName != null && matcher.find()) {
                    mainIPs.add(matcher.group(matcher.groupCount()));
                    nicName = null;
                }
            }
        }

        public Set<String> getMainIPs() {
            for (String ip: mainIPs) {
                logger.warn("main ips : {}", ip);
            }
            return mainIPs;
        }

        public void setMainIPs(Set<String> mainIPs) {
            this.mainIPs = mainIPs;
        }
    }

    public static Set<String> mainIPs() {
        final String cmdIfconfig = "ifconfig";
        MainIPsParser mainIPsParser;
        OSCMDExecutor.OSCMDOutputLogger errorOutput;

        mainIPsParser = new MainIPsParser();

        errorOutput = new OSCMDExecutor.OSCMDOutputLogger(cmdIfconfig);
        errorOutput.setErrorStream(true);

        try {
            OSCMDExecutor.exec(cmdIfconfig, mainIPsParser, errorOutput);
        } catch (Exception e) {
            logger.error("Unable to parse main IPs", e);
        }

        return mainIPsParser.getMainIPs();
    }
}
