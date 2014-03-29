package models;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.ebean.Model;

@Entity
public class Line extends Model{
	
	@Id
	public int id;
	
	public String name;

}
