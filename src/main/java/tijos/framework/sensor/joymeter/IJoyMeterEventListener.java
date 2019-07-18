package tijos.framework.sensor.joymeter;

/**
 * Event listener for Meter
 * @author lemon
 *
 */
public interface IJoyMeterEventListener {

	/**
	 * Data arrived and could be access from TiJoyMeter
	 * @param meter
	 */
	void onMeterDataArrived(TiJoyMeter meter);
	
	
	void onAlarmOverCurrent(long alarmTime, double current);
	
	void onAlarmOverPower(long alarmTime, double power);
	
	void onAlarmOverVoltage(long alarmTime, double voltage);
	
	void onAlarmUnderVoltage(long alarmTime, double voltage);
	
	void onAlarmLowPower(long alarmTime, double remaining);
	
	void onAlarmOverDraft(long alarmTime, double remaining);
	
	void onAlarmDeviceError(int devError);
	
	void onValveStateChange(int valveState);
	
	void onTempPowerSupplyCountUpdate(int count);
	
	
}
