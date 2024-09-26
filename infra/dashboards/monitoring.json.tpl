{
  "properties": {
    "lenses": [
      {
        "order": 0,
        "parts": [
          {
            "position": {
              "x": 0,
              "y": 0,
              "colSpan": 7,
              "rowSpan": 4
            },
            "metadata": {
              "inputs": [
                {
                  "name": "options",
                  "isOptional": true
                },
                {
                  "name": "sharedTimeRange",
                  "isOptional": true
                }
              ],
              "type": "Extension/HubsExtension/PartType/MonitorChartPart",
              "settings": {
                "content": {
                  "options": {
                    "chart": {
                      "metrics": [
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/Microsoft.Insights/components/${prefix}-appinsights"
                          },
                          "name": "customEvents/custom/EventsOnboardingInstitution_success",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "EventsOnboardingInstitution_success"
                          }
                        },
                        {
                          "resourceMetadata": {
                            "id": "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/Microsoft.Insights/components/${prefix}-appinsights"
                          },
                          "name": "customEvents/custom/EventsOnboardingInstitution_failures",
                          "aggregationType": 7,
                          "namespace": "microsoft.insights/components/kusto",
                          "metricVisualization": {
                            "displayName": "EventsOnboardingInstitution_failures"
                          }
                        }
                      ],
                      "title": "Notifications events count",
                      "titleKind": 2,
                      "visualization": {
                        "chartType": 2,
                        "legendVisualization": {
                          "isVisible": true,
                          "position": 2,
                          "hideHoverCard": false,
                          "hideLabelNames": true
                        },
                        "axisVisualization": {
                          "x": {
                            "isVisible": true,
                            "axisType": 2
                          },
                          "y": {
                            "isVisible": true,
                            "axisType": 1
                          }
                        },
                        "disablePinning": true
                      }
                    }
                  }
                }
              }
            }
          },
          {
            "position": {
              "x": 7,
              "y": 0,
              "colSpan": 9,
              "rowSpan": 2
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "80995217-105b-4393-b510-bb5bfff8ad5a",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "value": "P1D",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| order by timestamp desc\n| where tostring(customMeasurements) contains \"EventsOnboardingInstitution_success\"\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${prefix}-appinsights",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": false,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "PartTitle": "Notifications sent"
                }
              }
            }
          },
          {
            "position": {
              "x": 7,
              "y": 2,
              "colSpan": 9,
              "rowSpan": 2
            },
            "metadata": {
              "inputs": [
                {
                  "name": "resourceTypeMode",
                  "isOptional": true
                },
                {
                  "name": "ComponentId",
                  "isOptional": true
                },
                {
                  "name": "Scope",
                  "value": {
                    "resourceIds": [
                      "/subscriptions/${subscription_id}/resourceGroups/${prefix}-monitor-rg/providers/microsoft.insights/components/${prefix}-appinsights"
                    ]
                  },
                  "isOptional": true
                },
                {
                  "name": "PartId",
                  "value": "80be3866-9d1c-4b00-910c-108338113b57",
                  "isOptional": true
                },
                {
                  "name": "Version",
                  "value": "2.0",
                  "isOptional": true
                },
                {
                  "name": "TimeRange",
                  "value": "P1D",
                  "isOptional": true
                },
                {
                  "name": "DashboardId",
                  "isOptional": true
                },
                {
                  "name": "DraftRequestParameters",
                  "isOptional": true
                },
                {
                  "name": "Query",
                  "value": "customEvents\n| where name contains \"ONBOARDING-FN\"\n| extend d=parse_json(customDimensions)\n| order by timestamp desc\n| where tostring(customMeasurements) contains \"EventsOnboardingInstitution_failure\"\n\n",
                  "isOptional": true
                },
                {
                  "name": "ControlType",
                  "value": "AnalyticsGrid",
                  "isOptional": true
                },
                {
                  "name": "SpecificChart",
                  "isOptional": true
                },
                {
                  "name": "PartTitle",
                  "value": "Analytics",
                  "isOptional": true
                },
                {
                  "name": "PartSubTitle",
                  "value": "${prefix}-appinsights",
                  "isOptional": true
                },
                {
                  "name": "Dimensions",
                  "isOptional": true
                },
                {
                  "name": "LegendOptions",
                  "isOptional": true
                },
                {
                  "name": "IsQueryContainTimeRange",
                  "value": false,
                  "isOptional": true
                }
              ],
              "type": "Extension/Microsoft_OperationsManagementSuite_Workspace/PartType/LogsDashboardPart",
              "settings": {
                "content": {
                  "PartTitle": "Unsent notifications"
                }
              }
            }
          }
        ]
      }
    ],
    "metadata": {
      "model": {
        "timeRange": {
          "value": {
            "relative": {
              "duration": 24,
              "timeUnit": 1
            }
          },
          "type": "MsPortalFx.Composition.Configuration.ValueTypes.TimeRange"
        },
        "filterLocale": {
          "value": "en-us"
        },
        "filters": {
          "value": {
            "MsPortalFx_TimeRange": {
              "model": {
                "format": "utc",
                "granularity": "auto",
                "relative": "24h"
              },
              "displayCache": {
                "name": "UTC Time",
                "value": "Past 24 hours"
              },
              "filteredPartIds": [
                "StartboardPart-MonitorChartPart-5f1e177e-19ff-4652-87ff-6a71643f4355",
                "StartboardPart-LogsDashboardPart-4b6c45cd-8b84-486c-9165-c7c732d7b0fc",
                "StartboardPart-LogsDashboardPart-4b6c45cd-8b84-486c-9165-c7c732d7b141"
              ]
            }
          }
        }
      }
    }
  },
  "name": "Onboarding Notifications",
  "type": "Microsoft.Portal/dashboards",
  "location": "INSERT LOCATION",
  "tags": {
    "hidden-title": "onboarding-notifications"
  },
  "apiVersion": "2022-12-01-preview"
}