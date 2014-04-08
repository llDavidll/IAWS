package models;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import play.db.ebean.Model;

@Entity
public class Line extends Model {

	private static final long serialVersionUID = 1L;

	@Version
	public Timestamp lastUpdate;

	@Id
	public int id;

	public long lineId;

	public String name;

}
