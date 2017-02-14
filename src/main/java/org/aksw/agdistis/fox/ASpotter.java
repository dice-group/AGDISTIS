package org.aksw.agdistis.fox;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.List;

import org.aksw.agdistis.model.NamedEntity;

public abstract class ASpotter {
    public abstract List<NamedEntity> getEntities(String question);

    protected String requestPOST(String input, String requestURL) throws MalformedURLException, ProtocolException,
            IOException {
        String output = POST(input, requestURL);
        return output;
    }

    private String POST(String urlParameters, String requestURL) throws MalformedURLException, IOException,
            ProtocolException {
        URL url = new URL(requestURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        connection.setRequestProperty("Content-Length", String.valueOf(urlParameters.length()));
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        InputStream inputStream = connection.getInputStream();
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(in);
        StringBuilder sb = new StringBuilder();
        while (reader.ready()) {
            sb.append(reader.readLine());
        }
        wr.close();
        reader.close();
        connection.disconnect();
        return sb.toString();
    }

    @Override
    public String toString() {
        String[] name = getClass().getName().split("\\.");
        return name[name.length - 1].substring(0, 3);
    }
}
