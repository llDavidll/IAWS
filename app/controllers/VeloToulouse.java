package controllers;

import static play.data.Form.form;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import models.Station;
import models.StationsForm;
import play.api.libs.ws.Response;
import play.api.libs.ws.WS;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import com.fasterxml.jackson.databind.JsonNode;

import config.Config;

public class VeloToulouse extends Controller {

	/**
	 * Displays a form to choose a station
	 * 
	 * @return
	 */
	public static Result askStation() {
		// Create the form object
		Form<StationsForm> stationForm = form(StationsForm.class);
		return ok(views.html.velotoulouse.askStation.render(stationForm));
	}

	/**
	 * Displays the informations about a station
	 * 
	 * @return
	 */
	public static Result displayResults() {
		// Retrieve the form object
		Form<StationsForm> stationForm = form(StationsForm.class)
				.bindFromRequest();
		if (stationForm.hasErrors()) {
			// If there are errors
			return badRequest(views.html.velotoulouse.askStation
					.render(stationForm));
		}
		return ok(views.html.velotoulouse.displayResults
				.render(getStationInfo(stationForm.get().id)));
	}

	/**
	 * Retrieve the name of all the stations from jcdecaux's API
	 * 
	 * @return a map containing all the ids and names of the stations
	 */
	public static Map<String, String> getAllStations() {
		// Create a treemap to sort all results by their id
		TreeMap<String, String> options = new TreeMap<String, String>(
				new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						if (Integer.parseInt(o1) > Integer.parseInt(o2)) {
							return 1;
						}
						if (Integer.parseInt(o1) < Integer.parseInt(o2)) {
							return -1;
						}
						return 0;
					}
				});

		// Create a http request
		Future<Response> future = WS.url(
				"https://api.jcdecaux.com/vls/v1/stations" + Config.JC_DECAUX)
				.get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			// Iterate over the response to populate the results
			Iterator<JsonNode> stationIte = jsonNode.iterator();
			JsonNode temp;
			while (stationIte.hasNext()) {
				temp = stationIte.next();
				options.put(temp.path("number").asText(), temp.path("name")
						.asText());
			}
			return options;
		} catch (Exception e) {
			e.printStackTrace();
			return options;
		}
	}

	/**
	 * Retrieve the informations about a station from jcdecaux's API
	 * 
	 * @param pId
	 *            the id of the station to retrieve
	 * @return station object containing all the infos
	 */
	public static Station getStationInfo(int pId) {
		Station mStation = new Station();
		// Create a http request
		Future<Response> future = WS.url(
				"https://api.jcdecaux.com/vls/v1/stations/" + pId
						+ Config.JC_DECAUX).get();
		try {
			// Retrieve json response
			Response result = Await.result(future,
					Duration.apply(30, TimeUnit.SECONDS));
			// Parse json response
			JsonNode jsonNode = Json.parse(result.json().toString());
			mStation.id = jsonNode.path("number").asInt();
			mStation.name = jsonNode.path("name").asText();
			mStation.available_bikes = jsonNode.path("available_bikes").asInt();
			mStation.available_stands = jsonNode.path("available_bike_stands")
					.asInt();
			mStation.open = jsonNode.path("status").asText()
					.equalsIgnoreCase("open");
			return mStation;
		} catch (Exception e) {
			e.printStackTrace();
			return mStation;
		}
	}

}
