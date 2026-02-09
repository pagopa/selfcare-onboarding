package it.pagopa.selfcare.onboarding.functions.utils;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

import java.util.Map;

public class TelemetryUtils {
    private TelemetryUtils() {}

    public static void trackFunction(
            TelemetryClient telemetryClient,
            String functionName,
            String message,
            SeverityLevel severityLevel,
            Map<String, String> properties) {
        telemetryClient.getContext().getOperation().setName(functionName);
        telemetryClient.trackTrace(message, severityLevel, properties);
    }
}
