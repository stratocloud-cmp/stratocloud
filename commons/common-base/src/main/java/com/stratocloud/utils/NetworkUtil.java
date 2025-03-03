package com.stratocloud.utils;

import com.googlecode.ipv6.IPv6Network;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
public class NetworkUtil {

    private static final int MAX_CIDR = 32;

    public static boolean isValidIpv4(final String ip) {
        if (ip == null)
            return false;
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet4Address(ip);
    }

    public static boolean isValidIpv6(final String ip) {
        if (ip == null)
            return false;
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet6Address(ip);
    }

    public static boolean isValidIp(InternetProtocol protocol, String ip){
        switch (protocol){
            case IPv4 -> {return isValidIpv4(ip);}
            case IPv6 -> {return isValidIpv6(ip);}
            default -> {return false;}
        }
    }


    public static boolean isValidIp(final String ip) {
        if (ip == null)
            return false;
        final InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValid(ip);
    }

    public static long ip2Long(final String ip) {
        final String[] tokens = ip.split("[.]");
        assert tokens.length == 4;
        long result = 0;
        for (String token : tokens) {
            try {
                result = result << 8 | Integer.parseInt(token);
            } catch (final NumberFormatException e) {
                throw new RuntimeException("Incorrect number", e);
            }
        }

        return result;
    }

    public static String long2Ip(final long ip) {
        return (ip >> 24 & 0xff) + "." +
                (ip >> 16 & 0xff) + "." +
                (ip >> 8 & 0xff) + "." +
                (ip & 0xff);
    }

    public static InetAddress getLocalInetAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            throw new StratoException(e.getMessage(), e);
        }
    }

    private static long getIpv4NumericMask(final long prefixLength) {
        return ((long)0xffffffff) >> MAX_CIDR - prefixLength << MAX_CIDR - prefixLength;
    }

    public static String getIpv4Mask(final long prefixLength){
        return long2Ip(getIpv4NumericMask(prefixLength));
    }


    public static boolean isValidIp4Cidr(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }

        final String[] cidrPair = cidr.split("/");
        if (cidrPair.length != 2) {
            return false;
        }
        final String cidrAddress = cidrPair[0];
        final String cidrSize = cidrPair[1];
        if (!isValidIpv4(cidrAddress)) {
            return false;
        }
        int cidrSizeNum;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (final Exception e) {
            return false;
        }

        return cidrSizeNum >= 0 && cidrSizeNum <= MAX_CIDR;
    }

    public static boolean isValidIp6Cidr(final String ip6Cidr) {
        try {
            IPv6Network.fromString(ip6Cidr);
        } catch (final IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    public static boolean isValidCidr(InternetProtocol internetProtocol, String cidr){
        if(internetProtocol == InternetProtocol.IPv4){
            return isValidIp4Cidr(cidr);
        }else if(internetProtocol == InternetProtocol.IPv6) {
            return isValidIp6Cidr(cidr);
        }else {
            return isValidIp4Cidr(cidr);
        }
    }

    public static boolean isIpReachable(String address){
        try{
            String osName = System.getProperty("os.name").toUpperCase();


            String[] command;
            Charset defaultCharset;
            if(osName.contains("LINUX")){
                command = new String[]{"ping", "-c", "1", address};
                defaultCharset = Charset.defaultCharset();
            }else if(osName.contains("WINDOWS")) {
                command = new String[]{"ping", "-n", "1", address};
                defaultCharset = Charset.forName("GBK");
            }else {
                log.warn("Running on unknown os {}, cannot test ip.", osName);
                return false;
            }
            log.info("Testing ip with command {}...", List.of(command));
            Process process = Runtime.getRuntime().exec(command);
            log.info(IOUtils.toString(process.getInputStream(), defaultCharset));
            boolean reachable = (process.waitFor()==0);
            log.info("IP {} is {}.", address, reachable?"reachable":"unreachable");
            return reachable;
        }catch (Exception e){
            log.warn("Failed to ping ip {} so we do not know whether it's in use.", address, e);
            return false;
        }
    }

    public static boolean isValidCidr(String cidr) {
        return isValidCidr(InternetProtocol.IPv4, cidr) || isValidCidr(InternetProtocol.IPv6, cidr);
    }
}
