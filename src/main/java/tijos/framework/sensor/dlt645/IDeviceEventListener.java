package tijos.framework.sensor.dlt645;

/**
 * Event Listener
 *
 * @author TiJOS
 */
public interface IDeviceEventListener {

    /**
     * Data arrived from the remote node
     *
     * @param srcPort  source port
     * @param destPort target port
     * @param srcAddr  remote address
     * @param buff     data buffer arrived
     */
    void onDataArrived(int funCode, int dataTag, byte [] data);


}
