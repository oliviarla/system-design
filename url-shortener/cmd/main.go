package main

import (
	"net/http"
	"time"
)

/*
(단축 URL 입력을 리다이렉트 처리하는 엔드포인트) + (URL 단축을 처리하는 엔드포인트)로 구성 🍀
*/
func main() {

}

/*
단축 URL에 매핑된 진짜 URL이 있는지 확인
DB는 NoSQL 사용 (단축URL: 실제URL 매핑된 형태이어야 함)
*/
func redirectURL(r *http.Request) (*http.Response, error) {
	return nil, nil
}

/*
URL을 해싱하여 단축한 후 데이터베이스에 저장
MD5 -> 일부만 잘라 Base62 변환 -> 만약 기존에 존재한다면 리해싱
만료 시간을 지정하도록 한다. nil이면 만료되지 않음
*/
func shortenURL(url string, time time.Time) (string, error) {
	return "", nil
}
