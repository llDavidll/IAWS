package models;

import play.db.ebean.Model;

public class Station extends Model {

	private static final long serialVersionUID = 1L;

	public int id;
	public String name;

	public boolean open;
	public int available_stands;
	public int available_bikes;

}
