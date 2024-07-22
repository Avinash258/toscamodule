package toscamodule;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.XML;
import org.xml.sax.SAXException;

public class ToscaModuleReader {
    private static final String TOSCA_API_URL = "http://172.40.0.31/Rest/ToscaCommander";
    private static final String MODULE_ID = "3a13adbd-09a7-cf42-f559-eabc87b8b8ec";
    private static final String USERNAME = "Admin";
    private static final String WORKSPACE = "NV_PROJECT";
    private static final String PASSWORD = System.getenv("TOSCA_PASSWORD") != null ? System.getenv("TOSCA_PASSWORD") : "";

    public static void main(String[] args) {
        try {
            String response = getModule(MODULE_ID);
            if (response != null) {
                System.out.println(response);
            } else {
                System.err.println("Failed to retrieve response.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred while trying to fetch the module:");
            e.printStackTrace();
        }
    }

    private static String getModule(String moduleId) throws IOException, ParserConfigurationException, SAXException {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(USERNAME, PASSWORD));

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {
            HttpGet request = new HttpGet(TOSCA_API_URL + "/" + WORKSPACE + "/object/" + moduleId);
            System.out.println("Executing request: " + request.getRequestLine());

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    System.err.println("Failed to fetch the module. HTTP error code: " + statusCode);
                    return null;
                }

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    String contentType = entity.getContentType() != null ? entity.getContentType().getValue() : "";

                    if (contentType.contains("application/json")) {
                        // Strip BOM if present
                        if (!result.isEmpty() && result.charAt(0) == '\uFEFF') {
                            result = result.substring(1);
                        }
                        return formatJson(result);
                    } else if (contentType.contains("application/xml")) {
                        return convertXmlToJson(result);
                    } else {
                        System.err.println("Unexpected content type: " + contentType);
                        System.err.println("Response: " + result);
                    }
                } else {
                    System.err.println("Response entity is null.");
                }
            }
        }
        return null;
    }

    private static String formatJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return json.toString(4); // 4 is the number of spaces for indentation
        } catch (Exception e) {
            System.err.println("Error formatting JSON response: " + e.getMessage());
            return jsonString;
        }
    }

    private static String convertXmlToJson(String xmlString) throws ParserConfigurationException, SAXException, IOException {
        JSONObject xmlJSONObj = XML.toJSONObject(xmlString);
        return xmlJSONObj.toString(4); // 4 is the number of spaces for indentation
    }
}
