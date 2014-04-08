package models;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;
import controllers.VeloToulouse;

@Entity
public class StationsForm extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public int id;

	public static Map<String, String> options() {
		return VeloToulouse.getAllStations();
	}
}
