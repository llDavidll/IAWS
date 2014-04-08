package models;

import java.util.Map;

import javax.persistence.Id;

import controllers.Tisseo;
import play.db.ebean.Model;

public class LineStopsForm extends Model {

	private static final long serialVersionUID = 1L;

	@Id
	public long stopId;

	public long lineId;

	public static Map<String, String> options(long lineId) {
		return Tisseo.getStopsFromLine(lineId);
	}
}
