package models;

import java.util.Map;

import javax.persistence.Id;

import play.db.ebean.Model;
import controllers.Tisseo;

public class LinesForm extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public long id;

	public static Map<String, String> options() {
		return Tisseo.getLinesFromArea();
	}
}
