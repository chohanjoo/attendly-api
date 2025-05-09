#!/bin/bash

# Univ5 리더 계정 자동화 스크립트
#
# 이 스크립트는 다음 단계를 자동화합니다:
# 1. 관리자 계정으로 로그인
# 2. "univ5" 부서 생성
# 3. "univ5" 부서에 20명의 LEADER 권한 계정 생성

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

# 2. "univ5" 부서 생성
echo "2. 'univ5' 부서 생성"
DEPARTMENT_ID=$(curl -s -X POST "$BASE_URL/api/admin/organization/departments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -d '{
    "name": "univ5"
  }' | grep -o '"id":[0-9]*' | cut -d':' -f2)

echo "부서 ID: $DEPARTMENT_ID"

# 3. LEADER 권한의 계정 생성 (20명)
echo "3. LEADER 권한의 계정 생성 (20명)"

# 사용자 데이터 배열
names=("John Smith" "Michael Johnson" "Robert Williams" "David Brown" "James Jones" 
      "William Miller" "Richard Davis" "Joseph Wilson" "Thomas Taylor" "Charles Anderson" 
      "Daniel White" "Matthew Harris" "Anthony Thompson" "Donald Moore" "Mark Jackson" 
      "Paul Martin" "Steven Lee" "Andrew Garcia" "Kenneth Robinson" "Joshua Clark")

# 각 사용자에 대해 API 호출
for i in {1..20}; do
  index=$((i-1))
  name="${names[$index]}"
  email="univ4Rleader$(printf "%02d" $i)@example.com"
  phone="010-1$(printf "%03d" $i)-1$(printf "%03d" $i)"
  
  # 생년월일 계산 (1995년 시작, 매달 증가)
  if [ $i -le 12 ]; then
    birthdate="1995-$(printf "%02d" $i)-$(printf "%02d" $i)"
  else
    month=$((i-12))
    birthdate="1996-$(printf "%02d" $month)-$(printf "%02d" $i)"
  fi
  
  echo "사용자 생성 중 ($i/20): $name, $email"
  
  response=$(curl -s -X POST "$BASE_URL/api/admin/users" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{
      \"name\": \"$name\",
      \"email\": \"$email\",
      \"password\": \"test123!@#\",
      \"role\": \"LEADER\",
      \"departmentId\": $DEPARTMENT_ID,
      \"phoneNumber\": \"$phone\",
      \"birthDate\": \"$birthdate\"
    }")
  
  # 응답 확인
  if [[ $response == *"id"* ]]; then
    echo "✅ 성공: $name 사용자 생성됨"
  else
    echo "❌ 실패: $name 사용자 생성 실패"
    echo "오류 응답: $response"
  fi
  
  # 요청 사이에 약간의 지연 추가
  sleep 0.5
done

echo "작업 완료! 총 20명의 LEADER 계정이 univ5 부서에 생성되었습니다." 