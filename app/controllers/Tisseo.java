package controllers;

import static play.data.Form.form;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import models.Line;
import models.LineStopsForm;
import models.LinesForm;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;
import play.data.Form;
import play.db.ebean.Model;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import com.fasterxml.jackson.databind.JsonNode;

import config.Config;

public class Tisseo extends Controller {

	/**
	 * Add a selected line to the database
	 * 
	 * @return
	 */
	public static Result addLine() {
		Line line = form(Line.class).bindFromRequest().get();
		// Create the form object for the askStop view
		Form<LineStopsForm> stopsForm = form(LineStopsForm.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Line> lines = new Model.Finder(int.class, Line.class).all();
		for (Line l : lines) {
			if (l.lineId == line.lineId) {
				l.update();
				return ok(views.html.tisseo.askStop.render(stopsForm,
						getLineName(line.lineId), line.lineId));
			}
		}
		line.save();
		return ok(views.html.tisseo.askStop.render(stopsForm,
				getLineName(line.lineId), line.lineId));
	}

	/**
	 * Remove a selected line from the database
	 * 
	 * @return
	 */
	public static Result removeLine() {
		Line line = form(Line.class).bindFromRequest().get();
		// Create the form object for the askStop view
		Form<LineStopsForm> stopsForm = form(LineStopsForm.class);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Line> lines = new Model.Finder(int.class, Line.class).all();
		for (Line l : lines) {
			if (l.lineId == line.lineId) {
				l.delete();
				return ok(views.html.tisseo.askStop.render(stopsForm,
						getLineName(line.lineId), line.lineId));
			}
		}
		return ok(views.html.tisseo.askStop.render(stopsForm,
				getLineName(line.lineId), line.lineId));
	}

	/**
	 * Check if a line is already liked
	 * 
	 */
	public static boolean isLineLiked(long pLineId) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Line> lines = new Model.Finder(int.class, Line.class).all();
		for (Line l : lines) {
			if (l.lineId == pLineId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Displays in json format the entire database
	 * 
	 * @return
	 */
	public static Result getLines() {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<Line> lines = new Model.Finder(int.class, Line.class).all();
		return ok(Json.toJson(lines));
	}

	/**
	 * Displays a form to choose a line
	 * 
	 * @return
	 */
	public static Result askLine() {
		// Create the form object
		Form<LinesForm> linesForm = form(LinesForm.class);
		return ok(views.html.tisseo.askLine.render(linesForm));
	}

	/**
	 * Displays a form to choose a stop
	 * 
	 * @return
	 */
	public static Result askStop() {
		// Retrieve the form object
		Form<LinesForm> linesForm = form(LinesForm.class).bindFromRequest();
		if (linesForm.hasErrors()) {
			// If there are errors
			return badRequest(views.html.tisseo.askLine.render(linesForm));
		}
		// Create the form object
		Form<LineStopsForm> stopsForm = form(LineStopsForm.class);

		return ok(views.html.tisseo.askStop.render(stopsForm,
				getLineName(linesForm.get().id), linesForm.get().id));
	}

	/**
	 * Displays the informations about a stop and a line
	 * 
	 * @return
	 */
	public static Result displayStop() {
		// Retrieve the form object
		Form<LineStopsForm> stopForm = form(LineStopsForm.class)
				.bindFromRequest();
		if (stopForm.hasErrors()) {
			// If there are errors
			Form<LinesForm> linesForm = form(LinesForm.class);
			return badRequest(views.html.tisseo.askLine.render(linesForm));
		}
		return ok(views.html.tisseo.displayStop.render(stopForm.get()));
	}

	/**
	 * Retrieve the name of all the lines close to Paul Sabatier from tisseo API
	 * 
	 * @return a map containing all the ids and names of the lines
	 */
	public static Map<String, String> getLinesFromArea() {
		// Create a list to sort all the results by name
		List<TisseoLine> sortedList = new ArrayList<TisseoLine>();
		// Create a treemap to store all the results
		Map<String, String> options = new LinkedHashMap<String, String>();
		// Create a http request
		Future<Response> future = WS
				.url("http://pt.data.tisseo.fr/stopPointsList?displayLines=1&format=json&bbox=1.462553%2c43.547493%2c1.467660%2c43.575372"
						+ Config.TISSEO).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Get the stop areas
			jsonNode = jsonNode.path("physicalStops").path("physicalStop");
			// Iterate over the response to populate the results
			Iterator<JsonNode> stopIte = jsonNode.iterator();
			Iterator<JsonNode> destinationIte;
			Iterator<JsonNode> lineIte;
			JsonNode tempStop, tempDestination, tempLine;
			while (stopIte.hasNext()) {
				tempStop = stopIte.next();
				destinationIte = tempStop.get("destinations").iterator();
				while (destinationIte.hasNext()) {
					tempDestination = destinationIte.next();
					lineIte = tempDestination.get("line").iterator();
					while (lineIte.hasNext()) {
						tempLine = lineIte.next();
						options.put(tempLine.get("id").asText(),
								tempLine.get("shortName").asText() + " - "
										+ tempLine.get("name").asText());
					}
				}
			}
			// We have the data without duplicates, now sort it
			for (Map.Entry<String, String> entry : options.entrySet()) {
				sortedList
						.add(new TisseoLine(entry.getKey(), entry.getValue()));
			}
			Collections.sort(sortedList);
			// Save in the treeset
			options.clear();
			for (TisseoLine line : sortedList) {
				options.put(line.id, line.name);
			}
			return options;
		} catch (Exception e) {
			e.printStackTrace();
			return options;
		}
	}

	/**
	 * Retrieve the name of all the lines close to Paul Sabatier from tisseo API
	 * 
	 * @return a map containing all the ids and names of the lines
	 */
	public static Map<String, String> getStopsFromLine(long pLineId) {
		// Create a treemap to store all the results
		Map<String, String> options = new LinkedHashMap<String, String>();
		// Create a http request
		Future<Response> future = WS.url(
				"http://pt.data.tisseo.fr/stopPointsList"
						+ "?displayDestinations=1" + "&format=json"
						+ "&lineId=" + pLineId + "&bbox=" + Config.GPS_COORD
						+ Config.TISSEO).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Get the stop areas
			jsonNode = jsonNode.path("physicalStops").path("physicalStop");
			// Iterate over the response to populate the results
			Iterator<JsonNode> stopIte = jsonNode.iterator();
			Iterator<JsonNode> destinationIte;
			JsonNode tempStop, tempDestination;
			while (stopIte.hasNext()) {
				tempStop = stopIte.next();
				destinationIte = tempStop.get("destinations").iterator();
				while (destinationIte.hasNext()) {
					tempDestination = destinationIte.next();
					options.put(tempStop.get("id").asText(),
							tempStop.get("name") + " vers "
									+ tempDestination.get("name").asText());
				}
			}
			return options;
		} catch (Exception e) {
			e.printStackTrace();
			return options;
		}
	}

	/**
	 * Retrieve the next passage of a line to a stop
	 * 
	 */
	public static Map<Integer, String> getScheduleForStop(long pLineId,
			long pStopId) {
		// Create a treemap to store all the results
		Map<Integer, String> schedule = new TreeMap<>();
		// Create a http request
		Future<Response> future = WS.url(
				"http://pt.data.tisseo.fr/departureBoard" + "?format=json"
						+ "&displayRealTime=1" + "&lineId=" + pLineId
						+ "&stopPointId=" + pStopId + Config.TISSEO).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Get the stop areas
			jsonNode = jsonNode.path("departures").path("departure");
			// Iterate over the response to populate the results
			Iterator<JsonNode> timeIte = jsonNode.iterator();
			JsonNode tempTime;
			SimpleDateFormat dateParser = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
			Date tempDate;
			Date currentDate = new Date();
			while (timeIte.hasNext()) {
				tempTime = timeIte.next();
				tempDate = dateParser.parse(tempTime.get("dateTime").asText());
				schedule.put(
						getDateDiff(currentDate, tempDate, TimeUnit.MINUTES),
						dateFormat.format(tempDate));
			}
			return schedule;
		} catch (Exception e) {
			e.printStackTrace();
			return schedule;
		}
	}

	/**
	 * Get a diff between two dates
	 * 
	 * @param date1
	 *            the oldest date
	 * @param date2
	 *            the newest date
	 * @param timeUnit
	 *            the unit in which you want the diff
	 * @return the diff value, in the provided unit
	 */
	public static int getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		long diffInMillies = date2.getTime() - date1.getTime();
		return (int) timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
	}

	public static String getLineName(long pLineId) {
		String lineName = "";
		// Create a http request
		Future<Response> future = WS.url(
				"http://pt.data.tisseo.fr/linesList" + "?format=json"
						+ "&lineId=" + pLineId + Config.TISSEO).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Get the line
			jsonNode = jsonNode.path("lines").path("line");
			jsonNode = jsonNode.get(0);
			if (jsonNode != null) {
				lineName = jsonNode.get("shortName").asText() + " - "
						+ jsonNode.get("name").asText();
			}
			return lineName;
		} catch (Exception e) {
			e.printStackTrace();
			return lineName;
		}
	}

	public static String getStopName(long pStopId) {
		String stopName = "";
		// Create a http request
		Future<Response> future = WS.url(
				"http://pt.data.tisseo.fr/departureBoard" + "?format=json"
						+ "&stopPointId=" + pStopId + Config.TISSEO).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Get the stop areas
			jsonNode = jsonNode.path("departures").path("stop");
			stopName = jsonNode.get("name").asText();
			return stopName;
		} catch (Exception e) {

			e.printStackTrace();
			return stopName;
		}
	}

	private static class TisseoLine implements Comparable<TisseoLine> {
		private String id;
		private String name;

		public TisseoLine(String pId, String pName) {
			id = pId;
			name = pName;
		}

		@Override
		public int compareTo(TisseoLine o) {
			return name.compareTo(o.name);
		}
	}
}
