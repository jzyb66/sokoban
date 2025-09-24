module com.ai.sokoban {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.ai.sokoban to javafx.fxml;
    exports com.ai.sokoban;
}