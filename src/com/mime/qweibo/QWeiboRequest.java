package com.mime.qweibo;

import java.util.List;

public class QWeiboRequest {

	public QWeiboRequest() {

	}

	/**
	 * Do sync request.
	 * 
	 * @param url
	 *            The full url that needs to be signed including its non OAuth
	 *            url parameters
	 * @param httpMethod
	 *            The http method used. Must be a valid HTTP method verb
	 *            (POST,GET,PUT, etc)
	 * @param key
	 *            OAuth key
	 * @param listParam
	 *            Query parameters
	 * @param listFile
	 *            Files for post
	 * @return
	 * @throws Exception
	 */
	public String syncRequest(String url, String httpMethod, OauthKey key,
			List<QParameter> listParam, List<QParameter> listFile)
			throws Exception {
		if (url == null || url.equals("")) {
			return null;
		}
		QOAuth oauth = new QOAuth();

		StringBuffer sbQueryString = new StringBuffer();
		String oauthUrl = oauth.getOauthUrl(url, httpMethod, key.customKey,
				key.customSecrect, key.tokenKey, key.tokenSecrect, key.verify,
				key.callbackUrl, listParam, sbQueryString);
		String queryString = sbQueryString.toString();

		QHttpClient http = new QHttpClient();
		if ("GET".equals(httpMethod)) {
			return http.httpGet(oauthUrl, queryString);
		} else if ((listFile == null) || (listFile.size() == 0)) {
			return http.httpPost(oauthUrl, queryString);
		} else {
			return http.httpPostWithFile(oauthUrl, queryString, listFile);
		}
	}
	/** yuchberry added
	 * 
	 * @param url
	 * @param httpMethod
	 * @param key
	 * @param listParam
	 * @param _fileBuffer  file to added
	 * @param _fileType file type "image/jpeg"/"image/gif"/"image/png"
	 * @return
	 * @throws Exception
	 */
	public String syncRequest(String url, OauthKey key,
			List<QParameter> listParam,byte[] _fileBuffer,String _fileType)
			throws Exception {
		
		if (url == null || url.equals("")) {
			throw new IllegalArgumentException("syncRequest url == null"); 
		}
		
		QOAuth oauth = new QOAuth();

		StringBuffer sbQueryString = new StringBuffer();
		String oauthUrl = oauth.getOauthUrl(url, "POST", key.customKey,
				key.customSecrect, key.tokenKey, key.tokenSecrect, key.verify,
				key.callbackUrl, listParam, sbQueryString);
		String queryString = sbQueryString.toString();

		QHttpClient http = new QHttpClient();

		return http.httpPostWithFile(oauthUrl, queryString, _fileBuffer,_fileType);
		
	}

	/**
	 * Do async request
	 * 
	 * @param url
	 *            The full url that needs to be signed including its non OAuth
	 *            url parameters
	 * @param httpMethod
	 *            The http method used. Must be a valid HTTP method verb
	 *            (POST,GET,PUT, etc)
	 * @param key
	 *            OAuth key
	 * @param listParam
	 *            Query parameters
	 * @param listFile
	 *            Files for post
	 * @param asyncHandler
	 *            The async handler
	 * @param cookie
	 *            Cookie response to handler
	 * @return
	 */
	public boolean asyncRequest(String url, String httpMethod, OauthKey key,
			List<QParameter> listParam, List<QParameter> listFile,
			QAsyncHandler asyncHandler, Object cookie) {
		
		QOAuth oauth = new QOAuth();

		StringBuffer sbQueryString = new StringBuffer();
		String oauthUrl = oauth.getOauthUrl(url, httpMethod, key.customKey,
				key.customSecrect, key.tokenKey, key.tokenSecrect, key.verify,
				key.callbackUrl, listParam, sbQueryString);
		String queryString = sbQueryString.toString();

		QAsyncHttpClient asyncHttp = new QAsyncHttpClient();
		if ("GET".equals(httpMethod)) {
			return asyncHttp.httpGet(oauthUrl, queryString, asyncHandler,
					cookie);
		} else if ((listFile == null) || (listFile.size() == 0)) {
			return asyncHttp.httpPost(oauthUrl, queryString, asyncHandler,
					cookie);
		} else {
			return asyncHttp.httpPostWithFile(oauthUrl, queryString, listFile,
					asyncHandler, cookie);
		}
	}
}
