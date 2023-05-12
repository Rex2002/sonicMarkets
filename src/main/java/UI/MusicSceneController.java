package UI;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;

public class MusicSceneController implements Initializable {

    @FXML
    private TextField headerTitle;
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private LineChart lineChart;
    @FXML
    //Wahrscheinlich irgendwie zwei deminsionales Array oder so
    @Override
    public void initialize(URL location, ResourceBundle resources) {
       addData();
    }

    //Könten den ALLMIGHTY den Kursnamenn eben und dann wirft er ein 2D Array raus(Möglichkeit)
    public void addData(){
        //int x = 0;
        //int y = 0;
        //XYChart.Series series = new XYChart.Series();
        //while (Array groß nocnicht leer){
        //
        //while(Array nochnicht leer){
        //series.getData().add(new XYChart.Data(Array[1], Array[1][x]));
        //x+1
        //}
        //y+1
        //x = 0;
        //}
    }
    public void switchToMainScene(ActionEvent event) throws IOException {
        root = FXMLLoader.load(getClass().getResource("MainScene.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

}
