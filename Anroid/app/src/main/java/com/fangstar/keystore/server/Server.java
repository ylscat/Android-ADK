package com.fangstar.keystore.server;

import org.json.JSONException;
import org.json.JSONObject;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created at 2016/7/5.
 *
 * @author YinLanShan
 */
public class Server extends NanoHTTPD {
    private JSONObject mJson;
    public Server(int port) {
        super(port);
        mJson = new JSONObject();
        try {
            mJson.put("Server", "NanoHTTPD");
            mJson.put("Message", "Hello World!");
        }
        catch (JSONException e) {
            //ignore
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        return newFixedLengthResponse(Response.Status.OK, "text/json", mJson.toString());
    }
}
