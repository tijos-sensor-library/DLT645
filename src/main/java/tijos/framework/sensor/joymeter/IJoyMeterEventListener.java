package tijos.framework.sensor.joymeter;

public interface IJoyMeterEventListener {

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
