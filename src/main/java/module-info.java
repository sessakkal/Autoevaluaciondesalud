module com.example.autoevaluaciondesalud {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.autoevaluaciondesalud to javafx.fxml;
    exports com.example.autoevaluaciondesalud;
}