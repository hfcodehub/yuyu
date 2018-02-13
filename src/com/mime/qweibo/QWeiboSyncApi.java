package com.mime.qweibo;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import weibo4j.org.json.JSONObject;

//yuchberry modified
//

public class QWeiboSyncApi {
	
	public static boolean sm_debug = false;

	final static String fsm_requestTokenURL 			= "https://open.t.qq.com/cgi-bin/request_token";
	final static String fsm_authorizeTokenURL 			= "http://open.t.qq.com/cgi-bin/authorize";
	final static String fsm_accessTokenURL				= "https://open.t.qq.com/cgi-bin/access_token";
	
	final static String fsm_verifyURL					= "http://open.t.qq.com/api/user/info";
	final static String fsm_homeTimelineURL			= "http://open.t.qq.com/api/statuses/home_timeline";
	final static String fsm_mentionMeURL				= "http://open.t.qq.com/api/statuses/mentions_timeline";
	
	final static String fsm_publishURL					= "http://open.t.qq.com/api/t/add";
	final static String fsm_publishFileURL				= "http://open.t.qq.com/api/t/add_pic";
	final static String fsm_publishReplyURL			= "http://open.t.qq.com/api/t/reply";
	final static String fsm_publishForwardURL			= "http://open.t.qq.com/api/t/re_add";
	final static String fsm_publishCommentURL			= "http://open.t.qq.com/api/t/comment";
	
	final static String fsm_followUserURL				= "http://open.t.qq.com/api/friends/add";
	final static String fsm_unfollowUserURL			= "http://open.t.qq.com/api/friends/del";
	
	final static String fsm_directMessageInboxURL		= "http://open.t.qq.com/api/private/recv";
	final static String fsm_directMessageOutboxURL		= "http://open.t.qq.com/api/private/send";
	
	final static String fsm_deleteMessageURL			= "http://open.t.qq.com/api/t/del";
	final static String fsm_favoriteMessageURL			= "http://open.t.qq.com/api/fav/addt";
	
	final static String fsm_userInfoURL				= "http://open.t.qq.com/api/user/other_info";
	final static String fsm_userTimelingURL			= "http://open.t.qq.com/api/statuses/user_timeline";
	
	final static String fsm_sendDirectMsgURL			= "http://open.t.qq.com/api/private/add";
	
	final static String fsm_ownFollowingList			= "http://open.t.qq.com/api/friends/idollist_name";
	final static String fsm_ownFollowerList			= "http://open.t.qq.com/api/friends/fanslist_name";
	
	final static String fsm_userFollowingList			= "http://open.t.qq.com/api/friends/user_idollist";
	final static String fsm_userFollowerList			= "http://open.t.qq.com/api/friends/user_fanslist";
	
	
	OauthKey 			m_oauthKey = new OauthKey();
	QWeiboRequest 		m_request = new QWeiboRequest();
	
	List<QParameter> 	m_parameters = new ArrayList<QParameter>();
	
	public OauthKey getOauthKey(){
		return m_oauthKey;
	}
	
	public void setCostomerKey(String customKey, String customSecret){
		m_oauthKey.customKey = customKey;
		m_oauthKey.customSecrect = customSecret;
	}
	
	public void setAccessToken(String tokenKey, String tokenSecret){
		m_oauthKey.tokenKey = tokenKey;
		m_oauthKey.tokenSecrect = tokenSecret;
	}
	
	private void checkAllKeys()throws Exception{
		if(m_oauthKey.customKey == null || m_oauthKey.customSecrect == null){
			throw new Exception("customKey or customSecrect is null");
		}
		
		if(m_oauthKey.tokenKey == null || m_oauthKey.tokenSecrect == null){
			throw new Exception("access tokenKey or access tokenSecrect is null");
		}
	}
	
	// return user id if verify ok
	public QUser verifyCredentials()throws Exception{
		
		checkAllKeys();
		
		m_parameters.clear();
		m_parameters.add(new QParameter("format", "json"));
		
		JSONObject t_json = new JSONObject(m_request.syncRequest(fsm_verifyURL, "GET", m_oauthKey, m_parameters, null));
		if(t_json.getInt("ret") != 0){
			throw new Exception("verify Credentials failed." + t_json.toString());
		}
		
		return new QUser(t_json.getJSONObject("data")); 
	}
	
	public String getVerifyPinURL(String _callback)throws Exception{
			
		if(m_oauthKey.customKey == null || m_oauthKey.customSecrect == null){
			throw new Exception("customKey or customSecrect is null");
		}
		
		m_parameters.clear();
		m_oauthKey.reset();
				
		//The OAuth Call back URL(You should encode this url if it
		//contains some unreserved characters).
		//
		if(_callback != null){
			m_oauthKey.callbackUrl = _callback;
		}else{
			// the follow document said "must has oauth_callback parameter or set string 'null' " Orz
			// http://open.t.qq.com/resource.php?i=1,2
			//
			m_oauthKey.callbackUrl = "null";
		}

		String t_response = m_request.syncRequest(fsm_requestTokenURL, "GET", m_oauthKey, m_parameters, null);
		
		if(!parseToken(t_response)){
			throw new Exception("error server request PIN URL response :" + t_response); 
		}
				
		return fsm_authorizeTokenURL + "?oauth_token=" + m_oauthKey.tokenKey;
	}
	
	public OauthKey requestTokenByVerfiyPIN(String _verifyPIN)throws Exception{
		
		checkAllKeys();
		
		m_parameters.clear();
		m_oauthKey.verify = _verifyPIN;
		m_oauthKey.callbackUrl = null;

		String t_response = m_request.syncRequest(fsm_accessTokenURL, "GET", m_oauthKey, m_parameters, null);			
				
		if(!parseToken(t_response)){
			throw new Exception("error server request token response :" + t_response); 
		}
		
		return m_oauthKey;
	}
	
	private boolean parseToken(String response) {
		if (response == null || response.equals("")) {
			return false;
		}

		String[] tokenArray = response.split("&");

		if (tokenArray.length < 2) {
			return false;
		}

		String strTokenKey = tokenArray[0];
		String strTokenSecrect = tokenArray[1];

		String[] token1 = strTokenKey.split("=");
		if (token1.length < 2) {
			return false;
		}
		
		m_oauthKey.tokenKey = token1[1];		

		String[] token2 = strTokenSecrect.split("=");
		if (token2.length < 2) {
			return false;
		}

		m_oauthKey.tokenSecrect = token2[1];
		
		return true;
	}

	/**
	 *  get the home weibo list (time line)
	 * @param _startTime	start geting time
	 * @param _num			getting number (0,70]
	 * @return				the list of QWeibo
	 * @throws Exception	
	 */
	public List<QWeibo> getHomeList(long _startTime,int _num)throws Exception{
		
		if(_num > 70){
			_num = 70;
		}
		
		return getWeiboList(fsm_homeTimelineURL,_startTime,_num,null);
	}
	
	/**
	 * get the 20 default weibo timelime
	 * @return
	 * @throws Exception
	 */
	public List<QWeibo> getHomeList()throws Exception{
		return getHomeList(0,20);
	}
	
	/**
	 *  get the mention list of current user
	 * @param _startTime	start time of getting
	 * @param _num			getting number
	 * @return				weibo list 
	 * @throws Exception
	 */
	public List<QWeibo> getMentionList(long _startTime,int _num)throws Exception{
		
		if(_num > 100){
			_num = 100;
		}
		
		return getWeiboList(fsm_mentionMeURL,_startTime,_num,null);		
	}
	
	/**
	 *  get weibo list max 20 items
	 * @return
	 * @throws Exception
	 */
	public List<QWeibo> getMentionList()throws Exception{
		return getMentionList(0,20);
	}	

	public List<QWeibo> getUserTimeline(String _name)throws Exception{
		return getWeiboList(fsm_userTimelingURL,0,10,_name);
	}
	
	public QUser getUserInfo(String _name)throws Exception{
		
		m_parameters.clear();
		
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("name",URLEncoder.encode(_name,"UTF-8")));
		

		JSONObject t_json = new JSONObject(m_request.syncRequest(fsm_userInfoURL, "GET", m_oauthKey, m_parameters, null));
		if(t_json.getInt("ret") != 0){
			throw new Exception("get User info error:" + t_json.toString());
		}
		
		return new QUser(t_json.getJSONObject("data")); 
				
	}
	
	public void sendDirectMessage(String _screenName, String _text,String _ip,
									long _latitude,long _longitude)throws Exception{
		m_parameters.clear();
		
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("name",_screenName));
		m_parameters.add(new QParameter("content",_text));
		
		if(_latitude != -1 && _longitude != -1){
			m_parameters.add(new QParameter("wei", Long.toString(_latitude)));
			m_parameters.add(new QParameter("jing", Long.toString(_longitude)));
		}else if(_ip != null){
			m_parameters.add(new QParameter("clientip", _ip));
		}
		
		JSONObject t_json = new JSONObject(m_request.syncRequest(fsm_sendDirectMsgURL, "POST", m_oauthKey, m_parameters, null));
		if(t_json.getInt("ret") != 0){
			throw new Exception("send direct message error:" + t_json.toString());
		}
	}
	
	/**
	 *  private sub-function to get the weibo list information by url 
	 * @param _url		
	 * @param _time			starttime
	 * @param _num			request number
	 * @return
	 * @throws Exception
	 */
	private List<QWeibo> getWeiboList(String _url,long _time,int _num,String _userName)throws Exception{
						
		m_parameters.clear();
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("pageflag", String.valueOf(0)));
		m_parameters.add(new QParameter("reqnum", Integer.toString(_num)));
		m_parameters.add(new QParameter("pageTime", Long.toString(_time / 1000))); //to convert GMT time
		
		if(_userName != null){
			m_parameters.add(new QParameter("name", _userName));
		}
		
		return QWeibo.getWeiboList(new JSONObject(m_request.syncRequest(_url, "GET", 
																	m_oauthKey, m_parameters, null)));
	}
	

	/**
	 * get the my following list
	 * @param _pageIdx		page index (0 - n)
	 * @param _reqNum		request number 1-200
	 * @return
	 * @throws Exception
	 */
	public QOpenIDs getOwnFollowingList(int _pageIdx,int _reqNum)throws Exception{
		return getFollowingFollowerList(fsm_ownFollowingList, null, _pageIdx, _reqNum);
	}
	
	/**
	 * get the my own follower list 
	 * @param _pageIdx
	 * @param _reqNum
	 * @return
	 * @throws Exception
	 */
	public QOpenIDs getOwnFollowerList(int _pageIdx,int _reqNum)throws Exception{
		return getFollowingFollowerList(fsm_ownFollowerList, null, _pageIdx, _reqNum);
	}
	
	/**
	 * get the user following list
	 * @param _name 			user name
	 * @param _pageIdx
	 * @param _reqNum
	 * @return
	 * @throws Exception
	 */
	public QOpenIDs getUserFollowingList(String _name,int _pageIdx,int _reqNum)throws Exception{
		return getFollowingFollowerList(fsm_userFollowingList,_name,_pageIdx,_reqNum);
	}
	
	/**
	 * get the user follower list
	 * @param _name			user name
	 * @param _pageIdx
	 * @param _reqNum
	 * @return
	 * @throws Exception
	 */
	public QOpenIDs getUserFollowerList(String _name,int _pageIdx,int _reqNum)throws Exception{
		return getFollowingFollowerList(fsm_userFollowerList,_name,_pageIdx,_reqNum);
	}
	
	/**
	 * get the user relationship API ids
	 * @param _url
	 * @param _name
	 * @param _pageIdx
	 * @param _reqNum
	 * @return
	 * @throws Exception
	 */
	public QOpenIDs getFollowingFollowerList(String _url,String _name,int _pageIdx,int _reqNum)throws Exception{
		
		m_parameters.clear();
		m_parameters.add(new QParameter("mode", "1"));
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("reqnum", Integer.toString(_reqNum)));
		m_parameters.add(new QParameter("startindex", Integer.toString(_pageIdx)));
		
		if(_name != null && _name.length() != 0){
			m_parameters.add(new QParameter("name", _name));
		}
		
		return new QOpenIDs(new JSONObject(m_request.syncRequest(_url, "GET", 
									m_oauthKey, m_parameters, null)));
	}
	
	
	/**
	 *  publish a message 
	 * @param _text		message
	 * @return				return published message id if successfully
	 * @throws Exception
	 */
	public long publishMsg(String _text)throws Exception{
		return publishMsg_impl(_text,-1,-1,-1,-1,-1,null,null);
	}
	public long publishMsg(String _text,byte[] _fileBuffer,String _fileType)throws Exception{
		return publishMsg_impl(_text,-1,-1,-1,-1,-1,_fileBuffer,_fileType);
	}
	
	/**
	 *  publish a message with geo information
	 * @param _text				message
	 * @param _longitude
	 * @param _latitude
	 * @return				return published message id if successfully
	 * @throws Exception
	 */
	public long publishMsg(String _text,double _longitude,double _latitude)throws Exception{
		return publishMsg_impl(_text,-1,-1,-1,_longitude,_longitude,null,null);
	}
	
	public long publishMsg(String _text,double _longitude,double _latitude,byte[] _fileBuffer,String _fileType)throws Exception{
		return publishMsg_impl(_text,-1,-1,-1,_longitude,_longitude,_fileBuffer,_fileType);
	}
	
	/**
	 * forward a message by messsage's id
	 * @param _text
	 * @param _forwardId
	 * @return				return published message id if successfully
	 * @throws Exception
	 */
	public long forwardMsg(String _text,long _forwardId)throws Exception{
		return publishMsg_impl(_text,_forwardId,-1,-1,-1,-1,null,null);
	}
	
	/**
	 * forward a message by it's id and with geo information
	 * @param _text
	 * @param _forwardId
	 * @param _longitude
	 * @param _latitude
	 * @return				return published message id if successfully
	 * @throws Exception
	 */
	public long forwardMsg(String _text,long _forwardId,double _longitude,double _latitude)throws Exception{
		return publishMsg_impl(_text,_forwardId,-1,-1,_longitude,_latitude,null,null);
	}
	
	/**
	 * reply a message by it's id 
	 * @param _text
	 * @param _reply
	 * @return				return published message id if successfully
	 * @throws Exception
	 */
	public long replyMsg(String _text,long _reply)throws Exception{
		return publishMsg_impl(_text,-1,_reply,-1,-1,-1,null,null);
	}
	
	/**
	 * replay a message by it's id with geo information
	 * @param _text
	 * @param _reply
	 * @param _longitude
	 * @param _latitude
	 * @return					return published message id if successfully
	 * @throws Exception
	 */
	public long replyMsg(String _text,long _reply,double _longitude,double _latitude)throws Exception{
		return publishMsg_impl(_text,-1,_reply,-1,_longitude,_latitude,null,null);
	}
	
	/**
	 * comment a message 
	 * @param _text
	 * @param _commentId
	 * @return				return published message id if successfully
	 */
	public long commentMsg(String _text,long _commentId)throws Exception{
		return publishMsg_impl(_text,-1,-1,_commentId,-1,-1,null,null);
	}
	
	/**
	 *  comment a message with a geo information
	 * @param _text
	 * @param _commentId
	 * @param _longitude
	 * @param _latitude
	 * @return			return published message id if successfully
	 */
	public long commentMsg(String _text,long _commentId,double _longitude,double _latitude)throws Exception{
		return publishMsg_impl(_text,-1,-1,_commentId,_longitude,_latitude,null,null);
	}
	
	private long publishMsg_impl(String _text,long _forwardId,long _replyId,long _commentId,
			double _longitude,double _latitude,byte[] _fileBuffer,String _fileType)throws Exception{
		
		String t_url = fsm_publishURL;
		
		m_parameters.clear();
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("content",_text));
		m_parameters.add(new QParameter("clientip", "127.0.0.1"));
		
		if(_forwardId != -1){
			
			t_url = fsm_publishForwardURL;
			m_parameters.add(new QParameter("reid", Long.toString(_forwardId)));
			
		}else if(_replyId != -1){
			
			t_url = fsm_publishReplyURL;
			m_parameters.add(new QParameter("reid", Long.toString(_replyId)));
			
		}else if(_commentId != -1){
			
			t_url = fsm_publishCommentURL;
			m_parameters.add(new QParameter("reid", Long.toString(_commentId)));
		}
		
		if(_longitude != -1 && _latitude != -1 ){
			m_parameters.add(new QParameter("jing", Double.toString(_longitude)));
			m_parameters.add(new QParameter("wei", Double.toString(_latitude)));
		}
		
		JSONObject t_ret = null;
		
		if(_fileBuffer != null && _fileType != null && !_fileType.isEmpty()){
			t_url = fsm_publishFileURL;
			t_ret = new JSONObject(m_request.syncRequest(t_url,m_oauthKey,m_parameters,_fileBuffer,_fileType));
		}else{
			t_ret = new JSONObject(m_request.syncRequest(t_url, "POST", m_oauthKey, m_parameters, null));
		}	
		
		if(t_ret.getInt("ret") != 0){
			throw new Exception("publish message failed:" + t_ret.getString("msg"));
		}
		
		JSONObject t_data = t_ret.getJSONObject("data");
		if(t_data == null || t_data.get("id") == null){
			throw new Exception("publish message failed<data is null>:" + t_ret.toString());
		}
		
		return t_data.getLong("id");
	}
	
	/**
	 * follow a user
	 * @param _name
	 * @throws Exception
	 */
	public void followUser(String _name)throws Exception{
		
		m_parameters.clear();
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("name",_name));
		
		JSONObject t_ret = new JSONObject(m_request.syncRequest(fsm_followUserURL, "POST", m_oauthKey, m_parameters, null));
		
		if(t_ret.getInt("ret") != 0){
			throw new Exception("followUser failed:" + t_ret.getString("msg"));
		}
	}
	
	public void unfollowUser(String _name)throws Exception{
		m_parameters.clear();
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("name",_name));
		
		JSONObject t_ret = new JSONObject(m_request.syncRequest(fsm_unfollowUserURL, "POST", m_oauthKey, m_parameters, null));
		
		if(t_ret.getInt("ret") != 0){
			throw new Exception("unfollowUser failed:" + t_ret.getString("msg"));
		}
	}
	
	/**
	 *  get the max 20 item direct message of inbox
	 * @return	list of direct message
	 * @throws Exception
	 */
	public List<QDirectMessage> getInboxDirectMessage()throws Exception{
		return getInboxDirectMessage(0,20,0);
	}
	
	/**
	 * get the direct message of inbox by time,number and last direct message id
	 * @param _time
	 * @param _num 		20 is max
	 * @param _lastId
	 * @return
	 * @throws Exception
	 */
	public List<QDirectMessage> getInboxDirectMessage(long _time,int _num,long _lastId)throws Exception{
		return getDirectMsgList_impl(_time,_num,_lastId,QDirectMessage.INBOX_TYPE);		
	}
	
	/**
	 * get the max 20 items of direct message of outbox
	 * @return
	 * @throws Exception
	 */
	public List<QDirectMessage> getOutboxDirectMessage()throws Exception{
		return getDirectMsgList_impl(0,20,0,QDirectMessage.OUTBOX_TYPE);
	}
	
	/**
	 * get the direct message of outbox by time,number and last direct message id
	 * @param _time
	 * @param _num 20 is max
	 * @param _lastId
	 * @return
	 * @throws Exception
	 */
	public List<QDirectMessage> getOutboxDirectMessage(long _time,int _num,long _lastId)throws Exception{
		return getDirectMsgList_impl(_time,_num,_lastId,QDirectMessage.OUTBOX_TYPE);
	}
	
	private List<QDirectMessage> getDirectMsgList_impl(long _time,int _num,long _lastId,int _type)throws Exception{
		
		if(_num > 20){
			_num = 20;
		}
		
		String t_url;
		if(_type == QDirectMessage.INBOX_TYPE){
			t_url = fsm_directMessageInboxURL;
		}else{
			t_url = fsm_directMessageOutboxURL;
		}
		
		m_parameters.clear();
		
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("pagetime",Long.toString(_time / 1000)));
		m_parameters.add(new QParameter("pageflag","2"));
		m_parameters.add(new QParameter("reqnum",Integer.toString(_num)));
		m_parameters.add(new QParameter("lastid",Long.toString(_lastId)));
		
		return QDirectMessage.getDMList(new JSONObject(m_request.syncRequest(t_url, "GET", m_oauthKey, m_parameters, null)),_type);
	}
	
	public void deleteMessage(long _id)throws Exception{
		m_parameters.clear();
		
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("id", Long.toString(_id)));
		
		JSONObject t_ret = new JSONObject(m_request.syncRequest(fsm_deleteMessageURL, "POST", m_oauthKey, m_parameters, null));
		
		if(t_ret.getInt("ret") != 0){
			throw new Exception("delete message failed:" + t_ret.getString("msg"));
		}
	}
	
	public void favoriteMessage(long _id)throws Exception{
		m_parameters.clear();
		
		m_parameters.add(new QParameter("format", "json"));
		m_parameters.add(new QParameter("id", Long.toString(_id)));
		
		JSONObject t_ret = new JSONObject(m_request.syncRequest(fsm_favoriteMessageURL, "POST", m_oauthKey, m_parameters, null));
		
		if(t_ret.getInt("ret") != 0){
			throw new Exception("favorite message failed:" + t_ret.getString("msg"));
		}
	}
}
