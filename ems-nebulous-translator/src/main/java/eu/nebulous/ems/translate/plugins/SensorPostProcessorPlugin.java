package eu.nebulous.ems.translate.plugins;

import gr.iccs.imu.ems.api.Plugin;
import gr.iccs.imu.ems.translate.model.Sensor;

import java.util.List;
import java.util.Map;

/**
 * Sensor Post-Processor plugin (used ONLY in EMS-Nebulous)
 */
public interface SensorPostProcessorPlugin extends Plugin {
    List<String> getSupportedTypes();
    void postProcessSensor(Sensor sensor, String sensorType, Map<String,Object> sensorSpec);
}
