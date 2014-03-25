package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class MainMenu extends Controller {

    public static Result index() {
    	
        return ok(views.html.mainmenu.index.render());
    }

}
