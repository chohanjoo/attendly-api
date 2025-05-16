# Apache POI 및 OpenCSV 라이브러리 가이드

이 문서는 Attendly API에서 통계 데이터를 Excel 및 CSV 형식으로 내보내기 위해 사용하는 Apache POI 및 OpenCSV 라이브러리에 대한 가이드입니다.

## 목차

1. [개요](#개요)
2. [의존성](#의존성)
3. [Apache POI 사용 방법](#apache-poi-사용-방법)
4. [OpenCSV 사용 방법](#opencsv-사용-방법)
5. [사용 예시](#사용-예시)

## 개요

통계 데이터를 Excel 및 CSV 형식으로 내보내기 위해 다음 라이브러리를 사용합니다:

- **Apache POI**: 자바로 Microsoft Office 문서(Excel, Word 등)를 읽고 쓸 수 있게 해주는 라이브러리입니다.
- **OpenCSV**: CSV 파일 생성과 파싱을 위한 라이브러리입니다.

## 의존성

프로젝트의 `build.gradle.kts` 파일에 다음 의존성이 추가되어 있습니다:

```kotlin
// Excel 및 CSV 파일 생성을 위한 의존성
implementation("org.apache.poi:poi:5.2.3")
implementation("org.apache.poi:poi-ooxml:5.2.3")
implementation("com.opencsv:opencsv:5.7.1")
```

## Apache POI 사용 방법

### 기본 개념

Apache POI는 다음과 같은 주요 구성 요소를 가지고 있습니다:

- **Workbook**: Excel 파일 전체를 나타냅니다.
- **Sheet**: 워크북 내의 각 시트를 나타냅니다.
- **Row**: 시트 내의 행을 나타냅니다.
- **Cell**: 셀 값 및 서식을 나타냅니다.
- **CellStyle**: 셀 스타일을 정의합니다 (배경색, 테두리, 폰트 등).

### 기본 사용 방법

1. **워크북 생성**

```kotlin
// XLS 형식 워크북 생성 (Excel 97-2003)
val workbook = HSSFWorkbook()

// XLSX 형식 워크북 생성 (Excel 2007 이상)
val workbook = XSSFWorkbook()
```

2. **시트 생성**

```kotlin
val sheet = workbook.createSheet("시트이름")
```

3. **행 및 셀 생성**

```kotlin
val row = sheet.createRow(0) // 0번 행 (첫 번째 행)
val cell = row.createCell(0) // 0번 열 (A열)
cell.setCellValue("셀 내용") // 문자열 값 설정
```

4. **셀 스타일 적용**

```kotlin
val style = workbook.createCellStyle()
style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
style.fillPattern = FillPatternType.SOLID_FOREGROUND

// 스타일 적용
cell.cellStyle = style
```

5. **파일로 저장**

```kotlin
// OutputStream에 쓰기
workbook.write(outputStream)
outputStream.close()
workbook.close()
```

## OpenCSV 사용 방법

### 기본 사용 방법

1. **CSVWriter 초기화**

```kotlin
val writer = CSVWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
```

2. **데이터 쓰기**

```kotlin
// 헤더 작성
writer.writeNext(arrayOf("헤더1", "헤더2", "헤더3"), false)

// 데이터 행 작성
writer.writeNext(arrayOf("값1", "값2", "값3"), false)
```

3. **닫기**

```kotlin
writer.close()
```

## 사용 예시

Attendly API에서는 `MinisterStatisticsService` 클래스에 Excel 및 CSV 내보내기 메서드가 구현되어 있습니다:

### Excel 내보내기 예시

```kotlin
@Transactional(readOnly = true)
fun exportDepartmentStatisticsToExcel(departmentId: Long, startDate: LocalDate, endDate: LocalDate, outputStream: OutputStream) {
    val statistics = getDepartmentStatistics(departmentId, startDate, endDate)
    
    XSSFWorkbook().use { workbook ->
        // 헤더 스타일 생성
        val headerStyle = createHeaderStyle(workbook)
        
        // 요약 시트 생성
        createSummarySheet(workbook, statistics, headerStyle)
        
        // 마을별 통계 시트 생성
        createVillageStatisticsSheet(workbook, statistics, headerStyle)
        
        // 주간 통계 시트 생성
        createWeeklyStatisticsSheet(workbook, statistics, headerStyle)
        
        // 파일 작성
        workbook.write(outputStream)
        outputStream.flush()
    }
}
```

### CSV 내보내기 예시

```kotlin
@Transactional(readOnly = true)
fun exportDepartmentStatisticsToCSV(departmentId: Long, startDate: LocalDate, endDate: LocalDate, outputStream: OutputStream) {
    val statistics = getDepartmentStatistics(departmentId, startDate, endDate)
    
    OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
        CSVWriter(writer).use { csvWriter ->
            // 요약 정보 출력
            csvWriter.writeNext(arrayOf("부서 통계 요약", "", "", ""), false)
            csvWriter.writeNext(arrayOf("부서명", statistics.departmentName, "", ""), false)
            
            // 데이터 작성 계속...
        }
    }
}
```

## 참고 자료

- [Apache POI 공식 문서](https://poi.apache.org/components/spreadsheet/index.html)
- [OpenCSV 공식 문서](http://opencsv.sourceforge.net/)

---
문서 작성일: 2023-09-30 