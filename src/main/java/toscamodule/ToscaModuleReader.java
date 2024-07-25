package toscamodule;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
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

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class ToscaModuleReader extends Application {
    private static final String TOSCA_API_URL = "http://172.40.0.31/Rest/ToscaCommander";
    private static final String MODULE_ID = "3a13adbd-09a7-cf42-f559-eabc87b8b8ec";
    private static String username = "Admin";
    private static String workspace = "NV_PROJECT";
    private static String password = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Tosca Module Reader");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10, 10, 10, 10));
        grid.setVgap(8);
        grid.setHgap(10);

        Label instanceUrlLabel = new Label("Instance URL:");
        GridPane.setConstraints(instanceUrlLabel, 0, 0);
        TextField instanceUrlInput = new TextField(TOSCA_API_URL);
        GridPane.setConstraints(instanceUrlInput, 1, 0);

        Label usernameLabel = new Label("Username:");
        GridPane.setConstraints(usernameLabel, 0, 1);
        TextField usernameInput = new TextField(username);
        GridPane.setConstraints(usernameInput, 1, 1);

        Label passwordLabel = new Label("Password:");
        GridPane.setConstraints(passwordLabel, 0, 2);
        PasswordField passwordInput = new PasswordField();
        GridPane.setConstraints(passwordInput, 1, 2);

        Label workspaceLabel = new Label("Workspace:");
        GridPane.setConstraints(workspaceLabel, 0, 3);
        TextField workspaceInput = new TextField(workspace);
        GridPane.setConstraints(workspaceInput, 1, 3);

        Button submitButton = new Button("Submit");
        GridPane.setConstraints(submitButton, 1, 4);

        grid.getChildren().addAll(instanceUrlLabel, instanceUrlInput, usernameLabel, usernameInput,
                passwordLabel, passwordInput, workspaceLabel, workspaceInput, submitButton);

        submitButton.setOnAction(e -> {
            String instanceUrl = instanceUrlInput.getText();
            username = usernameInput.getText();
            password = passwordInput.getText();
            workspace = workspaceInput.getText();

            try {
                String response = getModule(instanceUrl, MODULE_ID);
                showResponseWindow(response);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(grid, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static String getModule(String instanceUrl, String moduleId) throws IOException, ParserConfigurationException, SAXException {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build()) {
            HttpGet request = new HttpGet(instanceUrl + "/" + workspace + "/object/" + moduleId);
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

    private void showResponseWindow(String response) {
        Stage responseStage = new Stage();
        responseStage.setTitle("Module Response");

        TextArea textArea = new TextArea();
        textArea.set
