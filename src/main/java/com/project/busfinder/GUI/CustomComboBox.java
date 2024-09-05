package com.project.busfinder.GUI;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

import java.util.stream.Collectors;


/**
 *
 * not currently working, fix later if can find the time
 *
 *
 * @param <T>
 */
public class CustomComboBox<T> extends ComboBox<T> {

    private ObservableList<T> originalItems = FXCollections.observableArrayList();

    public CustomComboBox() {
        super();
        this.setEditable(true);

        TextField editor = this.getEditor();
        editor.setOnKeyReleased(this::filterItems);

        this.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!this.isShowing()) {
                this.show();
            }
        });
    }

    public void setOriginalItems(ObservableList<T> items) {
        this.originalItems.setAll(items);
        this.setItems(originalItems);
    }

    private void filterItems(KeyEvent event) {
        TextField editor = this.getEditor();
        String filter = editor.getText();

        if (filter == null || filter.isEmpty()) {
            this.setItems(originalItems);
        } else {
            ObservableList<T> filteredItems = originalItems.stream()
                    .filter(item -> item.toString().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            this.setItems(filteredItems);
        }

        if (!this.isShowing()) {
            this.show();
        }
    }
}
