package edu.ucsc.ib.client;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

/**
 * Basic handling of JSON-RPC response object
 */
public class JsonRpcResponse {

	/**
	 * the JSONObject for this JSON-RPC response
	 */
	private final JSONObject jsonRpcRespJO;

	/**
	 * uses JSONParser.parseStrict(s)
	 * 
	 * @param s
	 */
	public JsonRpcResponse(final String s) {
		jsonRpcRespJO = JSONParser.parseStrict(s).isObject();
	}

	/**
	 * Get the "id" value.
	 * 
	 * @return
	 */
	public double getID() {
		return jsonRpcRespJO.get("id").isNumber().doubleValue();
	}

	/**
	 * Get the "error" JSONObject. If there is one, there should be an error
	 * message with the key, "message". If there is no error, null should be
	 * returned. Use hasError() to check if the error object is non-null.
	 * 
	 * @return
	 */
	public JSONObject getError() {
		return jsonRpcRespJO.get("error").isObject();
	}

	/**
	 * Get the "result" JSONObject.
	 * 
	 * @return
	 */
	public JSONObject getResult() {
		return jsonRpcRespJO.get("result").isObject();
	}

	/**
	 * Check if there is a JSON-RPC error.
	 * 
	 * @return
	 */
	public boolean hasError() {
		if (isNullValue(jsonRpcRespJO.get("error"))) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Convert to String
	 */
	public String toString() {
		return jsonRpcRespJO.toString();
	}

	/**
	 * Check if the JSONValue is null.
	 * 
	 * @param jv
	 * @return
	 */
	public static boolean isNullValue(JSONValue jv) {
		if (jv.toString().equalsIgnoreCase("null")) {
			return true;
		} else {
			return false;
		}
	}
}