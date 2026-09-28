package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

import client.ClientUI;
import logic.Request;
import navigation.Navigation;

import javafx.application.Platform;

/**
 * JavaFX controller for the reports and data analysis screen.
 * <p>
 * This controller is responsible for displaying statistical information related
 * to reservations, arrival delays, and waiting times using charts. It requests
 * the data from the server during initialization and updates the charts once
 * the data is received.
 * </p>
 * <p>
 * Two charts are displayed:
 * <ul>
 * <li>A line chart (rendered as points) showing arrival delays and waiting
 * times per order.</li>
 * <li>A bar chart showing weekly reservations and waiting list counts.</li>
 * </ul>
 *
 */
public class ReportsAnalysisController {

	/**
	 * Static reference to the currently active controller instance. Used by the
	 * networking layer to deliver server responses to this screen.
	 */
	public static ReportsAnalysisController activeController;

	/** Line chart displaying arrival delays and waiting times per order. */
	@FXML
	private LineChart<String, Number> arrivalChart;

	/** Bar chart displaying weekly reservations and waiting list statistics. */
	@FXML
	private BarChart<String, Number> reservationChart;

	/**
	 * Navigation buttons for returning to the home screen or exiting the
	 * application.
	 */
	@FXML
	Button backBtn, exitBtn;

	/**
	 * Regular expression pattern used to extract numeric week values from labels.
	 */
	private static final Pattern WEEK_NUM = Pattern.compile("\\d+");

	/**
	 * Initializes the controller after the FXML file has been loaded.
	 * <p>
	 * Stores a reference to this instance, configures the appearance of the arrival
	 * chart (points without connecting lines), and sends a request to the server to
	 * retrieve the reports data.
	 * </p>
	 */
	@FXML
	public void initialize() {
		activeController = this;

		arrivalChart.setCreateSymbols(true);
		arrivalChart.setAnimated(false);

		// Set a clearer Y-axis range for delay/waiting-time reports
		NumberAxis yAxis = (NumberAxis) arrivalChart.getYAxis();
		yAxis.setAutoRanging(false);
		yAxis.setLowerBound(0);
		yAxis.setUpperBound(20);
		yAxis.setTickUnit(2);

		ClientUI.chat.sendToServer(new Request("GET_REPORTS_DATA", null));
	}

	/**
	 * Extracts a numeric week number from a given label string.
	 *
	 * @param label a string containing a week identifier
	 * @return the extracted week number, or {@link Integer#MAX_VALUE} if parsing
	 *         fails
	 */
	private int extractWeek(String label) {
		var m = WEEK_NUM.matcher(label);
		return m.find() ? Integer.parseInt(m.group()) : Integer.MAX_VALUE;
	}

	/**
	 * Safely parses a string into an integer.
	 *
	 * @param s the string to parse
	 * @return the parsed integer value, or {@link Integer#MAX_VALUE} if parsing
	 *         fails
	 */
	private int safeParseInt(String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Updates both charts using the statistical data received from the server.
	 * <p>
	 * Expected data structure:
	 * <ul>
	 * <li>{@code "arrivalDelays"} – map of order number to arrival delay
	 * (minutes)</li>
	 * <li>{@code "waitingTimes"} – map of order number to waiting time
	 * (minutes)</li>
	 * <li>{@code "reservations"} – map of week label to number of reservations</li>
	 * <li>{@code "waiting"} – map of week label to waiting list count</li>
	 * </ul>
	 *
	 *
	 * @param data a nested map containing all report categories and their values
	 */
	public void setReportsData(Map<String, Map<String, Integer>> data) {

		arrivalChart.getData().clear();
		reservationChart.getData().clear();

		XYChart.Series<String, Number> arrivalDelaySeries = new XYChart.Series<>();
		arrivalDelaySeries.setName("Arrival Delay (min)");

		XYChart.Series<String, Number> waitingTimeSeries = new XYChart.Series<>();
		waitingTimeSeries.setName("Waiting Time (min)");

		if (data.get("arrivalDelays") != null) {
			data.get("arrivalDelays").entrySet().stream().sorted(Comparator.comparingInt(e -> safeParseInt(e.getKey())))
					.forEach(e -> arrivalDelaySeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
		}

		if (data.get("waitingTimes") != null) {
			data.get("waitingTimes").entrySet().stream().sorted(Comparator.comparingInt(e -> safeParseInt(e.getKey())))
					.forEach(e -> waitingTimeSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
		}

		arrivalChart.getData().addAll(arrivalDelaySeries, waitingTimeSeries);

		waitingTimeSeries.getData().forEach(point -> {
			if (point.getNode() != null) {
				point.getNode().setStyle("-fx-background-color: #3B82F6, white;" + "-fx-background-insets: 0, 2;"
						+ "-fx-background-radius: 8px;" + "-fx-padding: 6px;");
			}
		});

		// Remove connecting lines and keep symbols only
		arrivalChart.applyCss();
		arrivalChart.layout();

		for (XYChart.Series<String, Number> s : arrivalChart.getData()) {
			if (s.getNode() != null) {
				s.getNode().setStyle("-fx-stroke: transparent;");
			}
		}

		XYChart.Series<String, Number> reservationsSeries = new XYChart.Series<>();
		reservationsSeries.setName("Reservations");

		if (data.get("reservations") != null) {
			data.get("reservations").entrySet().stream().sorted(Comparator.comparingInt(e -> extractWeek(e.getKey())))
					.forEach(e -> reservationsSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
		}

		XYChart.Series<String, Number> waitingSeries = new XYChart.Series<>();
		waitingSeries.setName("Waiting List");

		if (data.get("waiting") != null) {
			data.get("waiting").entrySet().stream().sorted(Comparator.comparingInt(e -> extractWeek(e.getKey())))
					.forEach(e -> waitingSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
		}

		reservationChart.getData().addAll(reservationsSeries, waitingSeries);

		Platform.runLater(() -> {

			// Waiting List bars -> blue
			waitingSeries.getData().forEach(point -> {
				if (point.getNode() != null) {
					point.getNode().setStyle("-fx-bar-fill: #3B82F6;");
				}
			});

			// Waiting Time legend -> blue
			arrivalChart.lookupAll(".chart-legend-item-symbol.default-color1")
					.forEach(node -> node.setStyle("-fx-background-color: white, #3B82F6;"
							+ "-fx-background-insets: 0, 2;" + "-fx-background-radius: 8px;"));

			// Waiting List legend -> blue
			reservationChart.lookupAll(".chart-legend-item-symbol.default-color1")
					.forEach(node -> node.setStyle("-fx-background-color: #3B82F6;"));
		});

	}

	/**
	 * Exits the application immediately.
	 */
	@FXML
	private void exit() {
		System.exit(0);
	}

	/**
	 * Navigates back to the appropriate home screen according to the current user
	 * role.
	 *
	 * @param event the UI action event triggered by clicking the Back button
	 */
	@FXML
	private void back(ActionEvent event) {
		try {
			String target = Navigation.getHomeByRole();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/" + target));

			Scene scene = new Scene(loader.load());

			Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

			stage.setScene(scene);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
