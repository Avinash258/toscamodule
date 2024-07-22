package toscamodule;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser.ExtensionFilter;

import java.io.File;
import java.io.FileWriter;
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
    private Label oldFileLabel;
    private Label newFileLabel;
    private TextArea resultArea;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("HTML Comparator");

        // Label to show old HTML file name
        oldFileLabel = new Label("No file selected");

        // Button to upload old HTML file
        Button oldFileButton = new Button("Upload Old HTML");
        oldFileButton.setOnAction(e -> {
            oldFile = getFileFromDialog(primaryStage, "Select Old HTML File");
            if (oldFile != null) {
                oldFileLabel.setText("Selected: " + oldFile.getName());
            }
        });

        // Label to show new HTML file name
        newFileLabel = new Label("No file selected");

        // Button to upload new HTML file
        Button newFileButton = new Button("Upload New HTML");
        newFileButton.setOnAction(e -> {
            newFile = getFileFromDialog(primaryStage, "Select New HTML File");
            if (newFile != null) {
                newFileLabel.setText("Selected: " + newFile.getName());
            }
        });

        // Compare button
        Button compareButton = new Button("Compare");
        compareButton.setOnAction(e -> {
            if (oldFile != null && newFile != null) {
                compareAndDisplayHTMLFiles(oldFile, newFile, primaryStage);
            } else {
                resultArea.setText("Please upload both old and new HTML files.");
            }
        });

        // TextArea to display the comparison result
        resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);

        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(oldFileButton, oldFileLabel, newFileButton, newFileLabel, compareButton, resultArea);

        Scene scene = new Scene(vbox, 500, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private File getFileFromDialog(Stage primaryStage, String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.getExtensionFilters().add(new ExtensionFilter("HTML Files", "*.html", "*.htm"));
        return fileChooser.showOpenDialog(primaryStage);
    }

    private void compareAndDisplayHTMLFiles(File oldFile, File newFile, Stage primaryStage) {
        try {
            // Parse the HTML files into Jsoup Documents
            Document oldDoc = Jsoup.parse(oldFile, "UTF-8");
            Document newDoc = Jsoup.parse(newFile, "UTF-8");

            // JSON object to hold attribute changes
            JSONObject changes = new JSONObject();

            // Find all elements in old and new documents
            Elements oldElements = oldDoc.getAllElements();
            Elements newElements = newDoc.getAllElements();

            // StringBuilder to hold result text
            StringBuilder resultText = new StringBuilder();

            // Flag to check if there are any differences
            boolean hasDifferences = false;

            // Compare attributes
            for (Element oldElement : oldElements) {
                String cssQuery = getCssQuery(oldElement);
                Elements newElementMatches = newElements.select(cssQuery);

                if (!newElementMatches.isEmpty()) {
                    Element newElement = newElementMatches.first();
                    // Get attributes of the old element
                    Attributes oldAttributes = oldElement.attributes();

                    // Iterate over attributes
                    for (Attribute attr : oldAttributes) {
                        String attrKey = attr.getKey();
                        String oldAttrValue = attr.getValue();
                        String newAttrValue = newElement.attr(attrKey);
                        if (!oldAttrValue.equals(newAttrValue)) {
                            hasDifferences = true;
                            // Create a JSON object for the attribute change
                            JSONObject change = new JSONObject();
                            change.put("tag", oldElement.tagName());
                            change.put("cssQuery", cssQuery);
                            change.put("attribute", attrKey);
                            change.put("oldValue", oldAttrValue);
                            change.put("newValue", newAttrValue);

                            // Add this change to the main changes object
                            changes.append("attributeChanges", change);

                            // Append change to result text
                            resultText.append("Tag: ").append(oldElement.tagName())
                                      .append("\nCSS Query: ").append(cssQuery)
                                      .append("\nAttribute: ").append(attrKey)
                                      .append("\nOld Value: ").append(oldAttrValue)
                                      .append("\nNew Value: ").append(newAttrValue)
                                      .append("\n\n");
                        }
                    }
                }
            }

            if (hasDifferences) {
                // Display the result text in the TextArea
                resultArea.setText(resultText.toString());

                // Prompt the user to save the results
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Comparison Result");
                fileChooser.getExtensionFilters().add(new ExtensionFilter("Text Files", "*.txt"));
                File saveFile = fileChooser.showSaveDialog(primaryStage);
                if (saveFile != null) {
                    saveComparisonResult(saveFile, changes.toString(2)); // Pretty print with 2 spaces indent
                }
            } else {
                resultArea.setText("No differences found between the old and new HTML files.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveComparisonResult(File file, String content) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            System.out.println("Comparison result saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCssQuery(Element element) {
        StringBuilder cssQuery = new StringBuilder(element.tagName());
        if (element.id() != null && !element.id().isEmpty()) {
            cssQuery.append("#").append(element.id());
        }
        for (String className : element.classNames()) {
            cssQuery.append(".").append(className);
        }
        for (Attribute attribute : element.attributes()) {
            if (!attribute.getKey().equals("id") && !attribute.getKey().equals("class")) {
                cssQuery.append("[").append(attribute.getKey()).append("=\"").append(attribute.getValue()).append("\"]");
            }
        }
        return cssQuery.toString();
    }
}
