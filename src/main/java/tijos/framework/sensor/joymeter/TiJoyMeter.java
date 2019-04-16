package tijos.framework.sensor.joymeter;

import java.io.IOException;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.dlt645.IDeviceEventListener;
import tijos.framework.sensor.dlt645.TiDLT645;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;
import tijos.framework.util.LittleBitConverter;

public class TiJoyMeter implements IDeviceEventListener {

	private TiDLT645 dlt645;

	private static final int JOYMETER_TAG_CURRENT_DATA = 0x001D0000; //

	/**
	 * 计量时间（格林尼治）（hex）
	 */
	long measureTime = 0;

	/**
	 * 剩余电量（kWh）
	 */
	double remaining;

	/**
	 * 历史电量(kWh)
	 */
	double hisotry;

	/**
	 * 当前电流(A)
	 */
	double current;

	/**
	 * 当前电压(V)
	 */
	double voltage;

	/**
	 * 当前功率(W)
	 */
	double power;

	/**
	 * 功率因数
	 */
	double powerfactor;

	/**
	 * 开合闸状态 0合闸 1断闸
	 */
	int switchStatus;
	
	
	IJoyMeterEventListener evtListener;

	public TiJoyMeter(TiUART uart) {
		dlt645 = new TiDLT645(uart);
		// dlt645.start();
	}

	public void start() {
		dlt645.setEventListener(this);
		dlt645.start();
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] getMeterAddress() throws IOException {
		return dlt645.readMeterAddress();
	}

	/**
	 * 
	 * @throws IOException
	 */
	public void readMeter() throws IOException {
		byte[] meterData = dlt645.readMeterData(JOYMETER_TAG_CURRENT_DATA);
		if (meterData.length < 27)
			throw new IOException("Invalid data length");

		this.measureTime = LittleBitConverter.ToUInt32(meterData, 0);
		this.remaining = dlt645.BCD2Double(meterData, 4, 4, 2);
		this.hisotry = dlt645.BCD2Double(meterData, 8, 4, 2);
		this.current = dlt645.BCD2Double(meterData, 12, 4, 3);
		this.voltage = dlt645.BCD2Double(meterData, 16, 4, 2);
		this.power = dlt645.BCD2Double(meterData, 20, 4, 2);
		this.powerfactor = dlt645.BCD2Double(meterData, 24, 2, 3);

		this.switchStatus = meterData[26];
	}

	public void readMeterRequet() throws IOException {
		dlt645.sendMeterReadingRequest(JOYMETER_TAG_CURRENT_DATA);
	}

	@Override
	public void onDataArrived(int funCode, byte[] tag, byte[] data) {

		System.out.println(
				"funCode " + funCode + " tag " + Formatter.toHexString(tag) + " data " + Formatter.toHexString(data));

		
		try {
			if(tag[3] == 0x00 && tag[2] == 0x1d) {
				parseMeterData(tag, data);				
			}
			else if(tag[3] == 0x08) {
				parseAlarmData(tag, data);
			}
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "time " + this.measureTime + " left " + this.remaining + " history " + this.hisotry + " current "
				+ this.current + " voltage " + this.voltage + " power " + this.power + " powerfactor "
				+ this.powerfactor + " switch " + this.switchStatus;
	}

	private void parseMeterData(byte []tag, byte[] meterData) throws IOException {

		if (meterData.length < 27)
			throw new IOException("Invalid data length");

		this.measureTime = LittleBitConverter.ToUInt32(meterData, 0);
		this.remaining = dlt645.BCD2Double(meterData, 4, 4, 2);
		this.hisotry = dlt645.BCD2Double(meterData, 8, 4, 2);
		this.current = dlt645.BCD2Double(meterData, 12, 4, 3);
		this.voltage = dlt645.BCD2Double(meterData, 16, 4, 2);
		this.power = dlt645.BCD2Double(meterData, 20, 4, 2);
		this.powerfactor = dlt645.BCD2Double(meterData, 24, 2, 3);

		this.switchStatus = meterData[26];
		
		evtListener.onMeterDataArrived(this);
	}
	
	private void parseAlarmData(byte [] tag,  byte [] alarmData) throws IOException {
		long alarmTime = LittleBitConverter.ToUInt32(alarmData, 0);
		
		switch(tag[3])
		{
		case 0x66:
			double overcurrent = dlt645.BCD2Double(alarmData,4, 4, 3);
			this.evtListener.onAlarmOverCurrent(alarmTime, overcurrent);
			break;
		case 0x67:
			double overpower = dlt645.BCD2Double(alarmData,4, 4, 2);
			this.evtListener.onAlarmOverPower(alarmTime, overpower);
			break;
		case 0x68:			
			double overvoltage = dlt645.BCD2Double(alarmData,4, 4, 2);
			this.evtListener.onAlarmOverVoltage(alarmTime, overvoltage);
			break;
		case 0x69:
			double undervoltage = dlt645.BCD2Double(alarmData,4, 4, 2);
			this.evtListener.onAlarmUnderVoltage(alarmTime, undervoltage);
			break;
		case 0x6A:
			double remaining = dlt645.BCD2Double(alarmData,4,4,2);
			this.evtListener.onAlarmLowPower(alarmTime, remaining);
			break;
		case 0x6B:
			double remaining2 = dlt645.BCD2Double(alarmData,4,4,2);
			this.evtListener.onAlarmOverDraft(alarmTime, remaining2);
			break;
		case 0x6C:
			int devError = (int) LittleBitConverter.ToUInt32(alarmData, 4);
			this.evtListener.onAlarmDeviceError(devError);
			break;
		case 0x6D:
			int valveState = alarmData[0];
			this.evtListener.onValveStateChange(valveState);
			break;
		case 0x6F:
			int tmpCount = alarmData[0];
			this.evtListener.onTempPowerSupplyCountUpdate(tmpCount);
			break;
		}
		
		
	}

	public static void main(String[] args) {
		System.out.println("Hello World!");

		TiUART uart;
		try {
			uart = TiUART.open(5);
			uart.setWorkParameters(8, 1, TiUART.PARITY_EVEN, 2400);

			TiJoyMeter meter = new TiJoyMeter(uart);

			meter.getMeterAddress();
			// meter.readMeter();

			System.out.println(meter);

			meter.start();
			meter.readMeterRequet();

			while (true) {
				Delay.msDelay(1000);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
