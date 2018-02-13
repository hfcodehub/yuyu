/**
 *  Dear developer:
 *  
 *   If you want to modify this file of project and re-publish this please visit:
 *  
 *     http://code.google.com/p/yuchberry/wiki/Project_files_header
 *     
 *   to check your responsibility and my humble proposal. Thanks!
 *   
 *  -- 
 *  Yuchs' Developer    
 *  
 *  
 *  
 *  
 *  尊敬的开发者：
 *   
 *    如果你想要修改这个项目中的文件，同时重新发布项目程序，请访问一下：
 *    
 *      http://code.google.com/p/yuchberry/wiki/Project_files_header
 *      
 *    了解你的责任，还有我卑微的建议。 谢谢！
 *   
 *  -- 
 *  语盒开发者
 *  
 */
package com.yuchting.yuchberry.server;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.dom4j.Element;

import weibo4j.IDs;

import com.mime.qweibo.QDirectMessage;
import com.mime.qweibo.QOpenIDs;
import com.mime.qweibo.QUser;
import com.mime.qweibo.QWeibo;
import com.mime.qweibo.QWeiboSyncApi;

public class fetchQWeibo extends fetchAbsWeibo{
	
	final static String		fsm_consumerKey 	= getQQConsumerKey();
	final static String		fsm_consumerSecret	= getQQSecretKey();
		
	
	public QWeiboSyncApi m_api = new QWeiboSyncApi();
		
	QUser		m_userself = null;
	
	// following and followers list
	// Set<QQ OpenID>
	//
	Set<String>					m_followingList				= new HashSet<String>();
	Set<String>					m_followerList				= new HashSet<String>();
	long						m_refreshFollowingListTimer = 0;
	
	public fetchQWeibo(fetchMgr _mainMgr){
		super(_mainMgr);
		m_api.setCostomerKey(fsm_consumerKey, fsm_consumerSecret);
	}
	
	public void InitAccount(Element _elem)throws Exception{
		super.InitAccount(_elem);
		
		m_accountName = m_accountName + "[QQWeibo]";
		
		m_headImageDir			= m_headImageDir + "QQ/";
		File t_file = new File(m_headImageDir);
		if(!t_file.exists()){
			t_file.mkdir();
		}
		
		// if sync his/her account will refresh following and follower list
		//
		m_refreshFollowingListTimer = 0;
	}
	
	protected int GetCurrWeiboStyle(){
		return fetchWeibo.QQ_WEIBO_STYLE;
	}

	protected void CheckTimeline()throws Exception{
		List<QWeibo> t_fetch = null;
		if(m_timeline.m_fromIndex > 1){
			t_fetch = m_api.getHomeList(m_timeline.m_fromIndex, 20);
		}else{
			t_fetch = m_api.getHomeList();
		}		 
		
		AddWeibo(t_fetch,m_timeline,fetchWeibo.TIMELINE_CLASS);
	}
	
	protected void CheckDirectMessage()throws Exception{
		List<QDirectMessage> t_fetch = null;
		
		if(m_directMessage.m_fromIndex > 1){
			t_fetch = m_api.getOutboxDirectMessage(m_directMessage.m_fromIndex2,10,m_directMessage.m_fromIndex);
		}else{
			t_fetch = m_api.getOutboxDirectMessage();
		}
		
		AddDirectMsgWeibo(t_fetch,m_directMessage,fetchWeibo.DIRECT_MESSAGE_CLASS,true);
		
		if(!t_fetch.isEmpty()){
			m_directMessage.m_fromIndex = t_fetch.get(0).getWeiboContentItem().getId();
			m_directMessage.m_fromIndex2= t_fetch.get(0).getWeiboContentItem().getTime();
		}
		
		// inbox
		//
		if(m_directMessage.m_fromIndex2 > 1){
			t_fetch = m_api.getInboxDirectMessage(m_directMessage.m_fromIndex4,10,m_directMessage.m_fromIndex3);
		}else{
			t_fetch = m_api.getInboxDirectMessage();
		}
		
		AddDirectMsgWeibo(t_fetch,m_directMessage,fetchWeibo.DIRECT_MESSAGE_CLASS,false);
		
		if(!t_fetch.isEmpty()){
			m_directMessage.m_fromIndex3 = t_fetch.get(0).getWeiboContentItem().getId();
			m_directMessage.m_fromIndex4= t_fetch.get(0).getWeiboContentItem().getTime();
		}
						
		// sort by time
		//
		Collections.sort(m_directMessage.m_weiboList,new Comparator<fetchWeibo>() {
			
			public   int   compare(fetchWeibo o1, fetchWeibo o2){
				
				long a = o1.GetDateTime();
				long b = o2.GetDateTime();
				
				if(a < b){
					return 1;
				}else if(a > b){
					return -1;
				}else{
					return 0;
				}
			}
		});
		
	}
	
	private void AddDirectMsgWeibo(List<QDirectMessage> _from,fetchWeiboData _to,byte _class,boolean _outOrInBox){
		
		boolean t_insert;
		long t_compareId = _outOrInBox?_to.m_fromIndex2:_to.m_fromIndex4;
		
		for(QDirectMessage fetchOne : _from){

			if((_outOrInBox && fetchOne.getDirectMessageType() == QDirectMessage.OUTBOX_TYPE)
			|| (!_outOrInBox && fetchOne.getDirectMessageType() == QDirectMessage.INBOX_TYPE)){

				if(t_compareId >= fetchOne.getWeiboContentItem().getTime()){
					continue;
				}
			}
			
			t_insert = true;
			
			for(fetchWeibo weibo : _to.m_weiboList){
				if(weibo.GetId() == fetchOne.getWeiboContentItem().getId()){
					t_insert = false;
					break;
				}
			}
			
			if(t_insert){
				for(fetchWeibo weibo : _to.m_WeiboComfirm){
					if(weibo.GetId() == fetchOne.getWeiboContentItem().getId()){
						t_insert = false;
						break;
					}
				}
			}
			
			if(t_insert){
				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_weibo, fetchOne.getWeiboContentItem(),_class);
				
				t_weibo.SetOwnWeibo(fetchOne.getDirectMessageType() == QDirectMessage.OUTBOX_TYPE);
				
				if(t_weibo.IsOwnWeibo()){
					t_weibo.setReplyName(fetchOne.getSendToScreenName());
				}else{
					t_weibo.setReplyName(m_userself.getScreenName());
				}				
				
				_to.m_weiboList.add(t_weibo);
			}
		}
	}
	
	protected void CheckAtMeMessage()throws Exception{
		
		List<QWeibo> t_fetch = null;
		if(m_atMeMessage.m_fromIndex > 1){
			t_fetch = m_api.getMentionList(m_atMeMessage.m_fromIndex, 20);
		}else{
			t_fetch = m_api.getMentionList();
		}		 
		
		AddWeibo(t_fetch,m_atMeMessage,fetchWeibo.AT_ME_CLASS);
	}
	
	protected void CheckCommentMeMessage()throws Exception{
		// nothing about this list
	}

	protected void DeleteWeibo(long _id,boolean _isComment)throws Exception{
		m_api.deleteMessage(_id);
	}
	
	protected void setFriendRemark(String _id,String _remark)throws Exception{}
	
	/**
	 * overload to refresh following and follower list interval
	 */
	public synchronized void CheckFolder()throws Exception{
		super.CheckFolder();
		
		try{
			refreshFollowingFollowerList(false);
		}catch(Exception e){
			m_mainMgr.m_logger.LogOut(GetAccountName() + " refreshFollowingFollowerList Error:"+ e.getMessage());
			// sleep for a while
			//
			Thread.sleep(2000);
		}
	}
	
	
	/**
	 * reset the session for connection
	 * 
	 * @param _fullTest		: whether test the full configure( SMTP for email)
	 */
	public void ResetSession(boolean _fullTest)throws Exception{
		
		m_userself = m_api.verifyCredentials();
		
		ResetCheckFolderLimit();
		
		if(m_followerList.isEmpty() && m_followingList.isEmpty()){
			refreshFollowingFollowerList(true);
		}
		
		m_mainMgr.m_logger.LogOut("Weibo Account<" + GetAccountName() + "> Prepare OK!");
		
	}
	
	@Override
	public void setAccessTokens(String _access,String _secret){
		m_api.setAccessToken(_access, _secret);
	}
	
	/**
	 * request the following and follower list max page number
	 */
	static final int MaxPageNum = 200;
	
	/**
	 * refresh the following and followers list
	 * @throws Exception
	 */
	private void refreshFollowingFollowerList(boolean _ignoreTimer)throws Exception{
		
		if(!_ignoreTimer){
			// refresh interval judge
			//
			if(System.currentTimeMillis() - m_refreshFollowingListTimer < 24 * 3600000){
				return;
			}
		}
		
		// clear the list first
		//
		m_followingList.clear();
		m_followerList.clear();
		
		// get the friends ids
		//
		if(m_userself.getFollowingNum() != 0){	
			int t_pageIdx = 0;
			while(true){
				QOpenIDs t_ids = m_api.getOwnFollowingList(t_pageIdx,200);
								
				for(String id:t_ids.getOpenidList()){
					m_followingList.add(id);
				}
				
				if(t_ids.getOpenidList().length < MaxPageNum){
					break;
				}
				
				t_pageIdx++;
			}
		}
		
		// get the followers ids
		//
		if(m_userself.getFollowerNum() != 0){
			
			int t_pageIdx = 0;
			while(true){
				
				QOpenIDs t_ids = m_api.getOwnFollowerList(t_pageIdx,MaxPageNum);
				
				for(String id:t_ids.getOpenidList()){
					m_followerList.add(id);
				}
				
				if(t_ids.getOpenidList().length < MaxPageNum){
					break;
				}
				
				t_pageIdx++;
			}
		}
		
		m_refreshFollowingListTimer = System.currentTimeMillis();
	}
	
	private boolean isUserFollowing(String _openid){
		return m_followingList.contains(_openid);
	}
	
	private boolean isUserFollower(String _openid){
		return m_followerList.contains(_openid);
	}
	
	protected void ResetCheckFolderLimit()throws Exception{
		m_currRemainCheckFolderNum = 100;
		m_maxCheckFolderNum			= 100;
	}
	
	/**
	 * destroy the session connection
	 */
	public void DestroySession(){}	
	
	public void UpdateStatus(String _text,GPSInfo _info,byte[] _filePic,String _fileType)throws Exception{
		if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
			if(_filePic != null && _fileType != null){
				m_api.publishMsg(_text, _info.m_longitude, _info.m_latitude,_filePic,_fileType);
			}else{
				m_api.publishMsg(_text, _info.m_longitude, _info.m_latitude);
			}
			
		}else{
			if(_filePic != null && _fileType != null){
				m_api.publishMsg(_text,_filePic,_fileType);
			}else{
				m_api.publishMsg(_text);
			}
			
		}
	}
	
	protected void UpdateComment(int _style,String _text,long _orgWeiboId,
						GPSInfo _info,int _updateStyle)throws Exception{
		
		if(_style == GetCurrWeiboStyle()){
			
			if(_updateStyle != 2){ // don't commnet this message

				if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
					m_api.commentMsg(_text, _orgWeiboId, _info.m_longitude, _info.m_latitude);
				}else{
					m_api.commentMsg(_text, _orgWeiboId);
				}
			}
			
			if(_updateStyle == 1){ // need publish message in timeline

				if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
					m_api.publishMsg(_text, _info.m_longitude, _info.m_latitude);
				}else{
					m_api.publishMsg(_text);
				}
				
			}else if(_updateStyle == 2){
				
				if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
					m_api.forwardMsg(_text,_orgWeiboId, _info.m_longitude, _info.m_latitude);
				}else{
					m_api.forwardMsg(_text,_orgWeiboId);
				}
			}
			
		}else{
					
			if(_info != null && _info.m_latitude != 0 && _info.m_longitude != 0){
				m_api.publishMsg(_text, _info.m_longitude, _info.m_latitude);
			}else{
				m_api.publishMsg(_text);
			}
			
		}			
	}
	
	protected void UpdateReply(String _text,long _commentWeiboId,long _orgWeiboId,
									GPSInfo _info,boolean _updateTimeline)throws Exception{
		
		UpdateComment(GetCurrWeiboStyle(),_text,_orgWeiboId,_info,_updateTimeline?1:0);
	}
	
	protected void FavoriteWeibo(long _id)throws Exception{
		m_api.favoriteMessage(_id);
	}

	public void FollowUser(String _screenName)throws Exception{
		m_api.followUser(_screenName);
	}
	
	public void FollowUser(long _id)throws Exception{
		// nothing
	}
	
	protected void UnfollowUser(String _screenName)throws Exception{
		m_api.unfollowUser(_screenName);
	}
	
	protected fetchWeiboUser getWeiboUser(String _name)throws Exception{
		QUser t_user = m_api.getUserInfo(_name);
		
		fetchWeiboUser t_weibo = new fetchWeiboUser(m_mainMgr.m_convertToSimpleChar);
		
		t_weibo.setStyle(fetchWeibo.QQ_WEIBO_STYLE);
		t_weibo.setId(0);
		
		t_weibo.setName(t_user.getNickName());
		t_weibo.setScreenName(t_user.getScreenName());
		t_weibo.setHeadImage(DownloadHeadImage(new URL(t_user.getHeadImageURL()),t_user.getScreenName()));
		t_weibo.setDesc(t_user.getIntroduction());
		t_weibo.setCity(t_user.getLocation());
		t_weibo.setVerified(t_user.isVerified());
		t_weibo.setHasBeenFollowed(t_user.hasBeenFollowed());
		t_weibo.setIsMyFans(t_user.isMyFans());
		
		t_weibo.setFollowNum(t_user.getFollowingNum());
		t_weibo.setFansNum(t_user.getFollowerNum());
		t_weibo.setWeiboNum(t_user.getWeiboNum());
		
		List<QWeibo> t_list = m_api.getUserTimeline(_name);
		for(QWeibo s:t_list){
			fetchWeibo weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
			ImportWeibo(weibo, s,fetchWeibo.TIMELINE_CLASS);
			
			t_weibo.getUpdatedWeibo().insertElementAt(weibo, 0);
		}
		
		return t_weibo;
	}
	
	protected void sendDirectMsg(String _screenName,String _text)throws Exception{
		m_api.sendDirectMessage(_screenName,_text,null,-1,-1);
	}
	
	protected void addWeiboAccountList(Vector<WeiboAccount> _accList){
		if(m_userself != null){
			WeiboAccount acc = new WeiboAccount();
			
			acc.name 		= m_userself.getNickName();
			acc.id			= m_userself.getId();
			acc.weiboStyle	= (byte)GetCurrWeiboStyle();
			acc.needUpdate	= true;
			
			_accList.add(acc);
		}
	}
	
	protected long getCurrAccountId(){
		if(m_userself != null){
			return m_userself.getId();
		}else{
			return 0;
		}
	}
	
	private void AddWeibo(List<QWeibo> _from,fetchWeiboData _to,byte _class){
		
		boolean t_insert;
		
		for(QWeibo fetchOne : _from){
			
			if(_to.m_fromIndex >= fetchOne.getTime()){
				// directly return because of qq time fetching bug!
				//
				break;
			}
			
			if(_to.m_weiboList.size() >= _to.m_sum){
				break;
			}
			
			t_insert = true;
			for(fetchWeibo weibo : _to.m_weiboList){
				if(weibo.GetId() == fetchOne.getId()){
					t_insert = false;
					break;
				}
			}
			
			if(t_insert){
				for(fetchWeibo weibo : _to.m_WeiboComfirm){
					if(weibo.GetId() == fetchOne.getId()){
						t_insert = false;
						break;
					}
				}
			}
			
			if(t_insert){
				fetchWeibo t_weibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
				ImportWeibo(t_weibo, fetchOne,_class);
				
				_to.m_weiboList.add(t_weibo);
			}
		}
		
		if(!_from.isEmpty()){
			QWeibo t_lashOne = _from.get(0);
			_to.m_fromIndex = t_lashOne.getTime() + 1000;
		}
	}
	
	private void ImportWeibo(fetchWeibo _weibo,QWeibo _qweibo,byte _weiboClass){
		
		_weibo.SetId(_qweibo.getId());
		_weibo.SetDateLong(_qweibo.getTime());
		_weibo.SetText(_qweibo.getText());
		_weibo.SetSource(_qweibo.getSource());
		
		_weibo.SetWeiboStyle(fetchWeibo.QQ_WEIBO_STYLE);
		_weibo.SetWeiboClass(_weiboClass);
		
		_weibo.SetOwnWeibo(_qweibo.isOwnWeibo());
		
		_weibo.SetUserName(_qweibo.getNickName());
		_weibo.SetUserScreenName(_qweibo.getScreenName());
		
		_weibo.SetSinaVIP(_qweibo.isVIP());
		
		if(!_qweibo.isOwnWeibo()){
			_weibo.setUserFollowMe(isUserFollower(_qweibo.getUserOpenid()));
			_weibo.setUserFollowing(isUserFollowing(_qweibo.getUserOpenid()));
		}		
		
		if(_qweibo.getImage() != null){
			_weibo.SetOriginalPic(_qweibo.getImage());
		}		
		
		if(_qweibo.getHeadImageURL().length() != 0){
			try{
				_weibo.SetUserHeadImageHashCode(StoreHeadImage(new URL(_qweibo.getHeadImageURL()),_qweibo.getScreenName()));
			}catch(Exception e){
				m_mainMgr.m_logger.LogOut(GetAccountName() + " Exception:" + e.getMessage());
				m_mainMgr.m_logger.PrinterException(e);
			}				
		}
		
		if(_qweibo.getSourceWeibo() != null){
			
			fetchWeibo t_sourceWeibo = new fetchWeibo(m_mainMgr.m_convertToSimpleChar);
			ImportWeibo(t_sourceWeibo,_qweibo.getSourceWeibo(),_weiboClass);
			
			if(_qweibo.getType() == 4){
				_weibo.SetReplyWeiboId(t_sourceWeibo.GetId());
				_weibo.SetReplyWeibo(t_sourceWeibo);
				_weibo.setReplyName(t_sourceWeibo.GetUserScreenName());
			}else{
				_weibo.SetCommectWeiboId(t_sourceWeibo.GetId());
				_weibo.SetCommectWeibo(t_sourceWeibo);				
			}						
		}		
		
	}
	
	public static void main(String[] _arg)throws Exception{
		
		fetchMgr t_mgr = new fetchMgr();
		Logger t_logger = new Logger("");
		t_mgr.m_logger = t_logger;
		
		fetchQWeibo t_weibo = new fetchQWeibo(t_mgr);
		
		QWeiboSyncApi.sm_debug = true;
		
		t_weibo.m_api.setCostomerKey(fsm_consumerKey, fsm_consumerSecret);
		t_weibo.m_accessToken = "d7804a95bde0436ca9f0b62ef84de787";
		t_weibo.m_secretToken = "0a076b55a89ed75bada75b72d880a60b";
				
		t_weibo.ResetSession(true);
		
		t_weibo.m_atMeMessage.m_sum = 10;
		t_weibo.m_timeline.m_sum = 10;
		t_weibo.m_directMessage.m_sum = 5;
		
		
//		File t_file = new File("logo.png");
//		FileInputStream t_fileIn = new FileInputStream(t_file);
//		byte[] t_fileBuffer = new byte[(int)t_file.length()];
//		
//		sendReceive.ForceReadByte(t_fileIn, t_fileBuffer, t_fileBuffer.length);
//		
//		t_weibo.UpdateStatus("image again", null, t_fileBuffer, "image/png");	
//				
//		System.out.print(t_weibo);
			
		List<QWeibo> t_list = t_weibo.m_api.getHomeList(1307720032001L,20);
		
		fetchWeibo t_wb = new fetchWeibo(false);
		
		for(QWeibo weibo : t_list){
			
			t_weibo.ImportWeibo(t_wb,weibo,fetchWeibo.TIMELINE_CLASS);
			
			if(t_wb.isUserFollowing() && t_wb.isUserFollowMe()){
				System.out.print("互粉\t");
			}
			System.out.println(Long.toString(weibo.getId()) + " @" + weibo.getScreenName() + "("+weibo.getNickName()+") " + weibo.getTime() +":"+ weibo.getText());
			
			if(weibo.getSourceWeibo() != null){
				weibo = weibo.getSourceWeibo();
				System.out.print("\t\tsource:");
				System.out.print(Long.toString(weibo.getId()) + " @" + weibo.getScreenName() + "("+weibo.getNickName()+") :"+ weibo.getText());
			}
		}
		
//		List<QDirectMessage> t_dmInboxlist =  t_weibo.m_api.getInboxDirectMessage();
//		
//		for(QDirectMessage dm : t_dmInboxlist){
//			QWeibo weibo = dm.getWeiboContentItem();
//			
//			System.out.println(Long.toString(weibo.getId()) + " @" + weibo.getName() + "("+weibo.getNickName()+") :"+ weibo.getText());
//			
//			if(weibo.getSourceWeibo() != null){
//				weibo = weibo.getSourceWeibo();
//				System.out.print("\t\tsource:");
//				System.out.print(Long.toString(weibo.getId()) + " @" + weibo.getName() + "("+weibo.getNickName()+") :"+ weibo.getText());
//			}
//			
//		}
		
		//t_weibo.m_api.replyMsg("reply it",81005049499103L);
	
	}
	
	
	

}
