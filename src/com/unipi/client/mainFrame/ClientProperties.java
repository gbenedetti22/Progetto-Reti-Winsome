package com.unipi.client.mainFrame;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ClientProperties {
    public static final int USERNAME_MAX_LENGHT = 10;
    public static final int PASSWORD_MAX_LENGHT = 8;
    public static final int POST_TITLE_MAX_LENGHT = 20;
    public static final int POST_CONTENT_MAX_LENGHT = 500;
    private static Properties prop = new Properties();
    private static HashMap<ClientProperties.NAMES, Object> map = new HashMap<>(ClientProperties.NAMES.values().length);
    private static String multicastAddress;
    private static int multicastPort;

    static {
        init();
    }

    private static void init() {
        try {
            FileReader reader = new FileReader("prop.properties");

            setDefualtValues();
            prop.load(reader);
            reader.close();

            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                try {
                    String param = String.valueOf(entry.getKey());
                    String value = String.valueOf(entry.getValue());

                    if (isNumber(param)) {
                        try {
                            map.put(ClientProperties.NAMES.valueOf(param), Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            System.err.println(value + " non è un numero");
                        }

                        continue;
                    }

                    map.put(ClientProperties.NAMES.valueOf(param), value);
                } catch (IllegalArgumentException ignored) {

                }
            }

        } catch (IOException e) {
            System.err.println("Errore nella lettura del file \"prop.properties\"\n" +
                    "Verranno usati i valori di default");
        }
    }

    public static HashMap<ClientProperties.NAMES, Object> getValues() {
        return map;
    }

    private static void setDefualtValues() {
        for (ClientProperties.NAMES v : ClientProperties.NAMES.values()) {
            switch (v) {
                case SERVER_ADDRESS, RMI_ADDRESS -> map.put(v, "localhost");
                case DEFUALT_TCP_PORT -> map.put(v, 45678);
                case DEFUALT_UDP_PORT -> map.put(v, 45679);
                case RMI_REG_PORT -> map.put(v, 45676);
                case RMI_FOLLOW_PORT -> map.put(v, 45670);
                case SOCK_TIMEOUT -> map.put(v, 0);
            }
        }
    }

    private static boolean isNumber(String value) {
        switch (value) {
            case "SERVER_ADDRESS", "RMI_ADDRESS", "REWARD_TIME_DELAY" -> {
                return false;
            }

            default -> {
                return true;
            }
        }
    }

    public static String getMulticastAddress() {
        return multicastAddress;
    }

    public static void setMulticastAddress(String multicastAddress) {
        ClientProperties.multicastAddress = multicastAddress;
    }

    public static int getMulticastPort() {
        return multicastPort;
    }

    public static void setMulticastPort(int multicastPort) {
        ClientProperties.multicastPort = multicastPort;
    }

    public enum NAMES {
        SERVER_ADDRESS,
        RMI_ADDRESS,
        DEFUALT_TCP_PORT,
        DEFUALT_UDP_PORT,
        RMI_REG_PORT,
        RMI_FOLLOW_PORT,
        SOCK_TIMEOUT,
    }
}
