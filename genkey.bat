@echo off
echo ------------yuchberry ��ʾ------------
echo ��ʹ��ǰ��ϸ�Ķ� 
echo.
echo           http://code.google.com/p/yuchberry/wiki/SSL_yuchberry 
echo. 
echo Ū���SSL��ʽ��yuchberry֮����������Կ
echo.
echo ������� keytool�����ڲ����� ֮���������ʾ������JRE��װĿ¼������ң����磺
echo.
echo           C:\Program Files\Java\jre6\bin
echo.
echo ������������ļ����������Ŀ¼�����У�Ȼ���ٰ����ɵ���Կ YuchBerrySvr.key ���ƻ�������������PATH�����������������õ�������������



if exist YuchBerrySvr.key del YuchBerrySvr.key

echo ------------����������Կ��------------
keytool -genkey -alias serverkey -keystore YuchBerrySvr.key -validity 3650

pause