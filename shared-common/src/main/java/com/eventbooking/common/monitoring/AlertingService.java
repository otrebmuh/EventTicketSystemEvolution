package com.eventbooking.common.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for configuring CloudWatch alarms and alerting
 */
@Service
public class AlertingService {

    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);

    @Value("${spring.application.name:event-booking-service}")
    private String applicationName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${monitoring.cloudwatch.namespace:EventBookingSystem}")
    private String cloudWatchNamespace;

    @Value("${monitoring.alerting.enabled:false}")
    private boolean alertingEnabled;

    @Value("${monitoring.alerting.sns.topic.arn:}")
    private String snsTopicArn;

    private CloudWatchClient cloudWatchClient;

    @PostConstruct
    public void init() {
        if (alertingEnabled) {
            cloudWatchClient = CloudWatchClient.builder()
                .region(Region.of(awsRegion))
                .build();
            
            logger.info("Alerting service initialized for application: {}", applicationName);
        }
    }

    /**
     * Create critical system alarms
     */
    public void createSystemAlarms() {
        if (!alertingEnabled || snsTopicArn.isEmpty()) {
            logger.warn("Alerting is disabled or SNS topic ARN is not configured");
            return;
        }

        List<PutMetricAlarmRequest> alarms = new ArrayList<>();

        // High error rate alarm
        alarms.add(createErrorRateAlarm());

        // High response time alarm
        alarms.add(createResponseTimeAlarm());

        // High memory usage alarm
        alarms.add(createMemoryUsageAlarm());

        // Database connection pool alarm
        alarms.add(createDatabaseConnectionAlarm());

        // Failed payment alarm
        alarms.add(createFailedPaymentAlarm());

        // Create all alarms
        for (PutMetricAlarmRequest alarm : alarms) {
            try {
                cloudWatchClient.putMetricAlarm(alarm);
                logger.info("Created alarm: {}", alarm.alarmName());
            } catch (Exception e) {
                logger.error("Failed to create alarm: {}", alarm.alarmName(), e);
            }
        }
    }

    private PutMetricAlarmRequest createErrorRateAlarm() {
        return PutMetricAlarmRequest.builder()
            .alarmName(applicationName + "-high-error-rate")
            .alarmDescription("Alert when error rate exceeds 5%")
            .namespace(cloudWatchNamespace)
            .metricName("api.calls.total")
            .dimensions(Dimension.builder()
                .name("application")
                .value(applicationName)
                .build())
            .statistic(Statistic.SUM)
            .period(300) // 5 minutes
            .evaluationPeriods(2)
            .threshold(50.0) // 50 errors in 5 minutes
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .actionsEnabled(true)
            .alarmActions(snsTopicArn)
            .build();
    }

    private PutMetricAlarmRequest createResponseTimeAlarm() {
        return PutMetricAlarmRequest.builder()
            .alarmName(applicationName + "-high-response-time")
            .alarmDescription("Alert when average response time exceeds 3 seconds")
            .namespace(cloudWatchNamespace)
            .metricName("api.response.time")
            .dimensions(Dimension.builder()
                .name("application")
                .value(applicationName)
                .build())
            .statistic(Statistic.AVERAGE)
            .period(300) // 5 minutes
            .evaluationPeriods(2)
            .threshold(3000.0) // 3 seconds in milliseconds
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .actionsEnabled(true)
            .alarmActions(snsTopicArn)
            .build();
    }

    private PutMetricAlarmRequest createMemoryUsageAlarm() {
        return PutMetricAlarmRequest.builder()
            .alarmName(applicationName + "-high-memory-usage")
            .alarmDescription("Alert when memory usage exceeds 85%")
            .namespace("AWS/ECS")
            .metricName("MemoryUtilization")
            .dimensions(
                Dimension.builder()
                    .name("ServiceName")
                    .value(applicationName)
                    .build()
            )
            .statistic(Statistic.AVERAGE)
            .period(300) // 5 minutes
            .evaluationPeriods(2)
            .threshold(85.0)
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .actionsEnabled(true)
            .alarmActions(snsTopicArn)
            .build();
    }

    private PutMetricAlarmRequest createDatabaseConnectionAlarm() {
        return PutMetricAlarmRequest.builder()
            .alarmName(applicationName + "-database-connection-issues")
            .alarmDescription("Alert when database connections are failing")
            .namespace(cloudWatchNamespace)
            .metricName("database.query.time")
            .dimensions(Dimension.builder()
                .name("application")
                .value(applicationName)
                .build())
            .statistic(Statistic.AVERAGE)
            .period(300) // 5 minutes
            .evaluationPeriods(2)
            .threshold(5000.0) // 5 seconds
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .actionsEnabled(true)
            .alarmActions(snsTopicArn)
            .build();
    }

    private PutMetricAlarmRequest createFailedPaymentAlarm() {
        return PutMetricAlarmRequest.builder()
            .alarmName(applicationName + "-failed-payments")
            .alarmDescription("Alert when payment failure rate is high")
            .namespace(cloudWatchNamespace)
            .metricName("payment.processing.total")
            .dimensions(
                Dimension.builder()
                    .name("application")
                    .value(applicationName)
                    .build(),
                Dimension.builder()
                    .name("success")
                    .value("false")
                    .build()
            )
            .statistic(Statistic.SUM)
            .period(300) // 5 minutes
            .evaluationPeriods(1)
            .threshold(10.0) // 10 failed payments in 5 minutes
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .actionsEnabled(true)
            .alarmActions(snsTopicArn)
            .build();
    }

    /**
     * Delete all alarms for this application
     */
    public void deleteAlarms() {
        if (!alertingEnabled) {
            return;
        }

        List<String> alarmNames = List.of(
            applicationName + "-high-error-rate",
            applicationName + "-high-response-time",
            applicationName + "-high-memory-usage",
            applicationName + "-database-connection-issues",
            applicationName + "-failed-payments"
        );

        try {
            cloudWatchClient.deleteAlarms(DeleteAlarmsRequest.builder()
                .alarmNames(alarmNames)
                .build());
            logger.info("Deleted alarms for application: {}", applicationName);
        } catch (Exception e) {
            logger.error("Failed to delete alarms", e);
        }
    }
}
