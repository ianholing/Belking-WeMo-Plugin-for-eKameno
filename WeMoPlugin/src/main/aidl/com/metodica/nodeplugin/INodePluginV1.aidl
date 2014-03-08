package com.metodica.nodeplugin;

interface INodePluginV1 {
	void initiate();
    boolean run(String command, String data1, String data2, String data3, String actionID, int flagsInUse);
    int getStatusFlag();
    boolean is_compatible();
    String getResource();
    String getPluginName();
    String getPluginShowName();
    String getXMLDefaults();
    String getXMLCustomOptions();
}