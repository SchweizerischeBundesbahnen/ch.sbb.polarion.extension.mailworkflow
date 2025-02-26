[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.mailworkflow)

# Mail Workflow extension for Polarion ALM

> [!IMPORTANT]
> Starting from version 2.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2410.

## Quick start

The latest version of the extension can be downloaded from the [releases page](../../releases/latest) and installed to Polarion instance without necessity to be compiled from the sources.
The extension should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.mailworkflow/eclipse/plugins` and changes will take effect after Polarion restart.
> [!IMPORTANT]
> Don't forget to clear `<polarion_home>/data/workspace/.config` folder after extension installation/update to make it work properly.

## Build

This extension can be produced using maven:

```bash
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion `ch.sbb.polarion.extension.mailworkflow-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.mailworkflow/eclipse/plugins`
It can be done manually or automated using maven build:

```bash
mvn clean install -P install-to-local-polarion
```

For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

If the extension is installed, the workflow becomes available under the `Admin Panel -> Workitems -> Workflow -> .. -> Functions -> mailWorkflowFunction`

### Function parameters
The extension can be configured with help of parameters (accessible via the pen next to the function name), here is a list of these parameters and their description:

| Parameter description                                                                                                                         | Parameter name     | Required |                                    Default value                                     |
|-----------------------------------------------------------------------------------------------------------------------------------------------|--------------------|:--------:|:------------------------------------------------------------------------------------:|
| Sender email address                                                                                                                          | `sender`           |   yes    |                                          -                                           |
| WorkItem field containing recipient objects, which in their turn contain email. If custom field used, should contain `Collection<IUser>`      | `recipientsField`  |    no    | `assignees`<br/>Other possible values: `approvals`, `author`, or any custom field ID |
| Subject to use in emails                                                                                                                      | `emailSubject`     |    no    |                                 `Deadline Reminder`                                  |
| WorkItem field containing reminder date value                                                                                                 | `dateField`        |    no    |                                      `dueDate`                                       |
| Summary for generated calendar event                                                                                                          | `eventSummary`        |    no    |                          `WorkItem <WORKITEM ID> Deadline`                           |
| Description for generated calendar event                                                                                                      | `eventDescription`           |    no    |                                          -                                           |
| Priority for generated calendar event. Numeric values - 0 (which means undefined priority) and then from 1 (max priority) to 9 (min priority) | `eventPriority`           |    no    |                                         `0`                                          |
| Category for generated calendar event, like `Green category`                                                                                  | `eventCategory`           |    no    |                                          -                                           |
| Location for generated calendar event, like `Room 404`                                                                                        | `eventLocation`           |    no    |                                          -                                           |

### Calling Context

The name of the workflow is configured in `src/META-INF/hivemodule.xml` and can be changed there.

### REST API

This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).
