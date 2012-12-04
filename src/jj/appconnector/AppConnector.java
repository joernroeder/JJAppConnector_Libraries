package jj.appconnector;

import processing.core.PImage;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONObject;

public class AppConnector implements IOCallback {

	// version
	private float version = (float) 0.1;

	// server
	private String defaultHost = "http://appconnector.jit.su";
	private int defaultPort = 80;
	private String host;
	private int port;
	
	private Properties props;

	// socket connection
	private SocketIO socket;

	// app
	private String appKey;

	private HashMap<String, String> descriptions; // varName, description
	private HashMap<String, Object> publications; // varName, value
	private HashMap<String, AppData> subscriptions; // varName, value
	private HashMap<String, String> subscriptionsDataTypes; // varName, dataType
	private HashMap<String, String> subscriptionsShortcuts; // shortcut, name

	private HashMap<String, String> currentApps; // appKey, appTitle
	private ArrayList<String> updatingList; // varName

	private boolean subscriptionsSent = true;
	private boolean publicationsSent = true;
	private boolean isConnected;
	private boolean isDebug;

	private Logger logger;

	public AppConnector(String appKey) {
		this.appKey = appKey;

		this.descriptions = new HashMap<String, String>();
		this.publications = new HashMap<String, Object>();
		this.subscriptions = new HashMap<String, AppData>();
		this.subscriptionsDataTypes = new HashMap<String, String>();
		this.subscriptionsShortcuts = new HashMap<String, String>();

		this.updatingList = new ArrayList<String>();

		this.currentApps = new HashMap<String, String>();

		this.props = new Properties();

		this.props.setProperty("appkey", this.appKey);
		this.props.setProperty("appversion", String.valueOf(this.version));
		this.props.setProperty("apptype", "processing");

		logger = Logger.getLogger("io.socket");

		// set logging to false by default
		setDebug(false);
	}

	/**
	 * Returns, if a connection is established at the moment
	 * 
	 * @return true if a connection is established, false if the transport is
	 *         not connected or currently connecting
	 */
	public boolean isConnected() { 
		return (this.subscriptionsSent && this.publicationsSent) ? true : false;
	}

	public boolean isConnected(String appName) {
		return currentApps.values().contains(appName);
	}

	public void setDebug(boolean val) {
		this.isDebug = val;
		logger.setUseParentHandlers(val);
	}

	public boolean isDebug() {
		return isDebug;
	}

	public void debug(Object obj) {
		if (isDebug()) {
			System.out.println(obj);
		}
	}

	public float version() {
		return version;
	}
	
	/*
	 * Socket
	 */
	
	public void start() {
		this.checkPortAndHost();
		
		try {
			this.socket = new SocketIO(this.host + ":" + this.port + "/app", this.props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (this.socket != null) {
			connectSocket();
		} else {
			debug("no socket");
		}
	}
	
	public void setServer(String host, int port) {
		this.host = host;
		this.port = port;
	}

	// --- publish ------------------------------------------------

	public void addPublication(String varName, String desc) {
		this.publicationsSent = false;
		descriptions.put(varName, desc);
	}

	public void publish(String varName, Object val) {

		if (val instanceof processing.core.PImage) {
			String s = null;
			try {
				byte[] imageBytes = ImageHelper.extractBytes((PImage) val);
				s = ImageHelper.encodeBase64(imageBytes);
			} catch (IOException e) {
				e.printStackTrace();
			}
			val = s;
		}

		if (!descriptions.containsKey(varName)) {
			debug("wrong varName");
			return;
		}

		if (updatingList.contains(varName)) {
			debug("still updating");
			return;
		}

		if (!isConnected()) {
			debug("not connected");
			return;
		}

		if (isNewVal(varName, val) && !updatingList.contains(varName)) {
			updatingList.add(varName);
			// updating = true;

			// cache value
			publications.put(varName, val);
			// publish
			socket.emit("update " + varName, val);
		}
	}

	// --- subscribe ----------------------------------------------

	// joern.color
	public void subscribeTo(String name) {
		String[] app = name.split("\\.");

		if (app.length >= 2) {
			subscribeTo(name, app[1]);
		} else {
			debug("no appspace defined for varname: " + name
					+ ". Format: app.varname");
		}
	}

	public void subscribeTo(String name, String varName) {
		this.subscriptionsSent = false;
		AppData appData = new AppData();
		subscriptions.put(name, appData);

		// add and check shortcuts
		if (!subscriptionsShortcuts.containsKey(varName)) {
			subscriptionsShortcuts.put(varName, name);
		} else {
			debug("");
		}
	}

	public AppData get(String varName) {
		AppData data = null;
		// String dataType = null; @deprecated
		if (varName.indexOf(".") > -1) {
			data = subscriptions.get(varName);
			// dataType = subscriptionsDataTypes.get(varName); @deprecated
		} else {
			String shortcut = subscriptionsShortcuts.get(varName);
			data = subscriptions.get(shortcut);
			// dataType = subscriptionsDataTypes.get(shortcut); @deprecated
		}

		return data;
	}

	// === PRIVATE METHODS ========================================

	private boolean isNewVal(String varName, Object val) {
		if (publications.containsKey(varName)) {
			Object oldVal = publications.get(varName);

			if (oldVal.equals(val))
				return false;
		}

		return true;
	}

	private void connectSocket() {
		this.socket.connect(this);
	}

	private void updatePublications() {
		socket.emit("set publications", descriptions);
	}

	private void sendSubscriptions() {
		socket.emit("set subscriptions", subscriptions.keySet());
	}
	
	private void checkPortAndHost() {
		this.host = (this.host != null) ? this.host : this.defaultHost;
		this.port = (this.port > 0) ? this.port : this.defaultPort;
	}

	// --- SOCKET -------------------------------------------------

	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		try {
			debug("Server said:" + json.toString(2));
		} catch (Exception e) {
			debug(e.getStackTrace());
		}
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		debug("Server said: " + data);
	}

	@Override
	public void onError(SocketIOException socketIOException) {
		debug("an Error occured");
		debug(socketIOException.getStackTrace());
	}

	@Override
	public void onDisconnect() {
		debug("Connection terminated.");

		isConnected = false;
	}

	@Override
	public void onConnect() {
		debug("Connection established");

		updatePublications();
		sendSubscriptions();
	}

	// handle custom socket events and there callbacks defined at
	// SocketCallbacks.java
	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {

		String eventName = event.replace(" ", "_").toUpperCase();

		try {
			switch (SocketCallbacks.valueOf(eventName)) {

			case OBSOLETE_VERSION:
				onObsoleteVersion(args);
				break;

			case CONNECT_FAILED:
				onError((SocketIOException) args[0]);
				break;

			case CURRENTAPPS:
				updateCurrentApps(args);
				break;

			case PUBLICATIONS_SUCCESS:
				onPublicationsSuccess(args);
				break;

			case SUBSCRIPTIONS_SUCCESS:
				onSubscriptionsSuccess(args);
				break;

			case UPDATE_SUCCESS:
				onUpdateSuccess(args);
				break;

			case BROADCAST:
				onBroadcast(args);
				break;

			default:
				debug("Server triggered event '" + event + "'");
				break;
			}
		} catch (Exception e) {
			debug(e.getStackTrace());
		}
	}

	void onObsoleteVersion(Object... args) {
		if (isDebug()) {
			debug(args[0]);
			System.exit(0);
		}
	}

	void onPublicationsSuccess(Object... args) {
		boolean success = (Boolean) args[0];

		if (!success) {
			System.out.println("something went wrong! please try again :)");
		} else {
			this.publicationsSent = true;
		}
	}

	void onSubscriptionsSuccess(Object... args) {
		JSONObject subMap = (JSONObject) args[0];

		Iterator<?> keys = subMap.keys();

		subscriptions.clear();

		while (keys.hasNext()) {
			String key = (String) keys.next();

			JSONObject subData;
			try {
				subData = (JSONObject) subMap.get(key);
				AppData appData = new AppData();
				appData.setValue(subData.get("data"));
				subscriptions.put(key, appData);

				String dataType = subData.get("type") != null ? (String) subData
						.get("type") : null;
				subscriptionsDataTypes.put(key, dataType.toUpperCase());
			} catch (Exception e) {
				debug(e.getStackTrace());
			}
		}
		
		this.subscriptionsSent = true;
	}

	void updateCurrentApps(Object... args) {
		JSONObject current = (JSONObject) args[0];
		Iterator<?> keys = current.keys();

		currentApps.clear();

		while (keys.hasNext()) {
			String key = (String) keys.next();

			if (!key.equals(this.appKey)) {
				try {
					currentApps.put(key, (String) current.get(key));
				} catch (Exception e) {
					debug(e.getStackTrace());
				}
			}
		}

		debug(currentApps);
	}

	void onUpdateSuccess(Object... args) {
		String varName = (String) args[0];
		boolean success = (Boolean) args[1];

		if (updatingList.contains(varName) && success) {
			updatingList.remove(updatingList.indexOf(varName));
			updatingList.trimToSize();
		}
	}

	void onBroadcast(Object... args) {
		if (args.length < 2)
			return;

		String name = (String) args[0];
		Object val = args[1];

		if (subscriptions.containsKey(name)) {
			AppData appData = subscriptions.get(name);
			appData.setValue(val);
			subscriptions.put(name, appData);
		}
	}
}
