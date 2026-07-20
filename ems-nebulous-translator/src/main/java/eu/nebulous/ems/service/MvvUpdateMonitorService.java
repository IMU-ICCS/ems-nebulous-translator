/*
 * Copyright (C) 2023-2026 Institute of Communication and Computer Systems (imu.iccs.gr)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v2.0.
 * If a copy of the MPL was not distributed with this file, you can obtain one at
 * https://www.mozilla.org/en-US/MPL/2.0/
 */

package eu.nebulous.ems.service;

import eu.nebulous.ems.translate.NebulousEmsTranslatorProperties;
import gr.iccs.imu.ems.brokercep.BrokerCepService;
import gr.iccs.imu.ems.brokercep.event.EventMap;
import gr.iccs.imu.ems.control.plugin.PostTranslationPlugin;
import gr.iccs.imu.ems.control.util.TopicBeacon;
import gr.iccs.imu.ems.translate.TranslationContext;
import jakarta.jms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MvvUpdateMonitorService implements InitializingBean, PostTranslationPlugin {

	private final NebulousEmsTranslatorProperties properties;
	private final BrokerCepService brokerCepService;
	private final TaskScheduler taskScheduler;
	private final MvvService mvvService;

	private ScheduledFuture<?> initTask;
	private MessageConsumer consumer;

	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("MvvUpdateMonitorService initialized");
	}

	@Override
	public void processTranslationResults(TranslationContext translationContext, TopicBeacon topicBeacon) {
		log.debug("Subscribing to MVV update topics: {}", properties.getMvvUpdateTopic());

		if (StringUtils.isBlank(properties.getMvvUpdateTopic())) {
			log.warn("No MVV update topics configured. MVVs cannot be updated using events.");
			return;
		}

		this.initTask = taskScheduler.scheduleWithFixedDelay(() -> {
					try {
						// Close previous consumer, if any
						if (consumer != null) {
							try {
								consumer.close();
							} catch (JMSException e) {
								log.error("Error while closing existing consumer", e);
							}
						}

						// Create new consumer for MVV update topics
						Session session = brokerCepService.getBrokerCepBridge().getSession();
						Topic destinations = session.createTopic(properties.getMvvUpdateTopic()+"*");
						this.consumer = session.createConsumer(destinations);
						consumer.setMessageListener(this::onMessage);
						log.info("Subscribed to MVV update topics: {}*", properties.getMvvUpdateTopic());

						if (initTask!=null) {
							initTask.cancel(true);
							initTask = null;
						}
					} catch (JMSException e) {
						log.error("Failed to subscribe to MVV update topics", e);
					}
				},
				Instant.now().plusSeconds(properties.getMvvUpdateInitialDelaySeconds()),
				Duration.ofMinutes(properties.getMvvUpdateRetryDelaySeconds()));
	}

	private void onMessage(Message message) {
		try {
			log.debug("Received MVV update message");

			String mvvName = null;
			Double mvvValue = null;

			if (message instanceof ActiveMQMessage amqMessage) {
				mvvName = amqMessage.getDestination().getPhysicalName();
				log.debug("MVV update message topic: {}", mvvName);
			}
			if (Strings.CS.startsWith(mvvName, properties.getMvvUpdateTopic()))
				mvvName = Strings.CS.removeStart(mvvName, properties.getMvvUpdateTopic());
			log.debug("MVV constant: {}", mvvName);

			log.debug("Message type: {}", message.getClass().getName());
			if (message instanceof ActiveMQTextMessage amqTextMessage) {
				String bodyStr = amqTextMessage.getText();
				log.trace("AMQ Text message: {}", bodyStr);
				if (StringUtils.isNotBlank(bodyStr)) {
					EventMap payload = EventMap.parseEventMap(bodyStr);
                    mvvValue = payload.getMetricValue();
					log.debug("AMQ Text message: value={}", mvvValue);
                }
			}

			if (StringUtils.isNotBlank(mvvName) && mvvValue!=null) {
				log.debug("Ready to update MVV: {} = {}", mvvName, mvvValue);
				log.trace("MVV constants BEFORE: {}", brokerCepService.getConstants());
				Map<String, Double> values = mvvService.getValues();
				values.put(mvvName, mvvValue);
				mvvService.setValues(values);
				log.trace("MVV constants AFTER: {}", brokerCepService.getConstants());
			}
		} catch (JMSException e) {
			log.error("failed to process MVV update message", e);
		}
	}
}