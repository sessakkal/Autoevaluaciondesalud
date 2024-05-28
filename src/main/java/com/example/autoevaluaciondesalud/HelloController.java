package com.example.autoevaluaciondesalud;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.chart.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class HelloController {
    @FXML
    private Label etiquetaFecha;
    @FXML
    private TextField campoEstadoDeAnimo;
    @FXML
    private TextField campoEstadoFisico;
    @FXML
    private VBox vboxAlimentos;
    @FXML
    private TextArea areaEstadisticas;
    @FXML
    private VBox vboxGraficosAlimentos;
    @FXML
    private VBox vboxGraficosEstadoAnimo;
    @FXML
    private VBox vboxGraficosEstadoFisico;

    private Map<String, String[]> datos = new HashMap<>();
    private static final String DIRECTORIO_DATOS = "data";
    private static final String ARCHIVO_DATOS = DIRECTORIO_DATOS + "/data.txt";
    private final String[] alimentos = {"Leche", "Cereales", "Pasta", "Fruta", "Vegetales", "Legumbres", "Agua", "Alcohol", "Zumo"};

    @FXML
    public void initialize() {
        LocalDate hoy = LocalDate.now();
        etiquetaFecha.setText(hoy.toString());

        for (String item : alimentos) {
            CheckBox checkBox = new CheckBox(item);
            vboxAlimentos.getChildren().add(checkBox);
        }
    }

    @FXML
    private void agregarDatos() {
        try {
            String fecha = LocalDate.now().toString();
            double estadoDeAnimo = Double.parseDouble(campoEstadoDeAnimo.getText());
            double estadoFisico = Double.parseDouble(campoEstadoFisico.getText());

            if (estadoDeAnimo < 0 || estadoDeAnimo > 5 || estadoFisico < 0 || estadoFisico > 5) {
                Alert alerta = new Alert(Alert.AlertType.ERROR, "El estado de ánimo y el estado físico deben estar entre 0 y 5.", ButtonType.OK);
                alerta.showAndWait();
                return;
            }

            StringBuilder alimentosConsumidos = new StringBuilder();
            for (var node : vboxAlimentos.getChildren()) {
                if (node instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) node;
                    if (checkBox.isSelected()) {
                        if (alimentosConsumidos.length() > 0) {
                            alimentosConsumidos.append(";");
                        }
                        alimentosConsumidos.append(checkBox.getText());
                    }
                }
            }

            datos.put(fecha, new String[]{String.valueOf(estadoDeAnimo), String.valueOf(estadoFisico), alimentosConsumidos.toString()});
            guardarDatosEnArchivo();

            Alert alertaExito = new Alert(Alert.AlertType.INFORMATION, "Datos introducidos con éxito.", ButtonType.OK);
            alertaExito.showAndWait();
        } catch (NumberFormatException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR, "El estado de ánimo y el estado físico deben ser números enteros.", ButtonType.OK);
            alerta.showAndWait();
        } catch (IOException e) {
            Alert alerta = new Alert(Alert.AlertType.ERROR, "Error al guardar los datos en el archivo.", ButtonType.OK);
            alerta.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void about() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText(null);
        alert.setContentText("JavaFX desarrollado por Biel López y Sara Essakkal.");
        alert.showAndWait();
    }



    @FXML
    private void salir() {
        Platform.exit();
    }

    @FXML
    private void mostrarEstadisticas() {
        try {
            List<String> lineas = cargarDatosDelArchivo();
            if (lineas.isEmpty()) {
                areaEstadisticas.setText("No hay datos para mostrar.");
                return;
            }

            int totalEstadoDeAnimo = 0;
            int totalEstadoFisico = 0;
            int cantidad = lineas.size();


            Map<String, Integer> estadoDeAnimoPorDia = new TreeMap<>();
            Map<String, Integer> estadoFisicoPorDia = new TreeMap<>();
            Map<String, Integer> conteoAlimentos = new HashMap<>();

            for (String linea : lineas) {
                String[] partes = linea.split(",");
                if (partes.length < 4) {
                    continue;
                }
                String fecha = partes[0];
                double estadoDeAnimo = Double.parseDouble(partes[1]);
                double estadoFisico = Double.parseDouble(partes[2]);
                String[] alimentos = partes[3].split(";");

                totalEstadoDeAnimo += estadoDeAnimo;
                totalEstadoFisico += estadoFisico;
                estadoDeAnimoPorDia.put(fecha, (int) estadoDeAnimo);
                estadoFisicoPorDia.put(fecha, (int) estadoFisico);

                for (String alimento : alimentos) {
                    conteoAlimentos.put(alimento, conteoAlimentos.getOrDefault(alimento, 0) + 1);
                }
            }


            double promedioEstadoDeAnimo = (double) totalEstadoDeAnimo / cantidad;
            double promedioEstadoFisico = (double) totalEstadoFisico / cantidad;
            String alimentoMasConsumido = Collections.max(conteoAlimentos.entrySet(), Map.Entry.comparingByValue()).getKey();
            String mejorDiaEstadoDeAnimo = Collections.max(estadoDeAnimoPorDia.entrySet(), Map.Entry.comparingByValue()).getKey();
            String mejorDiaEstadoFisico = Collections.max(estadoFisicoPorDia.entrySet(), Map.Entry.comparingByValue()).getKey();

            areaEstadisticas.setText(String.format(
                    "Estadísticas:\n\nCantidad de días introducidos: %d\nPromedio de estado de ánimo: %.2f\nPromedio de estado físico: %.2f\nAlimento o bebida más consumido: %s\nDía con mejor estado de ánimo: %s\nDía con mejor estado físico: %s",
                    cantidad, promedioEstadoDeAnimo, promedioEstadoFisico, alimentoMasConsumido, mejorDiaEstadoDeAnimo, mejorDiaEstadoFisico
            ));

            mostrarGraficos(estadoDeAnimoPorDia, estadoFisicoPorDia, conteoAlimentos);
        } catch (FileNotFoundException e) {
            areaEstadisticas.setText("No hay datos para mostrar. El archivo de datos no existe.");
        } catch (IOException e) {
            areaEstadisticas.setText("Error al leer los datos del archivo.");
            e.printStackTrace();
        }
    }

    private void mostrarGraficos(Map<String, Integer> estadoDeAnimoPorDia, Map<String, Integer> estadoFisicoPorDia, Map<String, Integer> conteoAlimentos) {
        vboxGraficosAlimentos.getChildren().clear();
        vboxGraficosEstadoAnimo.getChildren().clear();
        vboxGraficosEstadoFisico.getChildren().clear();

        PieChart chartAlimentos = new PieChart();
        chartAlimentos.setTitle("Alimentos Consumidos");
        for (Map.Entry<String, Integer> entry : conteoAlimentos.entrySet()) {
            chartAlimentos.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        vboxGraficosAlimentos.getChildren().add(chartAlimentos);

        CategoryAxis xAxisAnimo = new CategoryAxis();
        NumberAxis yAxisAnimo = new NumberAxis();
        xAxisAnimo.setLabel("Fecha");
        yAxisAnimo.setLabel("Estado de Ánimo");

        LineChart<String, Number> chartEstadoAnimo = new LineChart<>(xAxisAnimo, yAxisAnimo);
        chartEstadoAnimo.setTitle("Estado de Ánimo por Día");

        XYChart.Series<String, Number> seriesEstadoAnimo = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : estadoDeAnimoPorDia.entrySet()) {
            seriesEstadoAnimo.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartEstadoAnimo.getData().add(seriesEstadoAnimo);
        vboxGraficosEstadoAnimo.getChildren().add(chartEstadoAnimo);

        CategoryAxis xAxisFisico = new CategoryAxis();
        NumberAxis yAxisFisico = new NumberAxis();
        xAxisFisico.setLabel("Fecha");
        yAxisFisico.setLabel("Estado Físico");

        LineChart<String, Number> chartEstadoFisico = new LineChart<>(xAxisFisico, yAxisFisico);
        chartEstadoFisico.setTitle("Estado Físico por Día");

        XYChart.Series<String, Number> seriesEstadoFisico = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : estadoFisicoPorDia.entrySet()) {
            seriesEstadoFisico.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartEstadoFisico.getData().add(seriesEstadoFisico);
        vboxGraficosEstadoFisico.getChildren().add(chartEstadoFisico);
    }

    private List<String> cargarDatosDelArchivo() throws IOException {
        File archivo = new File(ARCHIVO_DATOS);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }
        return Files.readAllLines(Paths.get(ARCHIVO_DATOS));
    }

    private void guardarDatosEnArchivo() throws IOException {
        File directorio = new File(DIRECTORIO_DATOS);
        if (!directorio.exists()) {
            directorio.mkdir();
        }

        List<String> lineas = cargarDatosDelArchivo();

        String fechaActual = LocalDate.now().toString();
        boolean fechaEncontrada = false;
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).startsWith(fechaActual)) {
                String nuevaEntrada = String.format("%s,%s,%s,%s", fechaActual, datos.get(fechaActual)[0], datos.get(fechaActual)[1], datos.get(fechaActual)[2]);
                lineas.set(i, nuevaEntrada);
                fechaEncontrada = true;
                break;
            }
        }

        if (!fechaEncontrada) {
            lineas.add(String.format("%s,%s,%s,%s", fechaActual, datos.get(fechaActual)[0], datos.get(fechaActual)[1], datos.get(fechaActual)[2]));
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_DATOS))) {
            for (String linea : lineas) {
                writer.write(linea);
                writer.newLine();
            }
        }
    }


}
