#!/bin/bash

# Univ5 멤버 계정 자동화 스크립트
#
# 이 스크립트는 다음 단계를 자동화합니다:
# 1. 관리자 계정으로 로그인
# 2. "univ5" 부서 조회
# 3. "univ5" 부서에 100명의 MEMBER 권한 계정 생성

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

# 2. "univ5" 부서 목록 조회 및 ID 찾기
echo "2. 'univ5' 부서 조회"
DEPARTMENTS=$(curl -s -X GET "$BASE_URL/api/admin/organization/departments" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

# univ5 부서의 ID 추출
DEPARTMENT_ID=$(echo $DEPARTMENTS | grep -o '{"id":[0-9]*,"name":"univ5"[^}]*}' | grep -o '"id":[0-9]*' | cut -d':' -f2)

if [ -z "$DEPARTMENT_ID" ]; then
  echo "❌ univ5 부서를 찾을 수 없습니다. 먼저 부서를 생성해주세요."
  exit 1
fi

echo "부서 ID: $DEPARTMENT_ID"

# 3. MEMBER 권한의 계정 생성 (100명)
echo "3. MEMBER 권한의 계정 생성 (100명)"

# 영어 이름 랜덤 생성을 위한 배열
first_names=("James" "John" "Robert" "Michael" "William" "David" "Richard" "Joseph" "Thomas" "Charles" 
            "Christopher" "Daniel" "Matthew" "Anthony" "Mark" "Donald" "Steven" "Paul" "Andrew" "Joshua"
            "Kenneth" "Kevin" "Brian" "George" "Timothy" "Ronald" "Edward" "Jason" "Jeffrey" "Ryan"
            "Jacob" "Gary" "Nicholas" "Eric" "Jonathan" "Stephen" "Larry" "Justin" "Scott" "Brandon"
            "Benjamin" "Samuel" "Gregory" "Alexander" "Frank" "Patrick" "Raymond" "Jack" "Dennis" "Jerry")
            
last_names=("Smith" "Johnson" "Williams" "Jones" "Brown" "Davis" "Miller" "Wilson" "Moore" "Taylor"
           "Anderson" "Thomas" "Jackson" "White" "Harris" "Martin" "Thompson" "Garcia" "Martinez" "Robinson"
           "Clark" "Rodriguez" "Lewis" "Lee" "Walker" "Hall" "Allen" "Young" "Hernandez" "King"
           "Wright" "Lopez" "Hill" "Scott" "Green" "Adams" "Baker" "Gonzalez" "Nelson" "Carter"
           "Mitchell" "Perez" "Roberts" "Turner" "Phillips" "Campbell" "Parker" "Evans" "Edwards" "Collins")

# 각 사용자에 대해 API 호출
for i in {1..100}; do
  # 랜덤 이름 생성
  first_name_idx=$((RANDOM % 50))
  last_name_idx=$((RANDOM % 50))
  first_name="${first_names[$first_name_idx]}"
  last_name="${last_names[$last_name_idx]}"
  name="$first_name $last_name"
  
  # 이메일 생성
  email="univ5Rmember$(printf "%03d" $i)@example.com"
  phone="010-20$(printf "%02d" $i)-20$(printf "%02d" $i)"
  
  # 생년월일 계산 (2000년 시작, 다양하게 분포)
  year=$((2000 + (i % 5)))
  month=$(( (i % 12) + 1 ))
  day=$(( (i % 28) + 1 ))
  birthdate="$year-$(printf "%02d" $month)-$(printf "%02d" $day)"
  
  echo "사용자 생성 중 ($i/100): $name, $email"
  
  response=$(curl -s -X POST "$BASE_URL/api/admin/users" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{
      \"name\": \"$name\",
      \"email\": \"$email\",
      \"password\": \"test123!@#\",
      \"role\": \"MEMBER\",
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

echo "작업 완료! 총 100명의 MEMBER 계정이 univ5 부서에 생성되었습니다." 