package com.femzyk.fleetmanagement.reporting;

import java.io.IOException;
import java.nio.file.Path;

public interface ReportExporter {
    String fileExtension();
    void export(Report report, Path target) throws IOException;
}
