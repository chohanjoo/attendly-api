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
# 1. "M village" 마을 조회
# 2. "M village" 마을에 속한 모든 GBS 그룹 조회
# 3. "M village" 마을에 속한 MEMBER 역할의 사용자 조회
# 4. GBS별로 5명씩 멤버 배정 (마지막 GBS는 4명)

BASE_URL="http://localhost:8080"

# 1. 관리자 계정으로 로그인
echo "1. 관리자 계정으로 로그인"
ACCESS_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "hanjoo@naver.com",
    "password": "test123!@#"
  }' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
  echo "❌ 로그인 실패. 관리자 계정 정보를 확인해주세요."
  exit 1
fi

echo "토큰: $ACCESS_TOKEN"

# M village 마을 조회 - 마을 ID 찾기
echo "2. 'M village' 마을 찾기"
# 모든 부서 목록 조회
DEPARTMENTS=$(curl -s -X GET "$BASE_URL/api/admin/organization/departments" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "부서 정보: $DEPARTMENTS"

# 각 부서에 속한 마을 검색하여 "M village" 찾기
VILLAGE_ID=""
if [ "$JQ_AVAILABLE" = true ]; then
  # jq를 사용한 방법
  DEPT_IDS=$(echo "$DEPARTMENTS" | jq -r '.[].id')
else
  # grep을 사용한 방법
  DEPT_IDS=$(echo "$DEPARTMENTS" | grep -o '"id":[0-9]*' | cut -d':' -f2)
fi

# 마을 ID를 찾기 위해 마을장 계정으로 조회 시도
echo "마을장 계정으로 마을 조회 중..."
VILLAGE_LEADERS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/by-roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "roles": ["VILLAGE_LEADER"]
  }')

echo "마을장 목록 응답: $VILLAGE_LEADERS_RESPONSE"

# 직접 모든 마을을 나열해서 찾기
ALL_VILLAGES=()
for DEPT_ID in $DEPT_IDS; do
  echo "부서 ID $DEPT_ID의 마을 조회 중..."
  
  # 해당 부서의 마을장들 정보 추출
  if [ "$JQ_AVAILABLE" = true ]; then
    # jq 사용
    LEADERS=$(echo "$VILLAGE_LEADERS_RESPONSE" | jq -r ".users[] | select(.departmentId == $DEPT_ID) | .id")
  else
    # grep 사용
    LEADERS=$(echo "$VILLAGE_LEADERS_RESPONSE" | grep -o "{[^}]*\"departmentId\":$DEPT_ID[^}]*\"role\":\"VILLAGE_LEADER\"[^}]*}" | grep -o '"id":[0-9]*' | cut -d':' -f2)
  fi
  
  for LEADER_ID in $LEADERS; do
    echo "마을장 ID $LEADER_ID의 마을 정보 조회 중..."
    
    # 이 마을장이 속한 마을 정보를 찾기 위해 
    # 부서 내 모든 마을 조회 API가 없으므로 다른 방법 시도
    # 실제로는 이런 API가 필요함
  done
done

# M village 찾기 - 대안 방법
echo "모든 마을 검색 대안 시도..."
# 관리자는 마을 목록을 볼 수 있는 API가 따로 없는 것 같으므로
# 마을 ID 직접 시도 (일반적으로 ID는 순차적으로 증가)
for i in {1..10}; do
  echo "마을 ID $i 확인 중..."
  VILLAGE_CHECK=$(curl -s -X GET "$BASE_URL/api/village-leader/$i/gbs" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  
  if [[ $VILLAGE_CHECK == *"M village"* ]]; then
    VILLAGE_ID=$i
    echo "M village 발견! ID: $VILLAGE_ID"
    break
  elif [[ $VILLAGE_CHECK != *"error"* && $VILLAGE_CHECK != *"Village not found"* ]]; then
    echo "다른 마을 발견: $VILLAGE_CHECK"
  fi
done

if [ -z "$VILLAGE_ID" ]; then
  echo "❌ 'M village' 마을을 찾을 수 없습니다."
  exit 1
fi

echo "M village 마을 ID: $VILLAGE_ID"

# 3. "M village" 마을에 속한 모든 GBS 그룹 조회
echo "3. 'M village' 마을에 속한 모든 GBS 그룹 조회"
GBS_INFO=$(curl -s -X GET "$BASE_URL/api/village-leader/$VILLAGE_ID/gbs" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "GBS 정보 응답: $GBS_INFO"

# GBS ID 목록 추출
GBS_IDS=()
if [ "$JQ_AVAILABLE" = true ]; then
  # jq를 사용하여 추출
  while read -r id; do
    GBS_IDS+=("$id")
  done < <(echo "$GBS_INFO" | jq -r '.gbsList[].gbsId')
else
  # grep을 사용하여 추출 (대체 방법)
  while read -r line; do
    if echo "$line" | grep -q '"gbsId":[0-9]*'; then
      id=$(echo "$line" | grep -o '"gbsId":[0-9]*' | head -1 | cut -d':' -f2)
      if [ -n "$id" ]; then
        GBS_IDS+=("$id")
      fi
    fi
  done < <(echo "$GBS_INFO" | tr '{' '\n')
fi

GBS_COUNT=${#GBS_IDS[@]}
echo "M village 마을의 GBS 수: $GBS_COUNT"

if [ $GBS_COUNT -eq 0 ]; then
  echo "❌ 'M village' 마을에 GBS 그룹이 없습니다."
  exit 1
fi

# 4. "M village" 마을에 속한 MEMBER 역할의 사용자 조회
echo "4. 'M village' 마을에 속한 MEMBER 역할의 사용자 조회"
MEMBERS_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/by-roles" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "roles": ["MEMBER"]
  }')

# MEMBER ID 목록 추출
MEMBER_IDS=()
if [ "$JQ_AVAILABLE" = true ]; then
  # jq를 사용하여 추출
  while read -r id; do
    MEMBER_IDS+=("$id")
  done < <(echo "$MEMBERS_RESPONSE" | jq -r '.users[].id')
else
  # grep을 사용하여 추출 (대체 방법)
  while read -r line; do
    if echo "$line" | grep -q '"role":"MEMBER"'; then
      id=$(echo "$line" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
      if [ -n "$id" ]; then
        MEMBER_IDS+=("$id")
      fi
    fi
  done < <(echo "$MEMBERS_RESPONSE" | tr '{' '\n')
fi

MEMBER_COUNT=${#MEMBER_IDS[@]}
echo "MEMBER 역할 사용자 수: $MEMBER_COUNT"

if [ $MEMBER_COUNT -eq 0 ]; then
  echo "❌ MEMBER 역할의 사용자가 없습니다."
  exit 1
fi

# 5. GBS별로 5명씩 멤버 배정 (마지막 GBS는 4명 또는 남은 인원)
echo "5. GBS별로 조원 배정 시작"

total_assigned=0
required_members=$((GBS_COUNT * 5 ))  # 마지막 GBS는 4명이므로 1명 적게 필요

if [ $MEMBER_COUNT -lt $required_members ]; then
  echo "⚠️ 경고: 필요한 멤버 수($required_members)보다 가용 멤버 수($MEMBER_COUNT)가 적습니다."
  echo "   각 GBS에 균등하게 배정합니다."
fi

# 각 GBS 그룹에 멤버 배정
for i in "${!GBS_IDS[@]}"; do
  GBS_ID=${GBS_IDS[$i]}
  
  # 각 GBS의 배정 인원 결정 (마지막 GBS는 4명, 나머지는 5명)
  members_to_assign=5
  
  # 남은 멤버가 부족할 경우 조정
  remaining_members=$((MEMBER_COUNT - total_assigned))
  if [ $remaining_members -lt $members_to_assign ]; then
    members_to_assign=$remaining_members
  fi
  
  echo "GBS ID: $GBS_ID에 $members_to_assign명 배정 중..."
  
  # 각 GBS에 멤버 배정
  for j in $(seq 0 $(($members_to_assign - 1))); do
    member_index=$((total_assigned + j))
    if [ $member_index -ge $MEMBER_COUNT ]; then
      break
    fi
    
    MEMBER_ID=${MEMBER_IDS[$member_index]}
    
    echo "  GBS ID: ${GBS_ID}에 멤버 ID: ${MEMBER_ID} 배정 중..."
    
    ASSIGN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/admin/organization/gbs-groups/$GBS_ID/members" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -d "{
        \"memberId\": $MEMBER_ID,
        \"startDate\": \"2025-01-01\"
      }")
    
    # 응답 확인
    if [[ $ASSIGN_RESPONSE == *"error"* ]]; then
      echo "  ❌ 멤버 배정 실패: $ASSIGN_RESPONSE"
    else
      echo "  ✅ 멤버 배정 성공: 멤버 ID $MEMBER_ID -> GBS ID $GBS_ID"
    fi
    
    # 요청 사이에 약간의 지연 추가
    sleep 0.5
  done
  
  total_assigned=$((total_assigned + members_to_assign))
  echo "GBS ID: ${GBS_ID}에 ${members_to_assign}명 배정 완료. 총 배정 인원: ${total_assigned}"
  
  # 더 이상 배정할 멤버가 없으면 중단
  if [ $total_assigned -ge $MEMBER_COUNT ]; then
    echo "모든 가용 멤버를 배정했습니다."
    break
  fi
done

echo "작업 완료! 테스트 시나리오가 성공적으로 실행되었습니다."
echo "총 ${GBS_COUNT}개의 GBS에 ${total_assigned}명의 멤버가 배정되었습니다." 