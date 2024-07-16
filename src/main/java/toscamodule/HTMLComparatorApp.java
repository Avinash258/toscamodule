package toscamodule;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.json.JSONObject;

public class HTMLComparatorApp extends Application {
    
    private File oldFile;
    private File newFile;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HTML Comparator");

        // Button to upload old HTML file
        Button oldFileButton = new Button("Upload Old HTML");
        oldFileButton.setOnAction(e -> {
            oldFile = getFileFromDialog(primaryStage);
        });

        // Button to upload new HTML file
        Button newFileButton = new Button("Upload New HTML");
        newFileButton.setOnAction(e -> {
            newFile = getFileFromDialog(primaryStage);
        });

        // Compare button
        Button compareButton = new Button("Compare");
        compareButton.setOnAction(e -> {
            if (oldFile != null && newFile != null) {
                compareHTMLFiles(oldFile, newFile);
            } else {
                System.out.println("Please upload both old and new HTML files.");
            }
        });

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(oldFileButton, newFileButton, compareButton);

        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private File getFileFromDialog(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select HTML File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        return selectedFile;
    }

    private void compareHTMLFiles(File oldFile, File newFile) {
        try {
            // Parse the HTML files into Jsoup Documents
            Document oldDoc = Jsoup.parse(oldFile, "UTF-8");
            Document newDoc = Jsoup.parse(newFile, "UTF-8");

            // JSON object to hold attribute changes
            JSONObject changes = new JSONObject();

            // Find all elements in old and new documents
            Elements oldElements = oldDoc.getAllElements();
            Elements newElements = newDoc.getAllElements();

            // Compare attributes
            for (Element oldElement : oldElements) {
                String tagName = oldElement.tagName();
                Element newElement = newElements.select(tagName).first();
                if (newElement != null) {
                    // Get attributes of the old element
                    Attributes oldAttributes = oldElement.attributes();

                    // Iterate over attributes
                    for (Attribute attr : oldAttributes) {
                        String attrKey = attr.getKey();
                        String oldAttrValue = attr.getValue();
                        String newAttrValue = newElement.attr(attrKey);
                        if (!oldAttrValue.equals(newAttrValue)) {
                            // Create a JSON object for the attribute change
                            JSONObject change = new JSONObject();
                            change.put("tag", tagName);
                            change.put("attribute", attrKey);
                            change.put("oldValue", oldAttrValue);
                            change.put("newValue", newAttrValue);

                            // Add this change to the main changes object
                            changes.append("attributeChanges", change);
                        }
                    }
                }
            }

            // Print the JSON object with attribute changes
            System.out.println(changes.toString(2)); // Pretty print with 2 spaces indent

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
