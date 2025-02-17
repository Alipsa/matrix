# Release history

## v1.2.1, 2025-01-17
- Fix ODS import for Percentage and Currency columns by converting them to Doubles
- ExcelImporter now returns a LocalDate is no time info was specified to align with OdsImporter behavior 

## v1.2.0, 2025-01-16
- Default to 1:st sheet instead of 'Sheet1' if no sheet is given
- Change check for sheet param to detect Number instead of integer
- Breaking change: Change importOds to fetch as object instead of as string
- Upgrade POI 5.3.0 -> 5.4.0

## v1.1.0, 2025-01-08
- clarify which sheet we are on when trying to read outside boundaries
- change base package name to se.alipsa.matrix.spreadsheet
- adopt to matrix-core 2.2.0

## v1.0.3, 2024-10-31
- Add import ODS and XLSX from an InputStream
- Add import multiple sheets from an InputStream for both ods and xlsx
- fix bug when firstRowAsColumnNames is false (was off by one)
- adapt to matrix-core 2.0.0

## v1.0.2, 2024-07-07
- Consistently use index starting with 1 in the user api

## v1.0.1, 2023-11-21
- Change index of the SpreadsheetImporter to start with 1
- Name the imported Matrix after the sheet name

## v1.0.0, 2023-05-18
- initial version