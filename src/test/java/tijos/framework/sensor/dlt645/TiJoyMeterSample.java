package tijos.framework.sensor.dlt645;

import java.io.IOException;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.joymeter.IJoyMeterEventListener;
import tijos.framework.sensor.joymeter.TiJoyMeter;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;

public class TiJoyMeterSample implements IJoyMeterEventListener {

	TiJoyMeter jmeter = null;

	byte[] operator = new byte[] { 0x00, 0x00, 0x00, 0x01 };  
	byte[] password = new byte[] { 0x00, 0x00, 0x00, 0x02 };

	public TiJoyMeterSample(TiUART uart) throws IOException {
		jmeter = new TiJoyMeter(uart);
	}

	public void init() throws IOException {
		byte[] address = jmeter.getMeterAddress();
		System.out.println("address " + Formatter.toHexString(address));

		jmeter.setEventListner(this);
		jmeter.start();  

	}

	public void loop() throws IOException {
		jmeter.readMeterRequet();
	}

	public void switchOn() throws IOException {
		jmeter.switchOn(password, operator);
	}

	public void switchOff() throws IOException {
		jmeter.switchOff(password, operator);
	}

	@Override
	public void onMeterDataArrived(TiJoyMeter meter) {
		System.out.println(meter);

	}

	@Override
	public void onAlarmOverCurrent(long alarmTime, double current) {
		System.out.println("onAlarmOverCurrent " + alarmTime + " current " + current);

	}

	@Override
	public void onAlarmOverPower(long alarmTime, double power) {
		System.out.println("onAlarmOverPower " + alarmTime + " power " + power);

	}

	@Override
	public void onAlarmOverVoltage(long alarmTime, double voltage) {
		System.out.println("onAlarmOverVoltage " + alarmTime + " voltage " + voltage);

	}

	@Override
	public void onAlarmUnderVoltage(long alarmTime, double voltage) {
		System.out.println("onAlarmUnderVoltage " + alarmTime + " voltage " + voltage);

	}

	@Override
	public void onAlarmLowPower(long alarmTime, double remaining) {
		System.out.println("onAlarmLowPower " + alarmTime + " remaining " + remaining);

	}

	@Override
	public void onAlarmOverDraft(long alarmTime, double remaining) {
		System.out.println("onAlarmOverDraft " + alarmTime + " remaining " + remaining);

	}

	@Override
	public void onAlarmDeviceError(int devError) {
		System.out.println("onAlarmDeviceError " + devError);

	}

	@Override
	public void onValveStateChange(int valveState) {
		System.out.println("onValveStateChange " + valveState);

	}

	@Override
	public void onTempPowerSupplyCountUpdate(int count) {
		System.out.println("onTempPowerSupplyCountUpdate " + count);

	}

	public static void main(String[] args) {
		System.out.println("Hello World!");

		try {
			TiUART uart = TiUART.open(5);
			uart.setWorkParameters(8, 1, TiUART.PARITY_EVEN, 2400);

			TiJoyMeterSample sample = new TiJoyMeterSample(uart);

			sample.init();

			int count = 0;
			while (true) {
				sample.loop();
				Delay.msDelay(5000);

				if (count++ == 5) {
					sample.switchOn();
				}

				if (count == 10) {
					sample.switchOff();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}
