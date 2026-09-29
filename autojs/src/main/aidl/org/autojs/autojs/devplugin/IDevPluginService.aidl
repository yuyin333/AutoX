package org.autojs.autojs.devplugin;

interface IDevPluginService {
    // connect to computer
    void connectToComputer(String url);

    // disconnect from computer
    void disconnectFromComputer();

    // get computer connection status
    boolean isComputerConnected();

    // get saved server address
    String getSavedServerAddress();

    // start/stop USB debug
    void startUSBDebug();
    void stopUSBDebug();
    boolean isUSBDebugActive();

}
