module com.example.hfx {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires ij;

    requires javafx.swing;


    opens com.example.hfx to javafx.fxml;
    exports com.example.hfx;
}