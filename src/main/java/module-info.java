module com.ai.demo {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.ai.demo to javafx.fxml;
    exports com.ai.demo;
}