package com.unipi.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ServerProperties {
    private static Properties prop = new Properties();
    private static HashMap<NAMES, Object> map = new HashMap<>(NAMES.values().length);

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
                            map.put(NAMES.valueOf(param), Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            System.err.println(value + " non è un numero");
                        }

                        continue;
                    }

                    map.put(NAMES.valueOf(param), value);
                } catch (IllegalArgumentException ignored) {

                }
            }

        } catch (IOException e) {
            System.err.println("Errore nella lettura del file \"prop.properties\"\n" +
                    "Verranno usati i valori di default");
        }
    }

    public static Map<NAMES, Object> getValues() {
        return Collections.unmodifiableMap(map);
    }

    public static Object getValue(NAMES prop) {
        return map.get(prop);
    }

    private static void setDefualtValues() {
        for (NAMES v : NAMES.values()) {
            switch (v) {
                case SERVER_ADDRESS, RMI_ADDRESS, DB_ADDRESS -> map.put(v, "localhost");
                case DEFUALT_TCP_PORT -> map.put(v, 45678);
                case DEFUALT_UDP_PORT -> map.put(v, 45679);
                case DB_PORT -> map.put(v, 45675);
                case MULTICAST_ADDRESS -> map.put(v, "239.255.32.31");
                case MULTICAST_PORT -> map.put(v, 45677);
                case RMI_REG_PORT -> map.put(v, 45676);
                case RMI_FOLLOW_PORT -> map.put(v, 45670);
                case SOCK_TIMEOUT, PRINT_LOG -> map.put(v, 0);
                case REWARD_TIME_DELAY -> map.put(v, "1d");
                case AUTHOR_PERCENTAGE -> map.put(v, 70);
                case CLOSE_DB -> map.put(v, "true");
            }
        }
    }

    private static boolean isNumber(String value) {
        switch (value) {
            case "SERVER_ADDRESS", "MULTICAST_ADDRESS", "RMI_ADDRESS", "REWARD_TIME_DELAY", "DB_ADDRESS", "CLOSE_DB" -> {
                return false;
            }

            default -> {
                return true;
            }
        }
    }

    public enum NAMES {
        SERVER_ADDRESS,
        RMI_ADDRESS,
        MULTICAST_ADDRESS,
        DB_ADDRESS,
        DB_PORT,
        DEFUALT_TCP_PORT,
        DEFUALT_UDP_PORT,
        MULTICAST_PORT,
        RMI_REG_PORT,   //porta per il servizio di registrazione
        RMI_FOLLOW_PORT,   //porta per il servizio dei followers
        SOCK_TIMEOUT,
        REWARD_TIME_DELAY,
        AUTHOR_PERCENTAGE,
        CLOSE_DB, PRINT_LOG
    }
}
