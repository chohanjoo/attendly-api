#!/bin/bash

# 교회 출석 자동화 테스트 스크립트
#
# 이 스크립트는 다음 시나리오를 테스트합니다:
# 1. "M village" 마을 조회
# 2. "M village" 마을에 속한 모든 GBS 그룹 조회
# 3. 각 GBS 그룹의 3, 4, 5월 주차별 출석 등록
#
# 실행 방법: ./register_gbs_attendance.sh

BASE_URL="http://localhost:8080"

# jq 설치 여부 확인
if ! command -v jq &> /dev/null; then
  echo "jq가 설치되어 있지 않습니다. 많은 기능이 제한됩니다."
  JQ_AVAILABLE=false
else
  JQ_AVAILABLE=true
fi

# 로그인 및 토큰 가져오기
login() {
  echo "관리자 계정으로 로그인 중..."
  
  LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
      "email": "admin@church.com",
      "password": "admin123"
    }')
  
  if [ "$JQ_AVAILABLE" = true ]; then
    # 새 API 응답 구조에서 토큰 추출
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
  else
    # grep 명령어로 토큰 추출 (새 응답 구조에 맞게 수정)
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
  fi
  
  if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "로그인 실패. 기본 관리자 계정이 아직 생성되지 않았을 수 있습니다."
    echo "다른 계정으로 로그인을 시도합니다..."
    
    # 다른 계정 시도
    LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "email": "admin@church.com",
        "password": "admin123"
      }')
    
    if [ "$JQ_AVAILABLE" = true ]; then
      ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
    else
      ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    fi
    
    if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
      echo "대체 계정으로도 로그인 실패. 기존 계정 정보를 확인해주세요."
      exit 1
    fi
  fi
  
  echo "로그인 성공! 토큰이 발급되었습니다."
}

# M Village 마을 정보 조회
get_village_info() {
  echo "1. 'M village' 마을 정보 조회 중..."
  
  # API 변경으로 인해 직접 마을 ID 1부터 10까지 시도
  for i in {1..10}; do
    VILLAGE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/village-leader/$i/gbs" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
    
    echo "마을 ID ${i} 조회 응답: ${VILLAGE_RESPONSE}"
    
    # 성공 응답인지 확인
    if [[ "$VILLAGE_RESPONSE" == *'"success":true'* ]]; then
      if [ "$JQ_AVAILABLE" = true ]; then
        VILLAGE_ID=$(echo "${VILLAGE_RESPONSE}" | jq -r '.data.villageId')
        VILLAGE_NAME=$(echo "${VILLAGE_RESPONSE}" | jq -r '.data.villageName')
      else
        VILLAGE_ID=$(echo "${VILLAGE_RESPONSE}" | grep -o '"villageId":[0-9]*' | cut -d':' -f2)
        VILLAGE_NAME=$(echo "${VILLAGE_RESPONSE}" | grep -o '"villageName":"[^"]*' | cut -d'"' -f4)
      fi
      
      echo "마을 정보: ID=${VILLAGE_ID}, 이름=${VILLAGE_NAME}"
      
      if [[ "$VILLAGE_NAME" == *"M village"* ]] || [[ "$VILLAGE_NAME" == *"M 마을"* ]]; then
        echo "M village 발견! ID: $VILLAGE_ID"
        break
      fi
    fi
    
    # 마을 찾지 못한 경우 1초 대기 후 다음 ID 시도
    sleep 1
  done
  
  # M village를 찾지 못한 경우, 첫 번째 마을 사용
  if [ -z "$VILLAGE_ID" ] || [ "$VILLAGE_ID" = "null" ]; then
    echo "M village를 찾을 수 없어 첫 번째 마을을 사용합니다."
    VILLAGE_ID=1
    VILLAGE_RESPONSE=$(curl -s -X GET "$BASE_URL/api/village-leader/$VILLAGE_ID/gbs" \
      -H "Authorization: Bearer $ACCESS_TOKEN")
      
    if [ "$JQ_AVAILABLE" = true ]; then
      VILLAGE_NAME=$(echo "${VILLAGE_RESPONSE}" | jq -r '.data.villageName')
    else
      VILLAGE_NAME=$(echo "${VILLAGE_RESPONSE}" | grep -o '"villageName":"[^"]*' | cut -d'"' -f4)
    fi
    
    echo "사용할 마을 정보: ID=${VILLAGE_ID}, 이름=${VILLAGE_NAME}"
  fi
}

# M Village에 속한 모든 GBS 그룹 조회
get_gbs_groups() {
  echo "2. '$VILLAGE_NAME' 마을에 속한 모든 GBS 그룹 조회 중..."
  
  GBS_RESPONSE=$(curl -s -X GET "$BASE_URL/api/village-leader/$VILLAGE_ID/gbs" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  
  echo "GBS 그룹 조회 응답: $GBS_RESPONSE"
  
  if [ "$JQ_AVAILABLE" = true ]; then
    GBS_COUNT=$(echo "${GBS_RESPONSE}" | jq -r '.data.gbsCount')
    GBS_IDS=($(echo "${GBS_RESPONSE}" | jq -r '.data.gbsList[].gbsId'))
    GBS_NAMES=($(echo "${GBS_RESPONSE}" | jq -r '.data.gbsList[].gbsName'))
    
    # GBS 그룹의 멤버 정보 저장을 위한 배열 생성
    declare -a ALL_GBS_MEMBERS
    
    echo "jq로 멤버 추출 시작..."
    
    # 각 GBS별 멤버 ID 추출 및 저장
    for i in "${!GBS_IDS[@]}"; do
      GBS_ID=${GBS_IDS[$i]}
      
      # API로부터 멤버 정보 추출
      MEMBERS=$(echo "${GBS_RESPONSE}" | jq -r ".data.gbsList[$i].members[].id" 2>/dev/null || echo "")
      
      # 멤버가 없거나 추출 실패 시 하드코딩 멤버 사용
      if [ -z "$MEMBERS" ]; then
        # 하드코딩된 멤버 ID 배열 (테스트용)
        if [ "$GBS_ID" -eq 1 ]; then
          ALL_GBS_MEMBERS[$i]="4"
        elif [ "$GBS_ID" -eq 2 ]; then
          ALL_GBS_MEMBERS[$i]="5 6 7 29 30"
        elif [ "$GBS_ID" -eq 3 ]; then
          ALL_GBS_MEMBERS[$i]="31 32 33 34 35"
        elif [ "$GBS_ID" -eq 4 ]; then
          ALL_GBS_MEMBERS[$i]="36 37 38 39 40"
        elif [ "$GBS_ID" -eq 5 ]; then
          ALL_GBS_MEMBERS[$i]="41 42 43 44 45"
        elif [ "$GBS_ID" -eq 6 ]; then
          ALL_GBS_MEMBERS[$i]="46 47 48 49 50"
        elif [ "$GBS_ID" -eq 7 ]; then
          ALL_GBS_MEMBERS[$i]="51 52 53 54 55"
        elif [ "$GBS_ID" -eq 8 ]; then
          ALL_GBS_MEMBERS[$i]="56 57 58 59 60"
        elif [ "$GBS_ID" -eq 9 ]; then
          ALL_GBS_MEMBERS[$i]="61 62 63 64 65"
        elif [ "$GBS_ID" -eq 10 ]; then
          ALL_GBS_MEMBERS[$i]="66 67 68 69 70"
        else
          ALL_GBS_MEMBERS[$i]=""
        fi
      else
        # API에서 추출한 멤버 ID 사용
        ALL_GBS_MEMBERS[$i]=$(echo "$MEMBERS" | tr '\n' ' ')
      fi
      
      echo "GBS ID $GBS_ID의 멤버 목록: ${ALL_GBS_MEMBERS[$i]}"
    done
  else
    # jq가 없는 환경에서는 grep/sed 등으로 처리
    GBS_COUNT=$(echo "${GBS_RESPONSE}" | grep -o '"gbsCount":[0-9]*' | cut -d':' -f2)
    
    # GBS ID와 이름 추출 (간단한 예시, 실제 환경에서 더 복잡할 수 있음)
    GBS_IDS=($(echo "${GBS_RESPONSE}" | grep -o '"gbsId":[0-9]*' | cut -d':' -f2))
    GBS_NAMES=($(echo "${GBS_RESPONSE}" | grep -o '"gbsName":"[^"]*' | cut -d'"' -f4))
    
    # GBS 그룹의 멤버 정보 저장을 위한 배열 생성
    declare -a ALL_GBS_MEMBERS
    
    echo "grep으로 멤버 추출 시작..."
    
    # 각 GBS 그룹별로 멤버 ID 추출
    for i in "${!GBS_IDS[@]}"; do
      GBS_ID=${GBS_IDS[$i]}
      
      # 하드코딩된 멤버 ID 배열 (테스트용)
      if [ "$GBS_ID" -eq 1 ]; then
        ALL_GBS_MEMBERS[$i]="4"
      elif [ "$GBS_ID" -eq 2 ]; then
        ALL_GBS_MEMBERS[$i]="5 6 7 29 30"
      elif [ "$GBS_ID" -eq 3 ]; then
        ALL_GBS_MEMBERS[$i]="31 32 33 34 35"
      elif [ "$GBS_ID" -eq 4 ]; then
        ALL_GBS_MEMBERS[$i]="36 37 38 39 40"
      elif [ "$GBS_ID" -eq 5 ]; then
        ALL_GBS_MEMBERS[$i]="41 42 43 44 45"
      elif [ "$GBS_ID" -eq 6 ]; then
        ALL_GBS_MEMBERS[$i]="46 47 48 49 50"
      elif [ "$GBS_ID" -eq 7 ]; then
        ALL_GBS_MEMBERS[$i]="51 52 53 54 55"
      elif [ "$GBS_ID" -eq 8 ]; then
        ALL_GBS_MEMBERS[$i]="56 57 58 59 60"
      elif [ "$GBS_ID" -eq 9 ]; then
        ALL_GBS_MEMBERS[$i]="61 62 63 64 65"
      elif [ "$GBS_ID" -eq 10 ]; then
        ALL_GBS_MEMBERS[$i]="66 67 68 69 70"
      else
        ALL_GBS_MEMBERS[$i]=""
      fi
      
      echo "GBS ID ${GBS_ID}의 멤버 목록: ${ALL_GBS_MEMBERS[$i]}"
    done
  fi
  
  echo "마을에 속한 GBS 그룹 수: ${GBS_COUNT}"
  
  if [ ${#GBS_IDS[@]} -eq 0 ]; then
    echo "GBS 그룹이 없습니다."
    exit 1
  fi
  
  for i in "${!GBS_IDS[@]}"; do
    echo "GBS ID: ${GBS_IDS[$i]}, 이름: ${GBS_NAMES[$i]}, 멤버 ID: ${ALL_GBS_MEMBERS[$i]}"
  done
  
  # 전역 변수로 설정
  GBS_MEMBERS=("${ALL_GBS_MEMBERS[@]}")
}

# 3월, 4월, 5월의 모든 일요일 날짜 구하기
get_sundays() {
  echo "3월, 4월, 5월의 모든 일요일 날짜를 계산합니다..."
  
  # 현재 연도 가져오기
  CURRENT_YEAR=$(date +"%Y")
  
  # 각 월별로 일요일 계산
  SUNDAYS=()
  
  # macOS 환경을 위한 일요일 계산 방식
  for MONTH in {3..5}; do
    # 해당 월의 첫 날
    FIRST_DAY="$CURRENT_YEAR-$MONTH-01"
    
    # 해당 월의 일 수 (macOS는 날짜 계산에 다른 방식 사용)
    case $MONTH in
      1|3|5|7|8|10|12) DAYS_IN_MONTH=31 ;;
      4|6|9|11) DAYS_IN_MONTH=30 ;;
      2)
        # 윤년 확인
        if (( $CURRENT_YEAR % 400 == 0 )) || (( $CURRENT_YEAR % 4 == 0 && $CURRENT_YEAR % 100 != 0 )); then
          DAYS_IN_MONTH=29
        else
          DAYS_IN_MONTH=28
        fi
        ;;
    esac
    
    for DAY in $(seq 1 $DAYS_IN_MONTH); do
      # 날짜 형식 생성
      DATE_STR=$(printf "%04d-%02d-%02d" $CURRENT_YEAR $MONTH $DAY)
      
      # macOS에서는 다른 방식으로 요일 확인 (0=일요일)
      DAY_OF_WEEK=$(date -j -f "%Y-%m-%d" "$DATE_STR" +%w 2>/dev/null)
      
      # 명령이 실패하면 기본값 설정
      if [ $? -ne 0 ]; then
        # 일부 macOS 버전에서는 다른 형식 사용
        DAY_OF_WEEK=$(date -j -f "%Y-%m-%d" "$DATE_STR" +%w 2>/dev/null || echo "9")
      fi
      
      if [ "$DAY_OF_WEEK" = "0" ]; then
        SUNDAYS+=("$DATE_STR")
      fi
    done
  done
  
  # 테스트용 고정 일요일 날짜 (실제 데이터를 얻을 수 없는 경우)
  if [ ${#SUNDAYS[@]} -eq 0 ]; then
    echo "일요일을 계산할 수 없어 고정 테스트 날짜를 사용합니다."
    SUNDAYS=("2025-03-02" "2025-03-09" "2025-03-16" "2025-03-23" "2025-03-30" 
             "2025-04-06" "2025-04-13" "2025-04-20" "2025-04-27"
             "2025-05-04" "2025-05-11" "2025-05-18" "2025-05-25")
  fi
  
  echo "3월, 4월, 5월의 일요일 날짜: ${SUNDAYS[*]}"
}

# 출석 데이터 생성 및 등록
register_attendance() {
  echo "3. 각 GBS 그룹의 3, 4, 5월 주차별 출석 등록 시작..."
  
  for i in "${!GBS_IDS[@]}"; do
    GBS_ID=${GBS_IDS[$i]}
    echo "GBS ID ${GBS_ID}의 출석 등록 시작..."
    
    # 해당 GBS의 멤버 ID 문자열을 배열로 변환
    MEMBERS_STR="${GBS_MEMBERS[$i]}"
    
    # 멤버 ID가 있는지 확인
    if [[ -n "$MEMBERS_STR" ]]; then
      # 문자열을 배열로 변환
      MEMBERS=($MEMBERS_STR)
      echo "멤버 ID: ${MEMBERS[*]}"
      
      # 모든 일요일(주차)에 대해 출석 등록
      for SUNDAY in "${SUNDAYS[@]}"; do
        echo "주차 시작일: ${SUNDAY} 출석 등록 중..."
        
        # 출석할 멤버 수 랜덤 선택 (최소 1명부터 전체 멤버까지)
        if [ ${#MEMBERS[@]} -gt 0 ]; then
          ATTEND_COUNT=$((RANDOM % ${#MEMBERS[@]} + 1))
          
          # 출석 데이터 JSON 생성
          ATTENDANCE_JSON='{"gbsId":'$GBS_ID',"weekStart":"'$SUNDAY'","attendances":['
          
          for ((j=0; j<ATTEND_COUNT; j++)); do
            MEMBER_ID=${MEMBERS[$j]}
            
            # 랜덤 출석 상태 (O 또는 X)
            if [ $((RANDOM % 5)) -lt 4 ]; then  # 80% 확률로 출석
              WORSHIP="O"
            else
              WORSHIP="X"
            fi
            
            # 랜덤 QT 횟수 (0-6)
            QT_COUNT=$((RANDOM % 7))
            
            # 랜덤 대학부 출석 등급 (A, B, C)
            MINISTRY_GRADES=("A" "B" "C")
            MINISTRY=${MINISTRY_GRADES[$((RANDOM % 3))]}
            
            ATTENDANCE_JSON+='{"memberId":'$MEMBER_ID',"worship":"'$WORSHIP'","qtCount":'$QT_COUNT',"ministry":"'$MINISTRY'"}'
            
            # 마지막 항목이 아니면 쉼표 추가
            if [ $j -lt $((ATTEND_COUNT-1)) ]; then
              ATTENDANCE_JSON+=','
            fi
          done
          
          ATTENDANCE_JSON+=']}'
          
          # 출석 데이터 전송
          echo "출석 데이터 전송 중: $ATTENDANCE_JSON"
          ATTENDANCE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/attendance" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -d "$ATTENDANCE_JSON")
          
          echo "출석 등록 응답: $ATTENDANCE_RESPONSE"
          
          # API 요청 사이에 잠시 대기 (서버 부하 방지)
          sleep 1
        else
          echo "멤버가 없습니다. 다음 GBS로 넘어갑니다."
          break
        fi
      done
    else
      echo "멤버 정보가 없습니다. 다음 GBS로 넘어갑니다."
    fi
  done
}

# 메인 실행 로직
main() {
  login
  get_village_info
  get_gbs_groups
  get_sundays
  register_attendance
  
  echo "모든 출석 등록이 완료되었습니다!"
}

# 스크립트 실행
main 