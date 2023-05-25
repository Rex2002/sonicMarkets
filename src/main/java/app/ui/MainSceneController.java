package app.ui;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import app.communication.EventQueues;
import app.communication.Msg;
import app.communication.MsgToUIType;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import dataRepo.Sonifiable;

public class MainSceneController implements Initializable {
    private CheckEQService service;

    // WARNING: Kommentare werden noch normalisiert
    @FXML
    private Label inst1EcLabel;
    @FXML
    private Label inst1VoLabel;
    @FXML
    private Label inst1PiLabel;
    @FXML
    private Label inst1HiLabel;
    @FXML
    private Button startBtn;
    @FXML
    private VBox paneBox;
    @FXML
    private VBox instBox;
    @FXML
    private Label headerTitle;
    @FXML
    private Label instPitchShare;
    @FXML
    private ChoiceBox<String> categorieChoice;
    @FXML
    private ChoiceBox<String> locationChoice;
    @FXML
    private ChoiceBox<String> ecChoice1;
    @FXML
    private ChoiceBox<String> ecChoice2;
    @FXML
    private ChoiceBox<String> hiChoice1;
    @FXML
    private ChoiceBox<String> hiChoice2;
    @FXML
    private ChoiceBox<String> priceChoice;
    @FXML
    private ChoiceBox<String> trendLineBreaksChoice;
    @FXML
    private ChoiceBox<String> derivateChoice;
    @FXML
    private Pane sharePane;
    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private VBox checkVBox;
    @FXML
    private ArrayList<String> shareCheckName = new ArrayList<>();
    // @FXML
    // private HashMap<String, String[]> shareSettingList = new HashMap<String,
    // String[]>(); Wharscheinlich Löschen
    @FXML
    private String[][] setArray = new String[10][4];
    @FXML
    private int countArray = 0;
    @FXML
    private Stage stage;
    private Scene scene;
    private Parent root;
    @FXML
    private String[] categories = { "Option 1", "Option 2" };
    @FXML
    private String[] locations = { "Option 1", "Option 2" };
    @FXML
    private String[] prices = { "Option 1", "Option 2" };
    @FXML
    private String[] trends = { "Option 1", "Option 2" };
    @FXML
    private String[] derivate = { "Option 1", "Option 2" };

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) { // Initialisierung mit den Optionen
        categorieChoice.getItems().addAll(categories);
        locationChoice.getItems().addAll(locations);
        // priceChoice.getItems().addAll(prices);
        // trendLineBreaksChoice.getItems().addAll(trends);
        // derivateChoice.getItems().addAll(derivate);

        inst1VoLabel.setText("null");
        enableBtn();
        service = new CheckEQService(this);
        service.setPeriod(Duration.millis(100));
        service.start();

    }

    @FXML
    public void switchToMusicScene(ActionEvent event) throws IOException { // Wechsel auf die Music Scene
        root = FXMLLoader.load(getClass().getResource("MusicScene.fxml"));
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void clearCheckList() {
        checkVBox.getChildren().removeAll(); // Die Checkliste Liste clearen, NOCH TESTEN
    }

    public void addToCheckList(String name) {
        CheckBox cBox = new CheckBox(name); // API Text Holen Was bei neu laden einfach weiter hinten abfragen
                                            // könnten die Size als indicator nehmen
        if (shareCheckName.contains(cBox.getText())) {
            cBox.setSelected(true);
        }
        cBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue) {
                    if (paneBox.getChildren().size() < 10) {
                        addToPaneBox(cBox.getText());
                        shareCheckName.add(cBox.getText());
                    } else {
                        cBox.setSelected(false);
                    }

                } else {
                    for (int i = 0; i < 10; i++) {
                        if (setArray[i][0] == cBox.getText()) {
                            setArray[i][0] = null;
                            setArray[i][1] = null;
                            setArray[i][2] = null;
                            enableBtn();
                            setArray[i][3] = null;
                        }
                    }
                    int i = shareCheckName.indexOf(cBox.getText());
                    shareCheckName.remove(cBox.getText());
                    paneBox.getChildren().remove(i);
                    paneBox.prefHeight(paneBox.getChildren().size() * 477.0);
                }
            }
        });
        checkVBox.setPrefHeight((checkVBox.getChildren().size()) * 74.0);
        checkVBox.getChildren().add(cBox);
        if (checkVBox.getChildren().size() == 10) {
            Button loadBtn = new Button("Nächste laden");
            loadBtn.setOnAction(event -> {
                loadNew();
            });
            loadBtn.setId("loadBtn");
            checkVBox.setPrefHeight((checkVBox.getChildren().size()) * 74.0);
            checkVBox.getChildren().add(loadBtn);
        }
    }

    private void loadNew() {
        // Reload sonifiables
    }

    @FXML
    public void addToPaneBox(String txt) { // add a Sharepanel to the Panel Box if there are less than 10 Sharepanel
        paneBox.getChildren().add(createSharePane(txt));
        paneBox.setPrefHeight((paneBox.getChildren().size()) * 800.0);
    }

    private Pane createSharePane(String name) { // initialize and dek the Share Pane
        for (int x = 0; x < 10; x++) {
            if (setArray[x][0] == null) {
                setArray[x][0] = name;
                break;
            }
        }
        Pane examplePane = new Pane();
        examplePane.setId("expPane");
        TextField tField = new TextField();
        tField.setText(name);
        tField.setId("txtField");
        examplePane.getChildren().add(tField);
        Label pLabel = new Label();
        pLabel.setId("paneShareLabel");
        pLabel.setText("Price");
        pLabel.setLayoutX(26);
        pLabel.setLayoutY(61.6);
        Label tLBLabel = new Label();
        tLBLabel.setId("paneShareLabel");
        tLBLabel.setText("Trend Line Break");
        tLBLabel.setLayoutX(16);
        tLBLabel.setLayoutY(224.8);
        examplePane.getChildren().add(tLBLabel);
        Label dLabel = new Label();
        dLabel.setId("paneShareLabel");
        dLabel.setText("Derivate");
        dLabel.setLayoutX(16);
        dLabel.setLayoutY(388);
        ChoiceBox pinstChoiceBox = new ChoiceBox<>();
        ChoiceBox pChoiceBox = new ChoiceBox<>();
        pChoiceBox.getItems().addAll("Inst1", "Inst2");
        pChoiceBox.setLayoutX(16);
        pChoiceBox.setLayoutY(106);
        pChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                while (setArray[countArray][0] != name) {
                    countArray++;
                }

                pinstChoiceBox.disableProperty().set(false);
                setArray[countArray][1] = prices[(int) number2];
                countArray = 0;
                enableBtn();
            }
        });

        pinstChoiceBox.getItems().addAll(prices);
        pinstChoiceBox.setLayoutX(16);
        pinstChoiceBox.disableProperty().set(true);
        pinstChoiceBox.setLayoutY(166);
        pinstChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                if (pChoiceBox.getValue().toString() == "Inst1") {
                    switch ((int) number) {
                        case 0:
                            inst1VoLabel.setText("Kein Kurs");
                            break;
                        case 1:
                            inst1EcLabel.setText("Kein Kurs");
                            break;
                    }
                    switch ((int) number2) {
                        case 0:
                            inst1VoLabel.setText(tField.getText());
                            break;
                        case 1:
                            inst1EcLabel.setText(tField.getText());
                            break;
                    }
                }
            }
        });
        ChoiceBox tLBChoiceBox = new ChoiceBox<>();
        tLBChoiceBox.getItems().addAll(trends);
        tLBChoiceBox.setLayoutX(16);
        tLBChoiceBox.setLayoutY(270.2);
        tLBChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                while (setArray[countArray][0] != name) {
                    countArray++;
                }
                setArray[countArray][2] = prices[(int) number2];
                countArray = 0;
                enableBtn();
            }
        });
        ChoiceBox tLBinstChoiceBox = new ChoiceBox<>();
        tLBinstChoiceBox.getItems().addAll(trends);
        tLBinstChoiceBox.setLayoutX(16);
        tLBinstChoiceBox.setLayoutY(330.2);
        tLBinstChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                while (setArray[countArray][0] != name) {
                    countArray++;
                }
                setArray[countArray][2] = prices[(int) number2];
                countArray = 0;
                enableBtn();
            }
        });
        ChoiceBox dChoiceBox = new ChoiceBox<>();
        dChoiceBox.getItems().addAll(derivate);
        dChoiceBox.setLayoutX(16);
        dChoiceBox.setLayoutY(433.4);
        dChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                while (setArray[countArray][0] != name) {
                    countArray++;
                }
                setArray[countArray][3] = prices[(int) number2]; // Klappt nicht so wie es soll
                countArray = 0;
                enableBtn();
            }
        });
        ChoiceBox dinstChoiceBox = new ChoiceBox<>();
        dinstChoiceBox.getItems().addAll(derivate);
        dinstChoiceBox.setLayoutX(16);
        dinstChoiceBox.setLayoutY(493.4);
        dinstChoiceBox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
                while (setArray[countArray][0] != name) {
                    countArray++;
                }
                setArray[countArray][3] = prices[(int) number2]; // Klappt nicht so wie es soll
                countArray = 0;
                enableBtn();
            }
        });
        examplePane.getChildren().add(pChoiceBox);
        examplePane.getChildren().add(pinstChoiceBox);
        examplePane.getChildren().add(tLBChoiceBox);
        examplePane.getChildren().add(tLBinstChoiceBox);
        examplePane.getChildren().add(dChoiceBox);
        examplePane.getChildren().add(dinstChoiceBox);
        examplePane.getChildren().add(dLabel);
        examplePane.getChildren().add(pLabel);
        return examplePane;
    }

    LocalDate minDateStart = LocalDate.of(2023, 4, 16);
    LocalDate maxDateStart = LocalDate.now();

    private void updateStartPicker() { // Datum Blockers WARNING: Müsen schauen dass wir die angegebenen Daten bei
                                       // änderung der Aktien überprüfen
        startPicker.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(item.isAfter(maxDateStart) || item.isBefore(minDateStart));
            }
        });
    }

    LocalDate minDateEnd = LocalDate.of(2023, 4, 16);
    LocalDate maxDateEnd = LocalDate.now();

    private void updateEndPicker() { // Datum Blockers
        endPicker.setDayCellFactory(d -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(item.isAfter(maxDateEnd) || item.isBefore(minDateEnd));
            }
        });
    }

    public void enableBtn() {
        int[] checkArray = new int[10];
        startBtn.setDisable(false);
        for (int x = 0; x < 10; x++) {
            if (setArray[x][0] != null) {
                checkArray[countArray] = x;
                countArray++;
            }
        }
        System.out.println("countArray" + countArray);
        if (countArray == 0) {
            startBtn.setDisable(true);
        } else {
            for (int p = 0; p < countArray; p++) {
                System.out.println(countArray);
                for (int c = 1; c < 4; c++) {
                    if (setArray[p][c] == null) {
                        startBtn.setDisable(true);
                        break;
                    }
                }
            }
        }
        if (endPicker.getValue() == null || startPicker.getValue() == null) {
            startBtn.setDisable(true);
        }
        countArray = 0;
    }

    private static class CheckEQService extends ScheduledService<Object> {
        MainSceneController controller;

        CheckEQService(MainSceneController controller) {
            this.controller = controller;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Task<Object> createTask() {
            return new Task<>() {
                @Override
                protected Object call() throws Exception {
                    while (!EventQueues.toUI.isEmpty()) {
                        Msg<MsgToUIType> msg = EventQueues.toUI.poll();
                        if (msg.type == MsgToUIType.FILTERED_SONIFIABLES) {
                            List<Sonifiable> sonifiables = (List<Sonifiable>) msg.data;
                            for (Sonifiable s : sonifiables) {
                                controller.addToCheckList(s.getName());
                            }
                        } else {
                            // @Cleanup This hard exit is only for development, to not forget adding any
                            // MsgTypes here
                            System.out.println("MsgType '" + msg.type.toString() + "' is not yet implemented");
                            System.exit(1);
                        }
                    }
                    return null;
                }
            };
        }
    }
}
