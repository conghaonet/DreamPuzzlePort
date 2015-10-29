package org.hao.util;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Cong Hao on 2015/7/9.
 * Email: hao.cong@qq.com
 */
public class StringRequestWithHeader extends Request<String> {
	private final Response.Listener<String> mListener;
	private Map<String, String> sendHeader=new HashMap<String, String>(1);
	public StringRequestWithHeader(int method, String url, Listener<String> listener, ErrorListener errorListener) {
		super(method, url, errorListener);
		mListener = listener;
	}
	public StringRequestWithHeader(String url, Listener<String> listener, ErrorListener errorListener) {
		this(Method.GET, url, listener, errorListener);
	}

	@Override
	protected Response<String> parseNetworkResponse(NetworkResponse response) {
		String parsed = null;
		try {
			Map<String,String> headers = response.headers;

			parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
		} catch (UnsupportedEncodingException e) {
			parsed = new String(response.data);
		}
		return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
	}

	@Override
	protected void deliverResponse(String response) {
		mListener.onResponse(response);
	}
	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return sendHeader;
	}
	public void setSendCookie(String name, String value){
		sendHeader.put("cookie",name+"="+value);
	}
	public void setSendCookie(String cookie) {
		sendHeader.put("cookie", cookie);
	}
}
