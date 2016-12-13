package org.aksw.agdistis.webapp;

import org.restlet.Component;
import org.restlet.data.Protocol;

public class RunApp {

	public static void main(String[] args) {
		try {
			// Create a new Component.
			Component component = new Component();

			// Add a new HTTP server listening on port 8082.
			component.getServers().add(Protocol.HTTP, 8080);

			// Attach the sample application.
			component.getDefaultHost().attach(new RestletApplication());

			// Start the component.
			component.start();
		} catch (Exception e) {
			// Something is wrong.
			e.printStackTrace();
		}
	}

}
