package com.ai.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        // 注意这里加载的是 BorderPane
        BorderPane root = fxmlLoader.load();
        HelloController controller = fxmlLoader.getController();

        Scene scene = new Scene(root);

        // *** 最终修复方案：将键盘事件监听器直接附加到根节点上 ***
        root.setOnKeyPressed(event -> {
            controller.handleKeyPress(event.getCode());
        });

        stage.setTitle("推箱子 (Sokoban)");
        stage.setScene(scene);
        stage.show();

        // *** 确保程序启动后，焦点立即在根节点上，以便接收按键 ***
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch();
    }
}