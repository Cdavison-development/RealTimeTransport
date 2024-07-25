module com.project.busfinder {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires org.json;

    opens com.project.busfinder to javafx.fxml;
    exports com.project.busfinder;
    exports com.project.busfinder.util;
    opens com.project.busfinder.util to javafx.fxml;
}