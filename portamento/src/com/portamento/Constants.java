package com.portamento;

public class Constants {

	public static final int MSG_SURFACESIZECHANGED = 0;
	
	// Array boundary
	public static final int FREQUENCY_MAX = 5000;
	
	// UI boundary
	public static final int FREQUENCY_HIGH = 1047; // High C
	public static final int FREQUENCY_LOW = 131; // Low C
	public static final int FREQUENCY_DELTA = FREQUENCY_HIGH-FREQUENCY_LOW;
	
	public static final int ENV_STANDBY = 0;
	public static final int ENV_ATTACK = 1;
	public static final int ENV_DECAY = 2;
	public static final int ENV_SUSTAIN = 3;
	public static final int ENV_RELEASE = 4;
}
