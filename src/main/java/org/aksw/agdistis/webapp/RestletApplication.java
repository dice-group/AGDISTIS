package org.aksw.agdistis.webapp;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Extractor;
import org.restlet.routing.Router;

public class RestletApplication extends Application {

	private static final String NIF_BASED_WEB_SERVICE = "/nif";

	public static final String DEPTH_PARAMETER_NAME = "depth";
	public static final String HEURISTIC_PARAMETER_NAME = "heuristic";
	public static final String SIMILARITY_PARAMETER_NAME = "similarity";
	// public static final String TEXT_PARAMETER_NAME = "text";
	public static final String TYPE_PARAMETER_NAME = "type";

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public Restlet createInboundRoot() {
		// Create a router Restlet that routes each call to a
		// new instance of GetDisambiguation.
		Router router = new Router(getContext());
		// Create an extractor that extracts the needed parameters

		// Add the default route
		router.attachDefault(GetDisambiguation.class);

		// Add a route for NIF documents
		// 1. get parameters from the URI
		Extractor extractor = new Extractor(getContext());
		extractor.extractFromQuery(DEPTH_PARAMETER_NAME, DEPTH_PARAMETER_NAME, true);
		extractor.extractFromQuery(HEURISTIC_PARAMETER_NAME, HEURISTIC_PARAMETER_NAME, true);
		extractor.extractFromQuery(SIMILARITY_PARAMETER_NAME, SIMILARITY_PARAMETER_NAME, true);
		extractor.extractFromQuery(TYPE_PARAMETER_NAME, TYPE_PARAMETER_NAME, true);
		// 2. create the route
		extractor.setNext(NifBasedServerResource.class);
		router.attach(NIF_BASED_WEB_SERVICE, extractor);

		return router;
	}
}
