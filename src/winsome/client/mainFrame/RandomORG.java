package winsome.client.mainFrame;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RandomORG {
    private URL url;

    public RandomORG() {
        try {
            String URL = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";
            url = new URL(URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getRandomNumber() {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String number = reader.readLine();
                reader.close();
                connection.disconnect();
                return Integer.parseInt(number);
            }

            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }
}
