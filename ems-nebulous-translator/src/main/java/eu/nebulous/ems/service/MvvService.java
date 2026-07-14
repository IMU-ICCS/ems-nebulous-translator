/*
 * Copyright (C) 2023-2026 Institute of Communication and Computer Systems (imu.iccs.gr)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v2.0.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * https://www.mozilla.org/en-US/MPL/2.0/
 */

package eu.nebulous.ems.service;

import gr.iccs.imu.ems.control.controller.ControlServiceCoordinator;
import gr.iccs.imu.ems.control.controller.ControlServiceRequestInfo;
import gr.iccs.imu.ems.translate.TranslationContext;
import gr.iccs.imu.ems.translate.mvv.MetricVariableValuesService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MvvService implements MetricVariableValuesService {
	private final ApplicationContext applicationContext;
	private Map<String,Double> values = new LinkedHashMap<>();

	@Override
	public void init() {
		log.info("MvvService: initialized");
	}

	public Map<String,Double> getValues() {
		return new LinkedHashMap<>(values);
	}

	public void setValues(@NonNull Map<String,Double> values) {
		this.values.clear();
		this.values.putAll(values);
		setControlServiceConstants(this.values);
	}

	@Scheduled(fixedRate = 60, timeUnit = TimeUnit.SECONDS)
	public void printValues() {
		Map<String, Double> vals = getValues();
		if (vals==null || vals.isEmpty())
			log.debug("MvvService: Curr. Values: ---");
		else
			log.debug("MvvService: Curr. Values: {}", vals);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public void translateAndSetValues(Map<String,Object> varValues) {
		// Log new values
		log.info("MvvService.translateAndSetValues: New Variable Values: {}", varValues);
		setControlServiceConstants(varValues.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, y->(double)y.getValue()
		)));
	}

	private void setControlServiceConstants(@NonNull Map<String, Double> newValues) {
		this.values = newValues;
		ControlServiceCoordinator controlServiceCoordinator =
				applicationContext.getBean(ControlServiceCoordinator.class);
		controlServiceCoordinator.setConstants(newValues, ControlServiceRequestInfo.EMPTY);
	}

	@Override
	public Map<String, Double> getMatchingMetricVariableValues(String cpModelPath, TranslationContext _TC) {
		return getMetricVariableValues(cpModelPath, _TC.getMvvCP().keySet());
	}

	@Override
	public Map<String, Double> getMetricVariableValues(String cpModelPath, Set<String> variableNames) {
		Map<String, Double> map = new HashMap<>(values);
		if (variableNames==null || variableNames.isEmpty()) return map;
		map.keySet().retainAll(variableNames);
		return map;
	}
}