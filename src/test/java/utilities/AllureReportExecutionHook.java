package utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

import java.util.List;

/**
 * TestNG execution listener to automatically generate and open Allure reports
 */
public class AllureReportExecutionHook implements IExecutionListener, IReporter {
    private static final Logger LOG = LoggerFactory.getLogger(AllureReportExecutionHook.class);

    @Override
    public void onExecutionStart() {
        LOG.info("Test execution started");
    }

    @Override
    public void onExecutionFinish() {
        LOG.info("Test execution finished, generating Allure report");
        if (Boolean.parseBoolean(System.getProperty("allure.auto.generate", "true"))) {
            AllureReportLauncher.generateAndOpenReport();
        }
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        // Additional report generation logic can be added here
    }
}
