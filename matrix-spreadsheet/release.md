# Release history

## v1.0.4, In progress
- clarify which sheet we are on when trying to read outside boundaries

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