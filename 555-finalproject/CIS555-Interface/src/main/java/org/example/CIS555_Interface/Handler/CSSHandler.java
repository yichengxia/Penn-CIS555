package org.example.CIS555_Interface.Handler;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.example.CIS555_Interface.Interface.InterfaceMain;

import spark.Request;
import spark.Response;
import spark.Route;
public class CSSHandler implements Route{

	@Override
	public Object handle(Request request, Response response) throws Exception {
        StringBuilder res = new StringBuilder();
        String name = request.params("name");
        try {
            Scanner myReader = new Scanner(new File(InterfaceMain.getPagePath() + "css/" + name));
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                res.append(line);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("getting css failed");
            e.printStackTrace();
        }
        response.type("text/css");
        return res;
	}

}
