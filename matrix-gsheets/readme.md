# Matrix-gsheets, Google Sheets support for Matrix

The Google API is a bit primitive in terms of available meta data. One such thing is handling date types.
Although it is possible to convert a date to a serial number, when downloading the sheet, there is now 
way to distinguish that from an ordinary number. Hence, we are forced to treat dates as strings in the export
method.