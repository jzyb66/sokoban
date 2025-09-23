package com.ai.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load()); // 尺寸会根据GridPane自动调整

        HelloController controller = fxmlLoader.getController();

        // 为场景设置键盘按下事件监听器
        scene.setOnKeyPressed(event -> {
            controller.handleKeyPress(event.getCode());
        });

        stage.setTitle("推箱子 (Sokoban)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}