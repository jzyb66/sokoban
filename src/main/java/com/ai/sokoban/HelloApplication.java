package com.ai.sokoban;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 游戏主程序入口类。
 * 【职责】: 继承自JavaFX的Application类，负责启动应用、加载主视图(FXML)并进行初始化设置。
 */
public class HelloApplication extends Application {

    /**
     * JavaFX应用的启动方法，是程序的主入口点。
     * @param stage 主舞台对象，由JavaFX平台自动创建和传入。
     * @throws IOException 当加载hello-view.fxml文件失败时抛出。
     */
    @Override
    public void start(Stage stage) throws IOException {
        // 创建FXMLLoader实例，用于加载FXML布局文件
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));

        // 加载FXML定义的根节点 (StackPane)
        StackPane root = fxmlLoader.load();
        // 获取与FXML关联的控制器实例
        HelloController controller = fxmlLoader.getController();

        // 创建场景(Scene)，并将根节点放入场景中
        Scene scene = new Scene(root);

        // **关键步骤**: 将键盘事件监听器直接设置在根节点上。
        // 这是最安全的位置，因为此时Scene和Root都已完全初始化。
        // 当用户按下键盘时，事件会传递给控制器的 handleKeyPress 方法进行处理。
        root.setOnKeyPressed(event -> {
            controller.handleKeyPress(event.getCode());
        });

        // 将根节点的引用传递给控制器，以便UIManager可以控制键盘事件的启用/禁用
        controller.setRootPaneForUIManager(root);

        // 设置窗口的标题
        stage.setTitle("推箱子 (Sokoban)");
        // 将场景设置到舞台上
        stage.setScene(scene);

        // 让窗口大小根据场景内容自动调整，确保界面元素完整显示
        stage.sizeToScene();
        // 禁止用户手动调整窗口大小，保持游戏界面的固定布局
        stage.setResizable(false);

        // 显示窗口
        stage.show();

        // 程序启动后，立即让根节点获得焦点，这样才能立刻响应键盘输入事件
        root.requestFocus();
    }

    /**
     * Java程序的main方法。
     * @param args 命令行参数（本游戏中未使用）。
     */
    public static void main(String[] args) {
        // 调用launch()方法来启动JavaFX应用
        launch();
    }
}