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
package com.yuchting.yuchberry.server.frame;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.yuchting.yuchberry.server.cryptPassword;

public class cryptPassTool extends JFrame implements ActionListener{

	JTextField	m_cryptKey		= new JTextField();
	JTextField	m_orgPass		= new JTextField();
	JTextField	m_cryptPass		= new JTextField();
	
	JButton		m_convert		= new JButton("转换");
	JButton		m_help			= new JButton("帮助");
	
	public cryptPassTool(){
		
		setTitle("加密密码工具");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		setResizable(false);
		
		
		Container t_con = getContentPane();
		
		t_con.setLayout(new FlowLayout(FlowLayout.CENTER));
		
		m_cryptPass.setEditable(false);
		
		createDialog.AddTextLabel(t_con,"加密算子:",m_cryptKey,500,"");
		createDialog.AddTextLabel(t_con,"明文密码:",m_orgPass,500,"");
		createDialog.AddTextLabel(t_con,"加密密码:",m_cryptPass,500,"");
		
		t_con.add(m_convert);
		t_con.add(m_help);
		m_convert.addActionListener(this);
		m_help.addActionListener(this);
		
		setLocationRelativeTo(null);
		
		pack();
		setSize(600,170);
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == m_convert){
			if(m_cryptKey.getText().isEmpty()){
				JOptionPane.showMessageDialog(this,"请输入加密算子", "错误", JOptionPane.ERROR_MESSAGE);
				return ;
			}
			
			if(m_orgPass.getText().isEmpty()){
				JOptionPane.showMessageDialog(this,"请输入明文密码", "错误", JOptionPane.ERROR_MESSAGE);
				return ;
			}
			
			try{
				cryptPassword t_crypt = new cryptPassword(cryptPassword.md5(m_cryptKey.getText()));
				m_cryptPass.setText(t_crypt.encrypt(m_orgPass.getText()));
				
			}catch(Exception _e){
				JOptionPane.showMessageDialog(this,_e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
			}
			

		}else if(e.getSource() == m_help){
			try{
				mainFrame.OpenURL("http://code.google.com/p/yuchberry/wiki/Password_Crypt");
			}catch(Exception _e){
				JOptionPane.showMessageDialog(this,_e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
			}
			
		}
	}
	
	static public void main(String _arg[]){
		new cryptPassTool();
	}
	
}
