package com.mime.qweibo;

public class OauthKey {
	public String customKey;
	public String customSecrect;
	public String tokenKey;
	public String tokenSecrect;
	public String verify;
	public String callbackUrl;

	
	public void reset() {		
		tokenKey = null;
		tokenSecrect = null;
		verify = null;
		callbackUrl = null;
	}
	
	
}
