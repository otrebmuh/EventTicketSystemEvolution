# JaCoCo Coverage Report Configuration Guide

## Overview
This project uses JaCoCo for code coverage analysis with SonarQube integration. The configuration generates both individual module reports and an aggregate report for the entire project.

## Configuration Changes Made

### 1. JaCoCo Plugin Configuration (pom.xml)

The parent `pom.xml` has been updated with the following JaCoCo executions:

#### a) prepare-agent
Instruments the code during test execution to collect coverage data.

#### b) report (test phase)
Generates HTML and XML coverage reports for each module after tests run.
- **Output**: `<module>/target/site/jacoco/jacoco.xml`
- **Formats**: HTML (for viewing) and XML (for SonarQube)

#### c) report-aggregate (verify phase)
Combines coverage data from all modules into a single aggregate report.
- **Output**: `target/site/jacoco-aggregate/jacoco.xml` (in parent directory)
- **Purpose**: Provides project-wide coverage metrics for SonarQube

#### d) jacoco-check
Validates coverage thresholds (currently set to 0% minimum).

### 2. SonarQube Property Configuration

```xml
<sonar.coverage.jacoco.xmlReportPaths>
    ${project.basedir}/target/site/jacoco-aggregate/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
```

This tells SonarQube where to find the aggregate coverage report.

## How to Generate Coverage Reports

### Option 1: Run Tests and Generate Reports
```bash
mvn clean test
```
This generates individual module reports at:
- `auth-service/target/site/jacoco/jacoco.xml`
- `event-service/target/site/jacoco/jacoco.xml`
- `ticket-service/target/site/jacoco/jacoco.xml`
- `payment-service/target/site/jacoco/jacoco.xml`
- `notification-service/target/site/jacoco/jacoco.xml`
- `shared-common/target/site/jacoco/jacoco.xml`

### Option 2: Generate Aggregate Report
```bash
mvn clean verify
```
This generates:
1. Individual module reports (as above)
2. Aggregate report at: `target/site/jacoco-aggregate/jacoco.xml`

### Option 3: Run with SonarQube Analysis
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=ETSE2 \
  -Dsonar.projectName='ETSE2' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN
```

This will:
1. Clean previous builds
2. Run all tests
3. Generate coverage reports (individual + aggregate)
4. Send coverage data to SonarQube

## Viewing Coverage Reports

### Local HTML Reports

**Individual Module Reports:**
Open in browser:
- `auth-service/target/site/jacoco/index.html`
- `event-service/target/site/jacoco/index.html`
- etc.

**Aggregate Report:**
Open in browser:
- `target/site/jacoco-aggregate/index.html`

### SonarQube Dashboard
After running the sonar:sonar goal, view coverage at:
- http://localhost:9000/dashboard?id=ETSE2

## Troubleshooting

### Warning: "No coverage report can be found"

**Cause**: The aggregate report hasn't been generated yet.

**Solution**: Run `mvn verify` before `mvn sonar:sonar`, or use `mvn clean verify sonar:sonar` to do both.

### No jacoco.xml files generated

**Cause**: Tests didn't run or JaCoCo agent wasn't attached.

**Solution**: 
1. Ensure tests are running: `mvn test`
2. Check that `jacoco.exec` files are created in `target/` directories
3. Verify JaCoCo plugin is in the parent pom.xml

### Coverage shows 0% in SonarQube

**Possible causes**:
1. Aggregate report path is incorrect
2. Tests didn't run during verify phase
3. JaCoCo exec files are missing

**Solution**:
1. Verify the path in `sonar.coverage.jacoco.xmlReportPaths`
2. Run `mvn clean verify` and check for `target/site/jacoco-aggregate/jacoco.xml`
3. Ensure all modules have tests that execute

## Coverage Report Structure

```
project-root/
├── target/
│   └── site/
│       └── jacoco-aggregate/
│           ├── jacoco.xml          # Aggregate XML report for SonarQube
│           └── index.html          # Aggregate HTML report for viewing
├── auth-service/
│   └── target/
│       ├── jacoco.exec             # Raw coverage data
│       └── site/
│           └── jacoco/
│               ├── jacoco.xml      # Module XML report
│               └── index.html      # Module HTML report
├── event-service/
│   └── target/
│       └── site/
│           └── jacoco/
│               ├── jacoco.xml
│               └── index.html
└── [other modules...]
```

## Best Practices

1. **Always run `verify` phase** before SonarQube analysis to ensure aggregate report is generated
2. **Use `clean`** to ensure fresh coverage data: `mvn clean verify`
3. **Check HTML reports locally** before pushing to SonarQube
4. **Set meaningful coverage thresholds** in the jacoco-check execution once baseline is established
5. **Exclude generated code** from coverage if needed (add to JaCoCo configuration)

## Current Coverage Status

After running `mvn clean verify`, you should see:
- ✅ 127 tests passing across all modules
- ✅ Individual coverage reports for each module
- ✅ Aggregate coverage report for the entire project
- ✅ Coverage data ready for SonarQube analysis

## Next Steps

1. Run `mvn clean verify` to generate all coverage reports
2. Open `target/site/jacoco-aggregate/index.html` to view aggregate coverage
3. Run SonarQube analysis with the command provided above
4. Review coverage metrics in SonarQube dashboard
5. Set appropriate coverage thresholds based on current baseline
