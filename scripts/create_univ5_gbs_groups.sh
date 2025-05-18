#!/bin/bash

# jq 설치 여부 확인
if ! command -v jq &> /dev/null; then
  echo "jq가 설치되어 있지 않습니다. jq 없이 진행합니다."
  JQ_AVAILABLE=false
else
  JQ_AVAILABLE=true
fi

# 테스트 자동화 스크립트
#
# 시나리오:
# 1. "univ5" 부서 조회
# 2. VILLAGE_LEADER 권한 마을장 계정 생성
# 3. "Q village" 마을 생성
# 4. "univ5" 부서에 속한 LEADER 조회
# 5. 20명의 리더에게 GBS 그룹 할당

BASE_URL="http://localhost:8080"

# 1. 관리자 계정으로 로그인
echo "1. 관리자 계정으로 로그인"
ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "hanjoo@naver.com",
    "password": "test123!@#"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

echo "토큰: $ACCESS_TOKEN"

# 1. univ5 부서 조회
echo "2. 'univ5' 부서 조회"
# 먼저 모든 부서를 가져옵니다
ALL_DEPARTMENTS=$(curl -s -X GET "$BASE_URL/api/admin/organization/departments" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "모든 부서: $ALL_DEPARTMENTS"

# univ5 부서의 ID를 추출합니다
if [ "$JQ_AVAILABLE" = true ]; then
  # jq를 사용하여 추출
  DEPARTMENT_ID=$(echo "$ALL_DEPARTMENTS" | jq '.data.items[] | select(.name=="univ5") | .id')
else
  # grep을 사용하여 추출 (대체 방법)
  # 응답 구조가 변경되어 items 배열 내에서 찾아야 함
  ITEMS_SECTION=$(echo "$ALL_DEPARTMENTS" | grep -o '"items":\[.*\]' | sed 's/"items"://')
  DEPARTMENT_ID=$(echo "$ITEMS_SECTION" | grep -o '"id":[0-9]*,"name":"univ5"' | grep -o '"id":[0-9]*' | cut -d':' -f2)
fi

if [ -z "$DEPARTMENT_ID" ]; then
  echo "❌ 'univ5' 부서를 찾을 수 없습니다."
  exit 1
fi

echo "univ5 부서 ID: $DEPARTMENT_ID"

# 부서 상세 정보 조회
DEPARTMENT_INFO=$(curl -s -X GET "$BASE_URL/api/admin/organization/departments/$DEPARTMENT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "univ5 부서 상세 정보: $DEPARTMENT_INFO"

# 2. 마을장 계정 생성
echo "3. 마을장 계정 생성"
# 타임스탬프를 이용하여 고유한 이메일 생성
TIMESTAMP=$(date +%s)
VILLAGE_LEADER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/users" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"name\": \"Emma Wilson\",
    \"email\": \"univ5village_$TIMESTAMP@example.com\",
    \"password\": \"test123!@#\",
    \"role\": \"VILLAGE_LEADER\",
    \"departmentId\": $DEPARTMENT_ID,
    \"phoneNumber\": \"010-9999-8888\",
    \"birthDate\": \"1990-05-15\"
  }")

# 마을장 ID 추출
if [ "$JQ_AVAILABLE" = true ]; then
  VILLAGE_LEADER_ID=$(echo "$VILLAGE_LEADER_RESPONSE" | jq -r '.data.id')
else
  VILLAGE_LEADER_ID=$(echo "$VILLAGE_LEADER_RESPONSE" | grep -o '"data":{.*}' | grep -o '"id":[0-9]*' | cut -d':' -f2)
fi

if [ -z "$VILLAGE_LEADER_ID" ] || [ "$VILLAGE_LEADER_ID" = "null" ]; then
  echo "❌ 마을장 계정 생성 실패"
  echo "응답: $VILLAGE_LEADER_RESPONSE"
  exit 1
fi

echo "마을장 ID: $VILLAGE_LEADER_ID"

# 3. M village 마을 생성
echo "4. 'M village' 마을 생성"
VILLAGE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/organization/villages" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"name\": \"M village\",
    \"departmentId\": $DEPARTMENT_ID,
    \"villageLeaderId\": $VILLAGE_LEADER_ID
  }")

# 마을 ID 추출
if [ "$JQ_AVAILABLE" = true ]; then
  VILLAGE_ID=$(echo "$VILLAGE_RESPONSE" | jq -r '.data.id')
else
  VILLAGE_ID=$(echo "$VILLAGE_RESPONSE" | grep -o '"data":{.*}' | grep -o '"id":[0-9]*' | cut -d':' -f2)
fi

if [ -z "$VILLAGE_ID" ] || [ "$VILLAGE_ID" = "null" ]; then
  echo "❌ 'M village' 마을 생성 실패"
  echo "응답: $VILLAGE_RESPONSE"
  exit 1
fi

echo "M village 마을 ID: $VILLAGE_ID"

# 마을장 등록
echo "4-1. 마을장 등록"
VILLAGE_LEADER_ASSIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/village-leader" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d "{
    \"villageId\": $VILLAGE_ID,
    \"userId\": $VILLAGE_LEADER_ID,
    \"startDate\": \"2025-01-01\"
  }")

if [[ "$VILLAGE_LEADER_ASSIGN_RESPONSE" == *"\"success\":true"* ]]; then
  echo "✅ 마을장 등록 성공"
else
  echo "❌ 마을장 등록 실패"
  echo "응답: $VILLAGE_LEADER_ASSIGN_RESPONSE"
fi

# 4. univ5 부서에 속한 LEADER 조회
echo "5. 'univ5' 부서에 속한 LEADER 조회"
LEADERS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/by-roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "roles": ["LEADER"]
  }')

# LEADER ID 목록 추출
LEADER_IDS=()
if [ "$JQ_AVAILABLE" = true ]; then
  # jq를 사용하여 추출
  while read -r id; do
    LEADER_IDS+=("$id")
  done < <(echo "$LEADERS_RESPONSE" | jq -r ".data.users[] | select(.departmentId == $DEPARTMENT_ID and .role == \"LEADER\") | .id")
else
  # grep을 사용하여 추출 (대체 방법)
  # 응답 구조가 변경되어 users 배열 내에서 찾아야 함
  USERS_SECTION=$(echo "$LEADERS_RESPONSE" | grep -o '"users":\[.*\]' | sed 's/"users"://')
  while read -r line; do
    if echo "$line" | grep -q "\"departmentId\":$DEPARTMENT_ID" && echo "$line" | grep -q "\"role\":\"LEADER\""; then
      id=$(echo "$line" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
      if [ -n "$id" ]; then
        LEADER_IDS+=("$id")
      fi
    fi
  done < <(echo "$USERS_SECTION" | tr '{' '\n')
fi

echo "univ5 부서의 LEADER 수: ${#LEADER_IDS[@]}"

if [ ${#LEADER_IDS[@]} -eq 0 ]; then
  echo "❌ 'univ5' 부서에 LEADER가 없습니다."
  exit 1
fi

# 5. 각 리더마다 GBS 그룹 생성
echo "6. 각 리더마다 GBS 그룹 생성"
for i in "${!LEADER_IDS[@]}"; do
  LEADER_ID=${LEADER_IDS[$i]}
  GROUP_NAME="GBS Group $((i+1))"
  
  echo "리더 ID: ${LEADER_ID}에 대한 GBS 그룹 생성: $GROUP_NAME"
  
  # GBS 그룹 생성
  GBS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/organization/gbs-groups" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{
      \"name\": \"$GROUP_NAME\",
      \"villageId\": $VILLAGE_ID,
      \"termStartDate\": \"2025-01-01\",
      \"termEndDate\": \"2025-09-01\",
      \"leaderId\": $LEADER_ID
    }")
  
  # GBS 그룹 ID 추출
  if [ "$JQ_AVAILABLE" = true ]; then
    GBS_ID=$(echo "$GBS_RESPONSE" | jq -r '.data.id')
  else
    GBS_ID=$(echo "$GBS_RESPONSE" | grep -o '"data":{.*}' | grep -o '"id":[0-9]*' | cut -d':' -f2)
  fi
  
  if [ -z "$GBS_ID" ] || [ "$GBS_ID" = "null" ]; then
    echo "❌ GBS 그룹 생성 실패: $GROUP_NAME"
    echo "응답: $GBS_RESPONSE"
  else
    echo "✅ GBS 그룹 생성 성공: $GROUP_NAME (ID: $GBS_ID)"
  fi
  
  # 요청 사이에 약간의 지연 추가
  sleep 0.5
done

echo "작업 완료! 테스트 시나리오가 성공적으로 실행되었습니다." 